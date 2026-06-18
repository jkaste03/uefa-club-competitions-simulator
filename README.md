# UEFA Club Competitions Simulator

A Java 22 / Maven project that simulates UEFA club competitions for the Champions League, Europa League, and Conference League.

The project reconstructs the tournament graph from local data, simulates club progression through qualifying and league phases, and produces aggregated club reports from many independent runs.

## What the project actually does

- Reads tournament data from JSON files in the repository and builds a complete round graph for all supported competitions.
- Models clubs as mutable simulation state with Elo ratings, while keeping the base club registry separate from round-specific state.
- Resolves tie-backed slots so later rounds can depend on earlier rounds without losing bracket structure.
- Simulates qualifying rounds, the league phase, knockout round play-offs, and the round of 16 as modeled in `Rounds`.
- Aggregates statistics per round, per club, and per opponent for later reporting.
- Verifies key invariants in the tests, including illegal draws, league-phase structure, and deep-copy isolation.

## Core building blocks

The codebase is organized around a small number of central layers:

- `UefaCCSim` is the application entry point and starts the parallel simulations.
- `Rounds` is the orchestrator that links all rounds together and controls the flow between them.
- `Round`, `QRound`, `LeaguePhaseRound`, `KnockoutRoundPlayoff`, and `RoundOf16` model the different round types.
- `ClubSlot` is a bracket wrapper that can represent either a concrete club or a tie that has not yet been resolved.
- `JsonDataLoader` loads clubs, ties, and league-phase scheduling from `data.json`.
- `ClubEloRatingsInitializer` populates Elo ratings by fetching ClubElo data and falls back to a cached CSV if the download fails.
- `DefaultMatchSimulator` simulates match results with an Elo-based goal model and overdispersion.
- `StatsAggregator`, `RoundStats`, `ClubRoundStats`, and `ClubRoundCounters` build the statistics used by the reports.
- `ClubReportWriter` and `RoundStatsReportFormatter` turn the aggregated data into readable club reports.

The repository is intentionally split by responsibility rather than by competition only. That makes it easier to reason about one concern at a time:

- configuration lives in `config`;
- tournament enums and static competition labels live in `enums`;
- bracket and match logic live in `model/competition`;
- ranking, team metadata, and repositories live in `model`, `repository`, and `service`;
- reporting is isolated in `reporting` so the simulation logic stays focused on bracket and result generation.

## Repository structure

- `src/main/java/com/github/jkaste03/uefaccsim/UefaCCSim.java` - main program.
- `src/main/java/com/github/jkaste03/uefaccsim/config/SimulationConfig.java` - core parameters for the goal model and tie-break behavior.
- `src/main/java/com/github/jkaste03/uefaccsim/data/` - input data, ClubElo cache, and historical snapshots.
- `src/main/java/com/github/jkaste03/uefaccsim/enums/` - tournament, country, round type, path type, and leg type enums.
- `src/main/java/com/github/jkaste03/uefaccsim/model/` - domain objects for clubs and competition structure.
- `src/main/java/com/github/jkaste03/uefaccsim/model/rule/` - restrictions such as political tie prohibitions.
- `src/main/java/com/github/jkaste03/uefaccsim/repository/` - in-memory repositories for clubs and simulation state.
- `src/main/java/com/github/jkaste03/uefaccsim/reporting/` - statistics and report generation.
- `src/main/java/com/github/jkaste03/uefaccsim/service/` - data import, Elo initialization, and match simulation.
- `src/test/java/com/github/jkaste03/uefaccsim/` - tests covering the structural and probabilistic logic.

Important subpackages in the model layer:

- `model/competition` contains the round graph, ties, league table, and bracket resolution logic.
- `model/rule` contains restrictions that affect draw legality.
- `model` holds the base `Club` type used by the repository and the data loader.

## Tournament and round flow

`Rounds` sets up a chronological chain of rounds for the three tournaments. That means the project does not just have separate classes for each phase; it also has explicit links between phases.

The flow is effectively:

- qualifying rounds with champions path, league path, and main path where relevant,
- a league phase with pot-based draws and a fixed matchday schedule,
- knockout round play-offs where reordering and legal pairing rules are explicit,
- the round of 16 as the next step after the play-offs.

The `Round` hierarchy handles, among other things:

- which clubs are part of a round,
- which tie combinations are allowed,
- how slots are resolved when a tie is decided,
- how statistics are recorded for the current round.

`ClubSlot` is important because it can point to a concrete club or to a previous tie. That is what makes the qualifying and knockout flow a connected graph rather than a set of isolated lists.

At a more concrete level:

- `QRound` is responsible for seeded/unseeded pairing, draw legality, and qualifying-round matchup accounting.
- `LeaguePhaseRound` handles pot creation, matchday scheduling, league-table initialization, and league-phase matchup recording.
- `KnockoutRoundPlayoff` uses a specialized draw strategy for the 9-16 versus 17-24 bracket structure.
- `RoundOf16` continues the post-league knockout flow once the play-off round is finished.

That separation matters because each round type has its own legality rules and its own statistics model.

## Data and input

The primary data model lives in:

- `src/main/java/com/github/jkaste03/uefaccsim/data/data.json`

That file contains, among other things:

- the clubs participating in the simulation,
- the predefined ties for each round,
- the league-phase play order grouped by day,
- the previous Champions League winner used in seeding logic.

`JsonDataLoader` loads that data into the concrete round objects at startup. Clubs are registered in `ClubRepository`, and tie references are built through a cache of `ClubSlot` instances.

`ClubEloRatingsInitializer` fills the clubs with Elo values by reading the current ClubElo CSV. If the download fails, it uses the most recent cached CSV file in the same data folder. That makes the repo more robust, but it also means that club names in `data.json` must match the names in the ClubElo source.

The data flow is layered:

1. clubs are created first and stored in the repository,
2. round-specific club slots are created from those clubs,
3. ties are loaded and linked to the cached slots,
4. round validation checks that the loaded structure is internally consistent,
5. Elo values are injected so simulation logic can use them during draw and match computation.

That design makes the input files the authoritative source for the competition structure while keeping simulation state separate from the static definitions.

## Simulation model

The match simulation is not deterministic. It combines:

- Elo difference,
- home advantage,
- a negative binomial goal model for overdispersed scores,
- draw inflation for selected small scorelines,
- extra time and penalty-shootout configuration where needed.

`SimulationConfig` collects these parameters in one place so the model can be tuned without spreading constants through the codebase.

The simulation flow in `UefaCCSim` works like this:

1. A baseline `Rounds` object is created once.
2. Each simulation receives a deep copy of the baseline so mutable state is not shared.
3. The simulations run in parallel.
4. Worker threads write statistics into their own aggregators.
5. The results are merged into a final aggregator used for reporting.

The code avoids shared mutable round state by deep-copying the full `Rounds` object before each run. That is important because the bracket graph contains many interconnected references, and simple shallow copies would let one run affect another.

`DefaultMatchSimulator` is designed to be self-contained: it receives the two Elo values, the time-state flag, and the simulation configuration, then returns a validated `MatchResult`. That keeps the actual round code focused on bracket logic rather than score generation.

## Reporting

Reports are written to:

- `temp/club-reports/<country>/<club>.txt`

For each club, a separate file is created inside a country-specific folder.

`RoundStatsReportFormatter` controls how these blocks are laid out. `StatsAggregator` and `RoundStats` keep both raw matchup totals and seeding splits, so the reports can show more than just opponent lists.

## Building and running

Requirements:

- Java 22
- Maven 3.9+ recommended

Build and test the project with:

```bash
mvn test
```

If you only want to compile the project:

```bash
mvn compile
```

The simplest way to run the simulator is to launch `com.github.jkaste03.uefaccsim.UefaCCSim` from your IDE.

If you want to run it from Maven on the command line, you can call the Exec plugin explicitly:

```bash
mvn -q -DskipTests compile org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass=com.github.jkaste03.uefaccsim.UefaCCSim -Dexec.classpathScope=runtime
```

The simulator writes its output into `temp/club-reports/` and prints progress to the console while it runs.

## Testing

The tests in this repo are designed to catch structural and draw-related failures, not just startup problems.

They cover, among other things:

- that qualifying rounds do not produce illegal ties,
- that the league phase keeps its pot structure intact,
- that each club meets the correct number and type of opponents,
- that deep-copying the simulation graph actually isolates state,
- that generated matchings do not contain duplicates or invalid pairs.

That is useful when changing `Rounds`, `JsonDataLoader`, draw algorithms, or report logic.

## Where changes usually belong

The most common extension points in the repo are:

- `SimulationConfig` if you want to calibrate the match model.
- `JsonDataLoader` if you want to change the input format or tournament data.
- `Rounds` if you want to add more rounds or adjust tournament flow.
- `KnockoutRoundPlayoff` if you want to change the playoff bracket.
- `DefaultMatchSimulator` if you want to adjust how goals and results are simulated.
- `RoundStatsReportFormatter` if you want to change how the reports look.

## Practical notes

- The simulation is stochastic, so two runs can produce different results.
- Reports are based on aggregated statistics from many simulations, not a single run.
- The repository also contains historical data files and cached ClubElo snapshots in the data folder.
- Output under `temp/club-reports/` can be regenerated safely.

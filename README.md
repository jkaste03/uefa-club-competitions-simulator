# UEFA Club Competitions Simulator

Work in progress: This project is under active development.

A Java-based simulator for UEFA club competitions (Champions League, Europa League, and Conference League), including draws, match execution, and Elo-based strength adjustments.

## What The Project Does Today

- Round modeling across all three competitions.
- Qualification rounds with seeding, constrained draws, and winner/loser progression.
- League phase with pot seeding, draw generation, matchday scheduling, and table handling.
- Knockout round playoff draw after the league phase.
- Match simulation based on Elo and a probabilistic goal model.
- Parallel execution of many simulations from a shared baseline.

## Tech Stack

- Java 22
- Maven
- Gson
- JUnit 5

## Project Structure (Short)

- src/main/java/com/github/jkaste03/uefaccsim/UefaCCSim.java: application entry point
- src/main/java/com/github/jkaste03/uefaccsim/model/competition/: rounds, ties, table, and progression flow
- src/main/java/com/github/jkaste03/uefaccsim/service/match/: match simulation
- src/main/java/com/github/jkaste03/uefaccsim/service/ClubEloRatingsInitializer.java: fetches/loads Elo data
- src/main/java/com/github/jkaste03/uefaccsim/service/JsonDataLoader.java: loads tournament setup from data.json
- src/main/java/com/github/jkaste03/uefaccsim/data/data.json: input data for clubs and rounds
- src/test/java/com/github/jkaste03/uefaccsim/UefaCCSimTest.java: probabilistic and structural tests

## Getting Started

### Prerequisites

- JDK 22 installed
- Maven installed

Check locally:

```bash
java -version
mvn -version
```

### Build And Test

```bash
mvn -q -DskipTests=false test
```

### Run The Simulator

If you want to run the main class directly via Maven:

```bash
mvn -DskipTests exec:java -Dexec.mainClass="com.github.jkaste03.uefaccsim.UefaCCSim"
```

Note: The first run may download required Maven plugins.

## Data And Input

The project uses two main sources:

- Static structure/input in data.json (rounds, clubs, previous CL winner, and league phase play calendar).
- Daily Elo ratings from the ClubElo API, with fallback to the latest locally stored CSV if the network call fails.

## Simulation Logic (Short)

- Rounds are built once in a baseline structure.
- When running many simulations, the baseline is deep-copied per simulation to isolate mutable state.
- Elo deltas are updated continuously and committed in controlled steps between rounds/matchdays.

## Current Status

- The project is functional for large parts of the flow up to and including the knockout round playoff draw.
- Some parts of the knockout phase are still under development (for example, the Round of 16 draw).
- The test setup includes repeated (stochastic) validations of key invariants, especially in qualification and league phase.

## Next Work

- Stabilize and complete the full knockout flow after the playoff round.
- Improve observability/logging for draw analysis and simulation outcomes.
- Reduce technical debt and expand test coverage around unfinished rounds.

Work completed so far has two clear improvement areas: league phase draw and more realistic simulation of individual matches.

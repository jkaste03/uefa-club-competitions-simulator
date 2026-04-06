# UEFA Club Competitions Simulator

Work in progress: This project is under active development.

## Overview

A Java-based simulator that runs thousands of independent UEFA club competitions (Champions League, Europa League, and Conference League) in parallel to explore possible tournament outcomes. Each simulation models the complete tournament flow from qualification rounds through the knockout stage, with realistic match outcomes driven by Elo ratings and a probabilistic goal model.

**Primary use case:** Analyze the likelihood of different competition outcomes, study seeding effects, and understand how tournament structures impact club progression.

## Core Features

### Tournament Simulation

- **Full tournament flow** from qualification rounds → league phase → knockout playoffs → knockout rounds
- **Qualification rounds** with seeding, constrained draws (respecting political/regional restrictions), and winner/loser pathways
- **League phase** with pot-based seeding, draw constraints, matchday scheduling, and final standings calculation
- **Knockout stage** including playoff draws and progressive elimination

### Match Simulation Engine

- **Elo-based ratings** — Club strength determined by live Elo ratings from ClubElo API
- **Probabilistic goal model** — Match outcomes (goals, results) generated stochastically based on team strength
- **Dynamic Elo updates** — Ratings adjusted after each match to reflect recent performance
- **Realistic variance** — Upsets and surprises occur proportionally to actual football dynamics

### Large-Scale Analysis

- **Parallel execution** — Runs 8000+ independent simulations concurrently using Java's Fork/Join framework
- **Isolated state** — Each simulation is a deep copy of the baseline, ensuring no cross-contamination
- **Performance optimized** — Processes thousands of simulations efficiently

### Data Integration

- **JSON configuration** — Tournament structure, club rosters, and round specifications
- **Live Elo ratings** — Fetches from ClubElo API (with local CSV fallback)
- **Deterministic seeding** — Pot assignments and draw constraints based on real tournament rules

## Tech Stack

- Java 22
- Maven
- Gson
- JUnit 5

## Project Structure (Explained)

### Core Simulation

- **[UefaCCSim.java](src/main/java/com/github/jkaste03/uefaccsim/UefaCCSim.java)** — Entry point; manages parallel simulation execution via Fork/Join
- **src/main/java/com/github/jkaste03/uefaccsim/model/competition/** — Tournament structure:
    - `Rounds.java` — Orchestrates the complete tournament flow
    - `Round.java`, `QRound.java`, `LeaguePhaseRound.java`, `KnockoutRound.java` — Different round types
    - `Tie.java` — Individual match/matchup representation
    - `LeagueTable.java` — Standings and progression logic
    - `RoundOf16.java`, `KnockoutRoundPlayoff.java` — Knockout stage implementations

### Match Simulation

- **src/main/java/com/github/jkaste03/uefaccsim/service/match/** — Match execution:
    - `MatchSimulator.java` — Interface for match simulation
    - `DefaultMatchSimulator.java` — Elo-based match simulation with probabilistic goals

### Data & Configuration

- **[ClubEloRatingsInitializer.java](src/main/java/com/github/jkaste03/uefaccsim/service/ClubEloRatingsInitializer.java)** — Fetches/loads live Elo ratings from ClubElo API or CSV fallback
- **[JsonDataLoader.java](src/main/java/com/github/jkaste03/uefaccsim/service/JsonDataLoader.java)** — Loads tournament configuration
- **[data/data.json](src/main/java/com/github/jkaste03/uefaccsim/data/data.json)** — Tournament setup (clubs, rounds, league phase calendar)

### Models

- **[Club.java](src/main/java/com/github/jkaste03/uefaccsim/model/Club.java)** — Represents a club with Elo rating and metadata
- **[ClubSimState.java](src/main/java/com/github/jkaste03/uefaccsim/model/competition/ClubSimState.java)** — Tracks club progression status during a simulation
- **Enums** — `Tournament.java`, `RoundType.java`, `Country.java`, `PathType.java` for structured data

### Testing

- **[UefaCCSimTest.java](src/test/java/com/github/jkaste03/uefaccsim/UefaCCSimTest.java)** — Probabilistic and structural validation tests (stochastic invariant checking)

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

- **Static structure/input in [data/data.json](src/main/java/com/github/jkaste03/uefaccsim/data/data.json)** — Tournament rounds, clubs, previous CL winner, and league phase play calendar
- **Live Elo ratings** — Fetched from the ClubElo API on each run; falls back to the latest locally stored CSV if the network call fails

## Key Concepts

### Elo Ratings

- **Strength Assessment** — Each club has a live Elo rating that reflects its recent performance
- **Match Prediction** — Higher Elo clubs have a higher win probability, but upsets are possible
- **Dynamic Updates** — Elo adjusts after each match based on the result and pre-match expectations

### Probabilistic Match Model

- **Goal Generation** — Match outcomes (goals scored) are generated stochastically using expected goals weighted by Elo difference
- **Realistic Variance** — Stronger teams are favored but weaker teams can still win (reflecting real football)
- **Context Matters** — Home/away, tournament stage, and other factors influence match dynamics

## Simulation Workflow

1. **Initialization**
    - Load tournament structure from `data.json` (clubs, qualification rounds, league phase layout, etc.)
    - Fetch current Elo ratings for all clubs from ClubElo API (or use cached CSV if offline)
    - Create a baseline `Rounds` object representing the tournament template

2. **Parallel Simulate**
    - Execute 8000 independent simulations concurrently
    - Each simulation:
        - Deep copies the baseline to isolate mutable state
        - Executes all rounds in sequence (qualification → league phase → playoffs → knockout)
        - Updates Elo ratings after each match
        - Records final outcomes

3. **Output**
    - Tournament progression for each simulation
    - Club advancement/elimination at each stage
    - Final winners of each competition
    - Can be analyzed to compute probabilities (e.g., "What % chance does Club X have of winning?")

## Current Status

### Implemented & Stable

✅ Qualification rounds with constrained draws and progression logic  
✅ League phase structure, draw generation, matchday scheduling, and table calculation  
✅ Knockout playoff draw after league phase  
✅ Match simulation engine (Elo + probabilistic goals)  
✅ Parallel execution of 8000+ simulations  
✅ Comprehensive stochastic testing of invariants

### In Progress / Not Yet Complete

⚙️ Round of 16 draw and subsequent knockout rounds  
⚙️ Full knockout phase completion and post-tournament analysis  
⚙️ Enhanced logging and observability for draw analysis and outcomes  
⚙️ Reduction of technical debt and expanded test coverage

### Known Limitations

- Some knockout-stage implementations are incomplete or under development
- Data currently focuses on current/recent season; historical data support is limited
- Draw analysis logging could be more detailed

## Next Work

### High Priority

- Stabilize and complete the full knockout flow (Round of 16 through final)
- Implement proper Round of 16 draw with seeding constraints
- Add logging and analytics for tournament progression tracking

### Medium Priority

- Improve observability — better logging for draw analysis and simulation outcomes
- Expand test coverage for knockout and playoff stages
- Reduce technical debt in competition flow orchestration

### Future Enhancements

- More realistic match simulation (fatigue, momentum, weather effects)
- Historical data support and comparative analysis
- Web UI for simulation parameter control and result visualization

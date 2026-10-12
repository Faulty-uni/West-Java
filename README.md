# Scotland Yard — Java Game Model

**Graph-based move generation, game-state transitions and observer-driven interaction.**

A collaborative University of Bristol Java coursework project implementing the model
for Scotland Yard. Mr X and the detectives move across a transport graph with ticket
constraints, hidden/revealed travel information and game-ending conditions.

## Scope and provenance

This repository includes a university-provided API, JavaFX interface, assets and tests.
The coursework implementations are primarily
[MyGameStateFactory.java](src/main/java/uk/ac/bris/cs/scotlandyard/model/MyGameStateFactory.java)
and [MyModelFactory.java](src/main/java/uk/ac/bris/cs/scotlandyard/model/MyModelFactory.java).
It is not an independently built game engine or a deployed commercial application.

**Team:** Aryan Rezaei Amiri and Oscar. Aryan's
[commit history](https://github.com/Faulty-uni/West-Java/commits/main/?author=Aryan-R-A)
records game-state work, comments and iterative debugging against the supplied tests,
including [this implementation revision](https://github.com/Faulty-uni/West-Java/commit/f1c548c0bf560fd55592b8ce8169dcc3f510746e).
The final implementation combines both contributors' work; exact line-by-line ownership
and authorship of supplied tests are not inferred from repository access.

## How it works

```mermaid
flowchart LR
    Setup["Transport graph + players + reveal schedule"]
    State["GameState factory"]
    Moves["Legal single / double moves"]
    Advance["Advance move: tickets, positions, log, turn"]
    Model["Model wrapper + observer notifications"]
    UI["Supplied JavaFX interface"]
    Setup --> State --> Moves --> Advance --> State
    State --> Model --> UI
    UI --> Model --> Advance
```

- **Legal moves:** traverse adjacent graph nodes and inspect transport/ticket choices.
  Detective occupancy, secret tickets and Mr X's double moves affect eligibility.
- **Transitions:** spend and transfer tickets, update locations, append revealed or
  hidden log entries and determine who may move next.
- **Termination:** evaluate capture, exhausted rounds and movement availability.
- **Observers:** notify registered consumers when the model advances or ends the game.

The interesting engineering problem is keeping these interacting rules consistent.
Graph traversal, state modelling, the move visitor and observer pattern are more
substantive evidence than the appearance of the supplied interface.

## Stack and project layout

Java **17**, Maven, Guava immutable collections and graph APIs, JavaFX **17.0.2**.
The supplied test stack includes JUnit 4, AssertJ, Mockito and EqualsVerifier.

```text
src/main/java/.../model/     model API and coursework implementations
src/main/java/.../ui/        supplied interface and controllers
src/main/resources/         transport graph, map, layouts and other assets
src/test/java/.../model/     supplied model and observer tests
pom.xml                     build, test-suite selection and application entry point
```

## Build and run

Install **JDK 17** and **Maven**, then run from the repository root:

```bash
git clone https://github.com/Faulty-uni/West-Java.git
cd West-Java
mvn test
mvn compile exec:java
```

The desktop interface requires a graphical environment and appropriate JavaFX support.
See [the bundled manual](src/main/resources/manual/MANUAL.md) for game interaction.

## Testing and evidence

The Maven configuration selects the supplied aggregate suite.
The source tests cover creation invariants, legal moves, transitions, winning conditions,
playouts, player/ticket access and observer behaviour.

Commit messages document iterative test-driven development, including historical claims
of passing tests. They are not a substitute for a fresh executed test report. Use
`mvn test` and retain its result for the exact revision being reviewed.

## Limitations and next steps

- Review state immutability and side effects across successive game states.
- Review duplicate/null observer handling and the consistency of terminal-state access.
- Add a repeatable CI run using the supplied suite before advertising a current pass count.
- Attribute supplied code, images and tests; the resource licence does not by itself
  establish a licence for every contribution in the repository.
- Confirm applicable coursework publication rules before redistributing solutions.

This project contributes **algorithms and software-design evidence** to a portfolio.
It does not claim machine-learning research, a custom AI player or distributed execution.

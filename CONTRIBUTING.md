# Contributing to Integration Hub

Thanks for taking a look at this project. It started as a learning/portfolio
exercise, so these guidelines are intentionally lightweight — the goal is
to keep the codebase easy to read and easy to extend, not to enforce heavy
process.

## Ground rules

- **Keep the zero-dependency philosophy.** The project builds with nothing
  but the JDK on purpose. If you want to add a library, open an issue
  first to discuss whether it's worth losing that property for.
- **One subsystem, one package.** `order`, `inventory`, `notification`,
  `resilience`, `event` each own their own concern. New code should slot
  into an existing package or introduce a clearly-scoped new one — avoid
  reaching across packages except through the interfaces they expose.
- **`OrderService` is the only orchestrator.** Controllers should not call
  `InventoryRepository` or `NotificationService` directly; go through the
  service layer so the integration logic stays in one place.

## Getting set up

```bash
git clone <this-repo-url>
cd integration-hub
mvn clean package
java -jar target/integration-hub.jar
```

See the [README](README.md#getting-started) for the Maven-free build path.

## Coding conventions

- Java 17+ language features are fine (records, text blocks, pattern
  matching) — the compiler target is 17.
- Favor constructor injection over static singletons; every class in this
  project takes its collaborators in its constructor so it can be tested
  in isolation.
- Public methods that can fail across a system boundary (file I/O, HTTP)
  should have that failure mode visible in their signature (checked
  `Exception`, `Optional`, or a boolean result) rather than swallowed.
- New integration points should be wrapped in `CircuitBreaker`/
  `RetryPolicy` the same way `OrderService` wraps calls to
  `InventoryRepository`, if they represent a real external dependency.

## Tests

Every new class in `order`, `inventory`, `event`, or `resilience` should
ship with tests using the existing hand-rolled `TestRunner` (see
`src/test/java/com/jojo/integrationhub/TestRunner.java` for the
convention: public `testXxx()` methods, `Assert` helpers). Register any
new test class in `TestRunner.main`.

Run the suite before opening a PR:

```bash
mkdir -p out
javac -d out $(find src/main -name "*.java") $(find src/test -name "*.java")
java -cp out com.jojo.integrationhub.TestRunner
```

## Commit messages

- Use an imperative summary line under ~72 characters
  (`Add retry policy to inventory reservation`, not `Added...`).
- Use the body to explain *why*, not just *what* — the diff already shows
  what changed.

## Submitting changes

1. Fork the repo and create a branch off `main`.
2. Make your change, with tests, following the conventions above.
3. Make sure `mvn clean package` (or the javac equivalent) succeeds and
   the test suite passes.
4. Open a pull request describing the change and its motivation.

## Reporting issues

Please include: what you expected, what happened instead, and the
smallest `curl` command or test case that reproduces it.

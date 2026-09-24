# Integration Hub

A small, dependency-free Java system that demonstrates **system integration
patterns**: a REST API, a file-backed legacy inventory system, and
pluggable outbound notification channels, all coordinated through an
in-memory event bus and protected by resilience patterns (circuit breaker +
retry).

Built as a portfolio/learning project to practice integrating independent
subsystems the way you would in a real distributed system, without the
overhead of an actual message broker, database, or external APIs.

---

## Table of contents

- [Why this project exists](#why-this-project-exists)
- [Architecture](#architecture)
- [Design patterns used](#design-patterns-used)
- [Project structure](#project-structure)
- [Getting started](#getting-started)
    - [Prerequisites](#prerequisites)
    - [Build & run with Maven](#build--run-with-maven)
    - [Build & run with plain javac](#build--run-with-plain-javac-no-maven-needed)
- [API reference](#api-reference)
- [Running the tests](#running-the-tests)
- [Resilience behaviour](#resilience-behaviour)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

---

## Why this project exists

Most tutorial projects show a single system in isolation (just a REST API,
just a database, just a script). Real integration work is about the seams
*between* systems: what happens when the inventory file is briefly locked?
What happens when the SMS gateway is down but email still works? What
happens when three subsystems all need to react to "an order was placed"
without being wired directly to each other?

This project answers those questions concretely by simulating:

| Subsystem | Stands in for | Implementation |
|---|---|---|
| Order intake | A public-facing REST API | `com.sun.net.httpserver.HttpServer` (JDK built-in) |
| Inventory | A legacy system you don't control the schema of | CSV file (`data/inventory.csv`) |
| Notifications | External provider APIs (email/SMS gateways) | Adapter classes logging to stdout |
| Event bus | A message broker (Kafka/RabbitMQ/SQS) | In-memory pub/sub |

Every integration point is wrapped with a **circuit breaker** and **retry
policy**, because in a real system every one of those four rows is a
network or disk call that can fail.

## Architecture

```
                         ┌────────────────────┐
   HTTP client  ──────▶  │   OrderController   │  (REST: POST /orders, GET /orders/{id})
                         └─────────┬───────────┘
                                   │
                                   ▼
                         ┌────────────────────┐
                         │     OrderService     │  ◀── composition root: Main.java
                         │  (integration hub)    │
                         └───┬────────────┬─────┘
                 CircuitBreaker+Retry     │
                             │            │ publishes
                             ▼            ▼
                 ┌───────────────────┐ ┌────────────┐
                 │ InventoryRepository│ │  EventBus   │──▶ audit log listeners
                 │  (data/inventory.csv)│ └────────────┘
                 └───────────────────┘
                             │
                             ▼ on success/failure
                 ┌───────────────────────┐
                 │   NotificationService   │
                 │  ┌─────────┐ ┌────────┐ │
                 │  │  Email  │ │  SMS   │ │  (Adapter pattern — add
                 │  └─────────┘ └────────┘ │   more channels freely)
                 └───────────────────────┘
```

`OrderService` is the only class that talks to every subsystem — the
controller never touches inventory or notifications directly, and
inventory/notifications never know about each other. That single seam is
what makes each subsystem independently testable and independently
replaceable.

## Design patterns used

- **Adapter** — `NotificationChannel` gives `EmailNotificationAdapter` and
  `SmsNotificationAdapter` a common interface so the service layer never
  branches on channel type.
- **Circuit breaker** — `CircuitBreaker` trips OPEN after N consecutive
  failures and fails fast until a cool-down elapses, then allows a single
  HALF_OPEN trial call.
- **Retry with backoff** — `RetryPolicy` retries transient failures with
  linear backoff before giving up.
- **Publish/subscribe** — `EventBus` decouples "something happened"
  (`ORDER_RECEIVED`, `ORDER_CONFIRMED`, `ORDER_FAILED`) from "who cares".
- **Repository** — `InventoryRepository` hides the fact that the backing
  store is a CSV file; it could become JDBC or an HTTP client with no
  change to callers.
- **Composition root** — `Main` is the only class that references concrete
  implementations; everything else depends on interfaces/abstractions.

## Project structure

```
integration-hub/
├── pom.xml
├── data/
│   └── inventory.csv              # seed data for the "legacy" inventory system
├── src/main/java/com/jojo/integrationhub/
│   ├── Main.java                  # composition root
│   ├── event/                     # EventBus, Event, EventListener
│   ├── order/                     # Order, OrderStatus, OrderService, OrderController
│   ├── inventory/                 # InventoryItem, InventoryRepository
│   ├── notification/              # NotificationChannel + adapters + NotificationService
│   ├── resilience/                # CircuitBreaker, RetryPolicy
│   └── util/                      # SimpleLogger
└── src/test/java/com/jojo/integrationhub/
    ├── TestRunner.java            # tiny reflection-based runner (no JUnit dependency)
    ├── Assert.java
    ├── EventBusTest.java
    ├── CircuitBreakerTest.java
    └── InventoryRepositoryTest.java
```

## Getting started

### Prerequisites

- JDK 17 or newer (developed and tested against JDK 21)
- Maven 3.8+ (optional — see the javac-only path below)

The project has **zero third-party dependencies**, on purpose: it builds
and runs with nothing but the JDK, which makes it trivial to clone and run
anywhere, including offline.

### Build & run with Maven

```bash
mvn clean package
java -jar target/integration-hub.jar
# or on a custom port:
java -jar target/integration-hub.jar 9090
```

### Build & run with plain javac (no Maven needed)

```bash
mkdir -p out
javac -d out $(find src/main -name "*.java")
java -cp out com.jojo.integrationhub.Main
```

Either way, once it's running you should see:

```
Integration Hub is up. Try:
  curl http://localhost:8080/health
  curl -X POST http://localhost:8080/orders -d 'sku=SKU-1001&quantity=2&email=customer@example.com'
```

## API reference

| Method | Path | Body | Description |
|---|---|---|---|
| `GET`  | `/health` | — | Liveness check + current inventory circuit state |
| `POST` | `/orders` | `sku=<sku>&quantity=<n>&email=<address>` (form-encoded) | Places an order; reserves stock and sends a notification |
| `GET`  | `/orders/{id}` | — | Fetches a previously placed order by id |

Example:

```bash
curl -X POST http://localhost:8080/orders \
  -d 'sku=SKU-1001&quantity=2&email=customer@example.com'

# {"id":"...","sku":"SKU-1001","quantity":2,"customerEmail":"customer@example.com",
#  "status":"NOTIFIED","createdAt":"..."}
```

Seed SKUs available out of the box: `SKU-1001` … `SKU-1005` (see
`data/inventory.csv`).

## Running the tests

The test suite intentionally uses no test framework, so it builds with
plain `javac` — consistent with the rest of the project's zero-dependency
philosophy.

```bash
mkdir -p out
javac -d out $(find src/main -name "*.java") $(find src/test -name "*.java")
java -cp out com.jojo.integrationhub.TestRunner
```

With Maven, the same classes compile as part of `mvn test-compile`; wiring
in JUnit 5 + Surefire is on the [roadmap](#roadmap) below.

## Resilience behaviour

Every call into `InventoryRepository` from `OrderService` goes through:

1. **`RetryPolicy`** (3 attempts, linear backoff) — absorbs momentary
   blips (e.g. file contention).
2. **`CircuitBreaker`** (trips after 3 consecutive failures, 10s
   cool-down) — stops hammering a dependency that's genuinely down, and
   lets the system fail fast instead of piling up slow requests.

You can see the circuit's current state at any time via `GET /health`.

## Roadmap

- [ ] Swap the CSV-backed `InventoryRepository` for an H2/JDBC-backed one
  behind the same interface, to demonstrate the seam actually holding
- [ ] Add a real JUnit 5 + Surefire test setup alongside the current
  dependency-free runner
- [ ] Add a Dockerfile for a one-command run
- [ ] Add an OpenAPI spec for the REST endpoints

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for coding conventions, commit
message style, and how to submit changes.

## License

Released under the [MIT License](LICENSE).

---
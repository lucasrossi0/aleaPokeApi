# Pokémon Rankings API

A small Spring Boot service that answers three questions, using [PokéAPI](https://pokeapi.co/api/v2/) as the data source:

1. Which are the **5 heaviest** Pokémon?
2. Which are the **5 highest** Pokémon?
3. Which are the **5 Pokémon with the most base experience**?

Built with **Java 25**, **Spring Boot 4.1**, Maven, Lombok. Tested with JUnit 5, Mockito, WireMock and ArchUnit; coverage measured by JaCoCo (build fails below 90% of lines).

---

## How it works (the short version)

1. When the application starts, it fetches the list of **all** Pokémon from PokéAPI (1351 entries today), and then fetches the details of each one **at the same time** using Java virtual threads. On my machine this takes about **2 seconds**; fetching them one by one would take minutes.
2. Each response is converted to our own `Pokemon` object. From that moment on, nothing in the code knows PokéAPI exists.
3. With Java streams we compute the three top-5 lists **once** and keep them in memory.
4. Requests only read that memory: they are instant and never call PokéAPI.
5. If someone calls an endpoint **before** the loading has finished, the answer is **`503 Not Ready`**.

---

## Approach and assumptions

**How I read the task.** PokéAPI has no "sort by weight" or "top N" query, so the only way to answer these questions is to read every Pokémon and rank them ourselves. The real design question is *when* to do that work.

| Option | Why I did not choose it |
|---|---|
| Fetch everything on every request | ~1350 HTTP calls per request: slow, fragile, and unfriendly to PokéAPI. |
| Fetch lazily on the first request, then cache | The first user would wait several seconds and a PokéAPI failure would surface as a request error. |
| **Fetch everything at startup, keep the three top-5 lists in memory** (chosen) | Requests are instant, never depend on PokéAPI being up, and the data (which almost never changes) is read only once. |

**Assumptions I made** (all of them are easy to change):

- **All 1351 entries of `/pokemon` count as Pokémon**, including alternate forms (`*-mega`, `*-gmax`...). I did not filter them because the task does not say to. The side effect is explained in [Things worth knowing about the data](#things-worth-knowing-about-the-data).
- **A missing `base_experience` counts as 0**, so those Pokémon never reach the top.
- **Ties are broken by name (A→Z)**, so the same data always gives the same answer.
- **Answering `503` while loading is my choice**, not something the task asked for. I preferred an honest "not ready yet" over blocking the request or returning a partial ranking.
- **Units are PokéAPI's own** (`weight` in hectograms, `height` in decimetres); I do not convert them, to avoid hiding the source data.

---

## Production readiness

What is already in place:

- **Health and probes:** `/actuator/health` plus liveness/readiness. Readiness is `UP` only once the rankings are loaded, so an orchestrator will not send traffic to an instance that would answer `503`.
- **Resilience towards PokéAPI:** connect and read timeouts, 3 attempts per Pokémon with a pause, a concurrency limit, and skip-and-log for a Pokémon that keeps failing instead of failing the whole load.
- **Predictable errors:** standard `ProblemDetail` JSON for our errors and Spring's own; unexpected errors return a generic 500 without leaking internals, and the cause is only logged.
- **Configuration without rebuilding:** every `pokeapi.*` property can be overridden with an environment variable.
- **Container:** multi-stage Docker image, runs as a non-root user.
- **Quality gates:** the build fails below 90% line coverage, and ArchUnit protects the layer rules.

Known gaps, so nobody has to find them by surprise:

- **If the initial list call fails, the service stays in `503` until it is restarted.** Readiness stays `DOWN`, so it is visible to the platform, but it does not heal by itself.
- **The data is loaded once.** There is no refresh, so a long-running instance never sees PokéAPI changes.
- No metrics, no Docker `HEALTHCHECK`, no CI pipeline, and no way to see how many Pokémon were skipped. See the improvements list below.

---

## Endpoints

| Endpoint | Returns |
|---|---|
| `GET /api/pokemon/heaviest` | 5 Pokémon with the highest `weight`, heaviest first |
| `GET /api/pokemon/highest` | 5 Pokémon with the highest `height`, tallest first |
| `GET /api/pokemon/base-experience` | 5 Pokémon with the highest `base_experience`, highest first |
| `GET /actuator/health` | Application health. `/actuator/health/readiness` is `UP` only after the rankings are loaded |
| `GET /actuator/info` | Basic application info |

Example:

```bash
curl localhost:8080/api/pokemon/base-experience
```
```json
[
  {"id": 242,   "name": "blissey",             "weight": 468, "height": 15,   "baseExperience": 608},
  {"id": 10190, "name": "eternatus-eternamax", "weight": 0,   "height": 1000, "baseExperience": 563},
  {"id": 10069, "name": "audino-mega",         "weight": 320, "height": 15,   "baseExperience": 425},
  {"id": 113,   "name": "chansey",             "weight": 346, "height": 11,   "baseExperience": 395},
  {"id": 531,   "name": "audino",              "weight": 310, "height": 11,   "baseExperience": 390}
]
```

While loading (the first seconds after startup) the three endpoints answer `503`:

```json
{"detail": "The rankings are still being loaded, please try again in a few seconds",
 "instance": "/api/pokemon/heaviest", "status": 503, "title": "Not Ready"}
```

Errors use the standard [problem details](https://www.rfc-editor.org/rfc/rfc9457) JSON format, also for Spring's own errors (404, 405...). Unexpected errors return a generic 500 and the real cause is only written to the log.

Units are the ones PokéAPI uses: `weight` in hectograms, `height` in decimetres.

---

## Run it

Requirements: **JDK 25**.

```bash
./mvnw spring-boot:run          # starts on http://localhost:8080
```

> If your default Java is not 25: `export JAVA_HOME=$(/usr/libexec/java_home -v 25)` (macOS) before running Maven.

### Docker

```bash
docker build -t pokemon-rankings .
docker run -p 8080:8080 pokemon-rankings
```

The image is multi-stage (build with JDK 25, run on the JRE 25) and runs as a non-root user.
I built and ran it (Docker 29): the container serves the three endpoints with the same results as `spring-boot:run` (503 for ~2 s, then 200; readiness `UP`; 1351 Pokémon loaded in ~2.3 s) and runs as user `app` (uid 10001), not root.

### Configuration (`src/main/resources/application.yml`)

| Property | Default | Meaning |
|---|---|---|
| `pokeapi.base-url` | `https://pokeapi.co/api/v2` | PokéAPI address |
| `pokeapi.connect-timeout` | `5s` | Timeout to open a connection |
| `pokeapi.read-timeout` | `10s` | Timeout waiting for an answer |
| `pokeapi.max-attempts` | `3` | Tries per Pokémon before skipping it |
| `pokeapi.retry-delay` | `500ms` | Pause between two tries of the same call |
| `pokeapi.max-concurrent-calls` | `50` | Max simultaneous calls to PokéAPI |

Any property can be overridden with an environment variable, e.g. `POKEAPI_MAX_CONCURRENT_CALLS=20`.

---

## Tests and coverage

```bash
./mvnw verify
```

This runs all tests, generates the coverage report in `target/site/jacoco/index.html` and **fails the build if line coverage is below 90%** (currently **96.5%**).

| Kind | What | Where |
|---|---|---|
| Unit | Ranking rules (order, limit 5, ties, empty) | `model/PokemonRankingsTest` |
| Unit (Mockito) | Service with a mocked loader and cache, startup trigger, health indicator, retry/skip/concurrency of the loader | `service/…`, `startup/…`, `health/…`, `PokemonLoaderTest` |
| HTTP client (WireMock) | Real HTTP calls to a fake PokéAPI: parsing, mapping, `null` base experience, 404/500 | `PokeApiClientTest` |
| Web layer | JSON, 200 / 503 / 500 / 404 | `PokemonControllerTest` |
| Architecture (ArchUnit) | The layer rules below | `ArchitectureTest` |
| **Integration** | The whole app against a fake PokéAPI: 503 while loading → 200, exact top-5 results, a Pokémon that fails once (retried) and one that always fails (skipped), PokéAPI is called only at startup, readiness probe | `PokemonRankingsIntegrationTest` |

---

## Bruno collection

The `bruno/` folder is a [Bruno](https://www.usebruno.com/) collection to check the API against a running instance. It has one request per endpoint (rankings, health, info) plus a few error cases (unknown path, wrong method, unsupported `Accept`, non-exposed actuator endpoint). Every request has assertions on the expected status and body.

To use it: start the app, then in Bruno choose **Open Collection**, select the `bruno/` folder and pick the `local` environment. The address lives in a single `baseUrl` variable (`bruno/environments/local.yml`); to test another instance, add an environment with a different `baseUrl`. With the [Bruno CLI](https://docs.usebruno.com/bru-cli/overview) you can run everything from a terminal: `cd bruno && bru run --env local`. The folder is not part of the Docker image.

---

## Architecture and decisions

I used the classic **layered MVC structure** of Spring applications. A request goes through the same few layers every time, and each layer only talks to the next one:

```
com.alea.pokemon
├── controller   PokemonController (the 3 endpoints), PokemonResponse (the JSON we send)
├── exception    GlobalExceptionHandler (clean JSON errors), RankingsNotReadyException, PokemonSourceException
├── service      PokemonRankingsService: builds the rankings once and serves them
├── model        Pokemon, PokemonRankings (builds the 3 top-5 lists with streams)
├── cache        RankingsCache: keeps the finished rankings in memory
├── client       ← the ONLY package that knows PokéAPI
│   ├── PokemonLoader       gets every Pokémon: virtual threads + concurrency limit + retries
│   ├── PokeApiClient       the HTTP calls
│   ├── PokeApiProperties / PokeApiConfig   settings and HTTP client
│   └── dto                 PokéAPI JSON shapes
├── health       RankingsHealthIndicator (actuator readiness)
└── startup      RankingsStartupTrigger (starts the loading when the app is ready)
```

```
 startup ──▶ PokemonRankingsService.load() ──▶ PokemonLoader ──▶ PokeApiClient ──▶ PokéAPI
                       │                         (virtual threads)
                       ▼
              PokemonRankings.from(list) ──▶ RankingsCache
                                                   ▲
 HTTP ──▶ PokemonController ──▶ PokemonRankingsService.heaviest() / highest() / mostBaseExperience()
```

What keeps it tidy:

- **PokéAPI is isolated.** Its URLs, JSON shapes (`base_experience`) and quirks (`null` values) live in the `client` package. The service only receives our own `Pokemon` objects from `PokemonLoader`.
- **The model is plain Java**, so the ranking rules are easy to read and test without Spring.
- **The service does not know the web**, and the web does not know PokéAPI.
- **It cannot rot silently:** `ArchitectureTest` fails the build if a rule is broken, for example if the service uses `PokeApiClient` directly, if any class outside `client` uses a PokéAPI DTO, or if the model imports Spring.

**Why MVC and not ports & adapters (hexagonal)?** I first built this project with hexagonal architecture and then simplified it. For a service with three read-only endpoints, interfaces with a single implementation (a source port, a store port, two use-case interfaces) added indirection and about five extra classes without giving anything the project needs today. The trade-off: swapping PokéAPI for another source, or the cache for Redis, now means introducing an interface at that moment, which is a small change.

### Decisions

| Decision | Why |
|---|---|
| Load everything **at startup, in the background** | Requests are instant and never depend on PokéAPI being up. The server starts immediately and answers `503` until the data is ready (my choice, see assumptions). |
| **Virtual threads** (one per Pokémon) | Each call mostly waits for the network; virtual threads make 1351 simultaneous waits cheap, with simple blocking code (no reactive code). |
| A **concurrency limit** (50 by default) | Being kind to PokéAPI. Configurable. |
| **Retry 3 times, then skip** the Pokémon and log a warning | One broken Pokémon should not stop the whole service. The trade-off is explained below. |
| `base_experience` of `null` counts as **0** | PokéAPI has a few forms without a value. Done in the `client` package, so the model never sees `null`. |
| **Ties** are broken by name (A→Z) | The result is always the same for the same data. |
| Hand-written retry loop instead of Spring Retry | Fewer concepts to learn, and easy to test. |
| In-memory cache without Spring Cache/Caffeine | The data is loaded once and never expires; the simplest thing that works. |
| Separate `PokemonResponse` for the JSON | The API format can change without touching the model. |
| `RestClient` (Spring) with the JDK HTTP client | Blocking calls are fine with virtual threads. |
| Standard `ProblemDetail` for errors | Same JSON shape for our errors and Spring's. |
| `wiremock-standalone` | Bundles its own libraries, so it does not clash with Spring Boot 4's Jackson 3. |

### Things worth knowing about the data

- **All 1351 entries of PokéAPI's `/pokemon` are used**, including alternate forms (`*-mega`, `*-gmax`, `deoxys-attack`...).
- Because of that, **the heaviest ranking is five Gigantamax forms tied at weight 10000** (there are more than five tied, so the name tie-break decides which five are shown). If you only want "main" Pokémon, that is a one-line filter in the `client` package (see improvements).
- If a Pokémon fails all its attempts it is skipped, so in that rare case the rankings could miss it. It is logged as `WARN Skipping <url> after 3 failed attempts`.

---

## Improvements I would make with more time

- **Refresh the data** periodically (e.g. `@Scheduled` daily), swapping the cache atomically so there is never a `503` after the first load. PokéAPI data rarely changes, so the current "load once at startup" is acceptable for now.
- **Retry the initial list call** too (today, if that single call fails, the service stays in `503` and logs an error until restarted), and expose a way to reload. This is the most important production gap.
- **Make skipped Pokémon visible**: a metric/health detail like "loaded 1349 of 1351", and optionally refuse to become ready below a threshold.
- **Option to exclude alternate forms** (`/pokemon-species` or `is_default`), possibly via a query parameter such as `?forms=false`.
- **Metrics and tracing** with Micrometer (load duration, number of retries, skipped Pokémon), structured JSON logs.
- **Retry with exponential backoff and jitter**, and respect for `Retry-After` / rate-limit responses.
- Add a `Retry-After` header to the `503` answers.
- **OpenAPI / Swagger UI** documentation.
- **CI pipeline** (build, tests, coverage gate, Docker image) and a Kubernetes manifest using the liveness/readiness probes that are already exposed.
- **Build the Docker image in CI** and add a container `HEALTHCHECK`.
- A **contract test** against the real PokéAPI (run on a schedule, not on every build) to detect upstream changes early.
- Cache the downloaded data on disk so a restart with PokéAPI down can still serve the last known rankings.
- Add a **branch coverage** limit to the JaCoCo gate (today only line coverage is enforced).

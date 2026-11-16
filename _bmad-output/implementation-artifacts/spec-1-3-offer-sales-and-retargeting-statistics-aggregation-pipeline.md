---
title: 'Story 1.3: Offer Sales and Retargeting Statistics Aggregation Pipeline'
type: 'feature'
created: '2026-11-16'
status: 'done'
baseline_commit: 'ff9b38cb7f738cfebfea0d4d5fe7f9a683056a8a'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The service now needs the analytical statistics capability (`FR-7` / `FR5`) to report how many offers of a specific `offerId` and `action` were sold (`CHECKED_OUT`), left in `ACTIVE` carts, or abandoned in `EVICTED` carts during a time period `[from, to]`, along with `retargetingCustomerIds` for discount outreach, plus `docker-compose.yml` and `README.md`.

**Approach:** Implement `CustomOfferStatisticsRepositoryImpl` in `com.retailsystem.cart.repository.impl` using a native `MongoTemplate` aggregation pipeline (`$match` -> `$unwind` -> `$match` -> `$group` -> `$project` with `$setDifference`), `OfferStatisticsResponse` in `dto.read`, `StatisticsService` and `StatisticsServiceImpl`, `StatisticsController` (`GET /api/v1/statistics/offers`), `docker-compose.yml` + `Dockerfile`, `README.md`, and `OfferStatisticsIntegrationTest` using Testcontainers MongoDB.

## Boundaries & Constraints

**Always:**
- Enforce `AD-2`: Define `OfferStatisticsResponse` as an immutable Java `record` in `com.retailsystem.cart.dto.read`.
- Enforce `AD-3` & `AD-4`: Implement `CustomOfferStatisticsRepositoryImpl` in `com.retailsystem.cart.repository.impl`, have `ShoppingCartRepository` extend `CustomOfferStatisticsRepository`, and execute the statistics calculation inside MongoDB using `MongoTemplate.aggregate(...)` without loading cart collections into JVM memory (`NFR-1`).
- Enforce `FR-7` retargeting logic: `retargetingCustomerIds` must contain distinct `customerId`s who added a matching `(offerId, action)` item within `[from, to]` in an `ACTIVE` or `EVICTED` cart, excluding any `customerId` who also completed a `CHECKED_OUT` purchase of that `(offerId, action)` within `[from, to]`.
- Validate query parameters (`offerId` non-blank, `action` non-null, `from` and `to` non-null with `!from.isAfter(to)`), returning `400 Bad Request` (`ProblemDetail`) when violated.
- Enforce `AD-6`: Zero AI comments or redundant Javadocs in `.java` files; verify end-to-end statistics and retargeting via Testcontainers MongoDB (`OfferStatisticsIntegrationTest`).

**Never:**
- Do not load all `ShoppingCart` documents into Java memory to filter or count offers.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Query statistics across sold, active, and evicted carts (`FR-7`) | `GET /api/v1/statistics/offers?offerId=tv-55&action=ADD&from=...&to=...` with matching `CHECKED_OUT`, `ACTIVE`, and `EVICTED` carts in window | Returns `200 OK` with accurate `soldCount`, `activeCartCount`, `evictedCartCount`, `totalAddedCount`, and `retargetingCustomerIds` | N/A |
| Exclude converted buyers from `retargetingCustomerIds` (`AC-1.3.2`) | Customer `cust-1` evicted a cart with `tv-55` (`ADD`) and later checked out a new cart with `tv-55` (`ADD`) in `[from, to]` | `soldCount` and `evictedCartCount` both increment, but `cust-1` is excluded from `retargetingCustomerIds` | N/A |
| Time window filtering (`FR-7`) | Items added before `from` or after `to`, or with a different `offerId` or `action` | Excluded from all counts and `retargetingCustomerIds` | N/A |
| Query period with zero matches (`FR-7`) | Valid `offerId`, `action`, `from`, `to` with no matching items | Returns `200 OK` with `0` for all counts and empty `retargetingCustomerIds` (`[]`) | N/A |
| Invalid query parameters (`FR-7`) | Blank `offerId`, invalid `action`, missing `from`/`to`, or `from > to` | Request rejected | `400 Bad Request` (`ProblemDetail`) |

</frozen-after-approval>

## Code Map

- `src/main/java/com/retailsystem/cart/repository/ShoppingCartRepository.java` -- Extend `CustomOfferStatisticsRepository` alongside `MongoRepository<ShoppingCart, String>`.
- `src/main/java/com/retailsystem/cart/repository/impl/CustomOfferStatisticsRepositoryImpl.java` -- Native MongoDB aggregation pipeline implementation for `aggregateOfferStatistics`.
- `src/main/java/com/retailsystem/cart/dto/read/OfferStatisticsResponse.java` -- Read DTO record for `FR-7` statistics response.
- `src/main/java/com/retailsystem/cart/service/StatisticsService.java` -- Service interface for offer statistics.
- `src/main/java/com/retailsystem/cart/service/impl/StatisticsServiceImpl.java` -- Service implementation validating time range and delegating to repository aggregation.
- `src/main/java/com/retailsystem/cart/controller/StatisticsController.java` -- REST controller for `GET /api/v1/statistics/offers`.
- `src/main/java/com/retailsystem/cart/exception/GlobalExceptionHandler.java` -- Ensure `MissingServletRequestParameterException` and `MethodArgumentTypeMismatchException` return `400 Bad Request` `ProblemDetail`.
- `src/test/java/com/retailsystem/cart/integration/OfferStatisticsIntegrationTest.java` -- Testcontainers MongoDB integration test verifying all `FR-7` statistics and retargeting scenarios.
- `Dockerfile`, `docker-compose.yml`, `README.md` -- Containerization and solution presentation documentation.

## Tasks & Acceptance

**Execution:**
- [x] `src/main/java/com/retailsystem/cart/repository/ShoppingCartRepository.java`, `src/main/java/com/retailsystem/cart/repository/impl/CustomOfferStatisticsRepositoryImpl.java` -- Implement the MongoDB aggregation pipeline (`$match` -> `$unwind` -> `$match` -> `$group` -> `$project` with `$setDifference`) -- Enforces `AD-4`, `NFR-1`, `AC-1.3.1`, and `AC-1.3.2`.
- [x] `src/main/java/com/retailsystem/cart/dto/read/OfferStatisticsResponse.java`, `src/main/java/com/retailsystem/cart/service/StatisticsService.java`, `src/main/java/com/retailsystem/cart/service/impl/StatisticsServiceImpl.java` -- Implement `OfferStatisticsResponse`, `StatisticsService`, and `StatisticsServiceImpl` with parameter validation -- Enforces `AD-2`, `AD-3`, and `FR-7`.
- [x] `src/main/java/com/retailsystem/cart/controller/StatisticsController.java`, `src/main/java/com/retailsystem/cart/exception/GlobalExceptionHandler.java` -- Expose `GET /api/v1/statistics/offers` and map query parameter binding errors to `400 Bad Request` `ProblemDetail` -- Enforces `FR-7` and `NFR-4`.
- [x] `src/test/java/com/retailsystem/cart/integration/OfferStatisticsIntegrationTest.java` -- Write Testcontainers MongoDB integration tests covering all I/O & Edge-Case Matrix scenarios -- Verifies `AC-1.3.1` through `AC-1.3.3`.
- [x] `Dockerfile`, `docker-compose.yml`, `README.md` -- Add Docker runtime files and architectural presentation README -- Completes Deliverable 3 and `AC-1.3.3`.

**Acceptance Criteria:**
- Given carts in `CHECKED_OUT`, `ACTIVE`, and `EVICTED` states, when `GET /api/v1/statistics/offers` is queried for an `offerId`, `action`, and `[from, to]` window, then the MongoDB aggregation pipeline returns accurate counts (`soldCount`, `activeCartCount`, `evictedCartCount`, `totalAddedCount`) (`AC-1.3.1`).
- Given customers with `ACTIVE` or `EVICTED` carts vs `CHECKED_OUT` carts in `[from, to]`, when statistics are queried, then `retargetingCustomerIds` includes only customers who have not purchased the offer in that period (`AC-1.3.2`).
- Given the complete test suite, when executed via `.\mvnw.cmd test`, then all unit and Testcontainers integration tests pass (`AC-1.3.3`).

## Implementation Notes

- Extended `ShoppingCartRepository` with `CustomOfferStatisticsRepository` and implemented `CustomOfferStatisticsRepositoryImpl` in `com.retailsystem.cart.repository.impl` using a 5-stage `MongoTemplate` aggregation pipeline (`$match` with `$elemMatch` -> `$unwind` -> `$match` -> `$group` -> `$project` with `$setDifference`).
- Created `OfferStatisticsResponse` in `com.retailsystem.cart.dto.read`, `StatisticsService`, `StatisticsServiceImpl` (with validation for blank `offerId`, null parameters, and `from > to`), and `StatisticsController` (`GET /api/v1/statistics/offers`).
- Updated `GlobalExceptionHandler` to map `MissingServletRequestParameterException` and `MethodArgumentTypeMismatchException` to `400 Bad Request` RFC 9457 `ProblemDetail`.
- Added `OfferStatisticsIntegrationTest` using Testcontainers `mongo:8.0` (`@ServiceConnection`) plus `Dockerfile`, `docker-compose.yml`, and `README.md`.

## Spec Change Log

## Review Triage Log

- [edge-case-hunter / blind-hunter] `CustomOfferStatisticsRepositoryImpl.java` `Date.from` overflow and `retargetingCustomerIds` ordering — verdict: `low` (route: `patch`). Added `IllegalArgumentException | ArithmeticException` guard throwing `DomainValidationException` and sorted `retargetingCustomerIds` deterministically.
- [blind-hunter] `GlobalExceptionHandler.java` `MethodArgumentTypeMismatchException` detail message — verdict: `low` (route: `patch`). Added dedicated handler returning `"Invalid value for parameter: " + ex.getName()`.
- [edge-case-hunter / blind-hunter] `OfferStatisticsResponse.java` defensive copy — verdict: `low` (route: `patch`). Added compact constructor defensively copying `retargetingCustomerIds`.
- [blind-hunter] Missing `.dockerignore` alongside `Dockerfile` — verdict: `low` (route: `patch`). Added `.dockerignore` excluding `target/`, `.git/`, `_bmad/`, `_bmad-output/`, and `.agents/`.
- [blind-hunter] Extra boundary/micro-test permutations — verdict: `false`. Existing Testcontainers integration test covers happy path, retargeting exclusion, empty window, and invalid parameters per `AGENTS.md` Rule 3.

## Verification

**Commands:**
- `.\mvnw.cmd test` -- expected: `BUILD SUCCESS` with all unit and integration tests passing.

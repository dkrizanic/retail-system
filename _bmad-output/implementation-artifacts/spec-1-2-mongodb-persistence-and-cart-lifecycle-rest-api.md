---
title: 'Story 1.2: MongoDB Persistence and Cart Lifecycle REST API'
type: 'feature'
created: '2026-11-16'
status: 'done'
baseline_commit: 'a6737a7cd5e6bacf1185b6f1ddbd8d2c79b813e8'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** With the rich domain entities in place from Story 1.1, the service now needs MongoDB index/converter configuration (`Decimal128`, partial unique `ACTIVE` cart index), separated Write and Read DTO models, the `CartService` interface and implementation, the `CartController` REST endpoints (`FR-1` through `FR-5`), RFC 9457 `ProblemDetail` error handling, and Testcontainers integration tests.

**Approach:** Implement `MongoConfig` (`BigDecimal <-> Decimal128` converters and `shopping_carts` indexes), separated Write (`dto.write`) and Read (`dto.read`) DTO records, `CartService` and `CartServiceImpl`, `CartController` (`/api/v1/carts/{customerId}`), `GlobalExceptionHandler` returning RFC 9457 `ProblemDetail`, `application.yml`, and `ShoppingCartIntegrationTest` using Testcontainers MongoDB (`@ServiceConnection`).

## Boundaries & Constraints

**Always:**
- Enforce `AD-2`: Use immutable Java `record` types in `com.retailsystem.cart.dto.write` (`AddCartItemRequest`, sealed `PriceWriteDto` with `OneTimePriceWriteDto` and `RecurringPriceWriteDto` discriminated by `type` = `ONE_TIME` | `RECURRING`) and `com.retailsystem.cart.dto.read` (`ShoppingCartResponse`, `CartItemResponse`, sealed `PriceReadDto` with `OneTimePriceReadDto` and `RecurringPriceReadDto`).
- Enforce `AD-3` & `AD-4`: `CartController` depends on `CartService` interface (`com.retailsystem.cart.service.CartService`), implemented by `CartServiceImpl` (`com.retailsystem.cart.service.impl.CartServiceImpl`), using `ShoppingCartRepository.findByCustomerIdAndStatus(customerId, CartStatus.ACTIVE)`.
- Enforce `AD-5`: Configure `BigDecimal <-> Decimal128` Mongo custom conversions and ensure indexes on `shopping_carts`:
  1. Partial unique index on `{ "customerId": 1 }` with `partialFilterExpression: { "status": "ACTIVE" }`.
  2. Compound index on `{ "items.offerId": 1, "items.action": 1, "items.addedAt": 1, "status": 1 }`.
- Enforce `AD-6`: Zero AI comments or redundant Javadocs in `.java` files; verify end-to-end cart lifecycle via Testcontainers MongoDB (`ShoppingCartIntegrationTest`).

**Never:**
- Do not implement `StatisticsController` or `StatisticsService` in Story 1.2 (those belong to Story 1.3).
- Do not write bloated micro-tests for every individual annotation permutation.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Add item to customer without active cart (`FR-2`) | `POST /api/v1/carts/{customerId}/items` with valid `offerId`, `action`, `prices` (`ONE_TIME` and `RECURRING`) | Creates `ACTIVE` cart, persists item with `Decimal128` prices, returns `200 OK` with `ShoppingCartResponse` | N/A |
| Get active cart (`FR-1`) | `GET /api/v1/carts/{customerId}` when `ACTIVE` cart exists | Returns `200 OK` with `ShoppingCartResponse` and all items | N/A |
| Get active cart when none exists (`FR-1`) | `GET /api/v1/carts/{customerId}` with no `ACTIVE` cart | Request rejected | `404 Not Found` (`ProblemDetail`) |
| Remove existing item (`FR-3`) | `DELETE /api/v1/carts/{customerId}/items/{itemId}` | Removes item from `ACTIVE` cart, returns `200 OK` with updated `ShoppingCartResponse` | N/A |
| Remove unknown item (`FR-3`) | `DELETE /api/v1/carts/{customerId}/items/{unknownId}` | Request rejected | `404 Not Found` (`ProblemDetail`) |
| Evict active cart (`FR-4`) | `DELETE /api/v1/carts/{customerId}` on `ACTIVE` cart | Transitions cart to `EVICTED` in MongoDB, returns `204 No Content`; subsequent `GET` returns `404` | N/A |
| Checkout non-empty active cart (`FR-5`) | `POST /api/v1/carts/{customerId}/checkout` on `ACTIVE` cart with `>= 1` item | Transitions cart to `CHECKED_OUT` with `completedAt`, returns `200 OK` with `ShoppingCartResponse` | N/A |
| Checkout empty active cart (`FR-5`) | `POST /api/v1/carts/{customerId}/checkout` on `ACTIVE` cart with `0` items | Request rejected | `409 Conflict` (`ProblemDetail`) |
| Add item with invalid payload (`FR-6`) | Blank `offerId`, empty `prices`, `value <= 0`, or `recurrences <= 0` | Request rejected | `400 Bad Request` (`ProblemDetail`) |

</frozen-after-approval>

## Code Map

- `src/main/resources/application.yml` -- Spring Boot configuration (`spring.mongodb.uri`, ProblemDetail support).
- `src/main/java/com/retailsystem/cart/config/MongoConfig.java` -- `MongoCustomConversions` (`BigDecimal <-> Decimal128`) and programmatic index creation on `shopping_carts`.
- `src/main/java/com/retailsystem/cart/dto/write/PriceWriteDto.java` -- Sealed polymorphic write DTO (`@JsonTypeInfo` on `type`: `ONE_TIME`, `RECURRING`).
- `src/main/java/com/retailsystem/cart/dto/write/OneTimePriceWriteDto.java` -- Write DTO record for one-time price.
- `src/main/java/com/retailsystem/cart/dto/write/RecurringPriceWriteDto.java` -- Write DTO record for recurring price.
- `src/main/java/com/retailsystem/cart/dto/write/AddCartItemRequest.java` -- Write DTO record for adding an item to a cart.
- `src/main/java/com/retailsystem/cart/dto/read/PriceReadDto.java` -- Sealed polymorphic read DTO (`ONE_TIME`, `RECURRING`).
- `src/main/java/com/retailsystem/cart/dto/read/OneTimePriceReadDto.java` -- Read DTO record for one-time price.
- `src/main/java/com/retailsystem/cart/dto/read/RecurringPriceReadDto.java` -- Read DTO record for recurring price.
- `src/main/java/com/retailsystem/cart/dto/read/CartItemResponse.java` -- Read DTO record for a cart item.
- `src/main/java/com/retailsystem/cart/dto/read/ShoppingCartResponse.java` -- Read DTO record for a shopping cart.
- `src/main/java/com/retailsystem/cart/service/CartService.java` -- Service interface for cart lifecycle operations.
- `src/main/java/com/retailsystem/cart/service/impl/CartServiceImpl.java` -- Implementation of `CartService`.
- `src/main/java/com/retailsystem/cart/controller/CartController.java` -- REST controller for `/api/v1/carts/{customerId}`.
- `src/main/java/com/retailsystem/cart/exception/GlobalExceptionHandler.java` -- `@RestControllerAdvice` mapping domain and validation exceptions to RFC 9457 `ProblemDetail`.
- `src/test/java/com/retailsystem/cart/integration/ShoppingCartIntegrationTest.java` -- End-to-end Testcontainers MongoDB integration test for cart lifecycle and error handling.

## Tasks & Acceptance

**Execution:**
- [x] `src/main/resources/application.yml`, `src/main/java/com/retailsystem/cart/config/MongoConfig.java` -- Configure MongoDB connection settings, `BigDecimal <-> Decimal128` converters, partial unique index (`{ customerId: 1 }` where `status == "ACTIVE"`), and compound analytics index -- Enforces `AD-5`, `NFR-2`, and `NFR-3`.
- [x] `src/main/java/com/retailsystem/cart/dto/write/PriceWriteDto.java`, `src/main/java/com/retailsystem/cart/dto/write/OneTimePriceWriteDto.java`, `src/main/java/com/retailsystem/cart/dto/write/RecurringPriceWriteDto.java`, `src/main/java/com/retailsystem/cart/dto/write/AddCartItemRequest.java` -- Implement Write DTO records with polymorphic Jackson annotations and validation -- Enforces `AD-2` and `FR-2`/`FR-6`.
- [x] `src/main/java/com/retailsystem/cart/dto/read/PriceReadDto.java`, `src/main/java/com/retailsystem/cart/dto/read/OneTimePriceReadDto.java`, `src/main/java/com/retailsystem/cart/dto/read/RecurringPriceReadDto.java`, `src/main/java/com/retailsystem/cart/dto/read/CartItemResponse.java`, `src/main/java/com/retailsystem/cart/dto/read/ShoppingCartResponse.java` -- Implement Read DTO records and static `from(ShoppingCart)` factory mapping -- Enforces `AD-2`.
- [x] `src/main/java/com/retailsystem/cart/service/CartService.java`, `src/main/java/com/retailsystem/cart/service/impl/CartServiceImpl.java` -- Implement `CartService` interface and `CartServiceImpl` (`getActiveCart`, `addItem`, `removeItem`, `evictCart`, `checkoutCart`) -- Enforces `AD-3` and `AD-4`.
- [x] `src/main/java/com/retailsystem/cart/controller/CartController.java`, `src/main/java/com/retailsystem/cart/exception/GlobalExceptionHandler.java` -- Implement `CartController` REST endpoints (`FR-1`..`FR-5`) and `GlobalExceptionHandler` (`ProblemDetail` for `400`, `404`, `409`) -- Enforces `NFR-4`.
- [x] `src/test/java/com/retailsystem/cart/integration/ShoppingCartIntegrationTest.java` -- Implement Testcontainers MongoDB integration test covering the I/O & Edge-Case Matrix -- Verifies `AC-1.2.1` through `AC-1.2.4`.

**Acceptance Criteria:**
- Given inbound and outbound cart API payloads, when serialized/deserialized, then separated `dto.write` and `dto.read` records handle polymorphic `ONE_TIME` and `RECURRING` prices (`AC-1.2.1`).
- Given MongoDB startup, when `MongoConfig` runs, then the partial unique index on `ACTIVE` carts and `Decimal128` converters are active (`AC-1.2.2`).
- Given active cart lookups, when `CartServiceImpl` queries MongoDB, then it uses `ShoppingCartRepository.findByCustomerIdAndStatus` (`AC-1.2.3`).
- Given the REST endpoints `/api/v1/carts/{customerId}`, when tested with Testcontainers MongoDB via `.\mvnw.cmd test`, then all cart lifecycle operations and `ProblemDetail` error responses pass (`AC-1.2.4`).

## Implementation Notes

- Configured `application.yml` with `spring.mongodb.uri` and RFC 9457 `ProblemDetail` support.
- Added `MongoConfig` with `BigDecimal <-> Decimal128` `MongoCustomConversions` and programmatic index initialization for `idx_active_customer_cart` (partial unique on `status == "ACTIVE"`) and `idx_offer_statistics` (compound analytics index).
- Implemented immutable Write (`dto.write`) and Read (`dto.read`) DTO records with polymorphic Jackson `@JsonTypeInfo`/`@JsonSubTypes` for `ONE_TIME` and `RECURRING` prices.
- Implemented `CartService`, `CartServiceImpl`, `CartController`, and `GlobalExceptionHandler` returning RFC 9457 `ProblemDetail` for `400`, `404`, and `409` responses.
- Added `ShoppingCartIntegrationTest` using Testcontainers MongoDB (`mongo:8.0` via `@ServiceConnection`) verifying full cart lifecycle, `Decimal128` BSON storage, partial unique index enforcement, and error responses.

## Spec Change Log

## Review Triage Log

- [blind-hunter] `ShoppingCartResponse.java` uses `id` instead of `cartId` — verdict: `medium` (route: `patch`). Renamed `id` component to `cartId` to match `FR-1` and `ARCHITECTURE-SPINE.md`.
- [blind-hunter] `CartServiceImpl.java` blank `customerId` check and empty-cart eviction — verdict: `low` (route: `patch`). Added blank `customerId` validation (`DomainValidationException`) in `findActiveCartOrThrow` and deleted empty active carts on `evictCart`.
- [blind-hunter] `GlobalExceptionHandler.java` raw Mongo/Jackson exception messages — verdict: `low` (route: `patch`). Sanitized `DuplicateKeyException`, `OptimisticLockingFailureException`, and `HttpMessageNotReadableException` detail strings.
- [edge-case-hunter / blind-hunter] `MongoConfig.java` `Decimal128` rounding and redundant cast — verdict: `low` (route: `patch`). Added `MathContext.DECIMAL128` rounding and `mongoTemplate.indexOps(ShoppingCart.class)`.
- [blind-hunter] Obsolete `.gitkeep` files in populated packages — verdict: `low` (route: `patch`). Removed `.gitkeep` files from populated directories.
- [blind-hunter] Concurrent `addItem` retry loop and extra test permutations — verdict: `low` (rejected). Optimistic locking / unique index returns `409 Conflict` by design (`NFR-2`), and extra micro-tests are excluded by `AGENTS.md` Rule 3.

## Verification

**Commands:**
- `.\mvnw.cmd test` -- expected: `BUILD SUCCESS` with all unit and Testcontainers integration tests passing.

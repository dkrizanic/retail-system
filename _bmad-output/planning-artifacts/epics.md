---
title: Shopping Cart Microservice - Epics and Stories
status: final
stepsCompleted:
  - step-01-validate-prerequisites
  - step-02-design-epics
  - step-03-create-stories
  - step-04-final-validation
created: 2026-11-16
updated: 2026-11-16
---

# Shopping Cart Microservice — Epics and User Stories

## Epic 1: Shopping Cart Lifecycle & Offer Retargeting Statistics

Deliver the complete Shopping Cart microservice (`FR-1` through `FR-7`, `NFR-1` through `NFR-4`) following Pragmatic Layered Architecture with Rich Entities (`AD-1` through `AD-6`).

### Story 1.1: Project Foundation and Rich Domain Entities

**As a** platform engineer,
**I want** rich domain entities (`ShoppingCart`, `CartItem`, `Price`, `OneTimePrice`, `RecurringPrice`) in `entity` and isolated enums (`ItemAction`, `RecurrenceUnit`, `CartStatus`) in `enums` within a Spring Boot `4.1.2` / Java `26` Maven project,
**So that** all business rules (`BR1`, `BR2`, `FR-6`) and cart lifecycle transitions are enforced directly on the entities without duplicate model/document mapper bloat (`AD-1`, `AD-3`, `AD-6`).

**Acceptance Criteria:**
- **AC-1.1.1 (BR1 Minimum Price Invariant):** Given a `CartItem` creation request, when the `prices` list is null or empty, then `DomainValidationException` is thrown.
- **AC-1.1.2 (BR2 Mandatory Item & Price Invariants):** Given `OneTimePrice` or `RecurringPrice` construction, when `value <= 0`, `recurrences <= 0`, `unit` is null, or `offerId`/`customerId` is blank, then `DomainValidationException` is thrown.
- **AC-1.1.3 (Cart Lifecycle State Transitions):** Given an `ACTIVE` `ShoppingCart`, when `addItem`, `removeItem`, `evict`, or `checkout` is invoked, then the cart enforces state rules (rejecting mutations on non-`ACTIVE` carts and rejecting `checkout` on an empty cart with `InvalidCartStateException`).
- **AC-1.1.4 (Unit Test Verification):** Pure JUnit 5 unit tests in `src/test/java/com/retailsystem/cart/unit` verify all entity invariants and state transitions with zero Spring context startup and zero inline AI comments.

### Story 1.2: MongoDB Persistence and Cart Lifecycle REST API

**As a** digital channel API client,
**I want** REST endpoints to retrieve, add items to, remove items from, evict, and check out a customer's `ACTIVE` shopping cart (`FR-1` through `FR-5`),
**So that** customers can manage their shopping baskets with atomic persistence and RFC 9457 `ProblemDetail` error responses (`AD-2`, `AD-3`, `AD-4`, `AD-5`).

**Acceptance Criteria:**
- **AC-1.2.1 (Separated Write & Read DTOs):** Inbound requests use immutable Java records in `dto.write` (`AddCartItemRequest`, polymorphic `PriceWriteDto` for `ONE_TIME` and `RECURRING`); outbound responses use immutable Java records in `dto.read` (`ShoppingCartResponse`, `CartItemResponse`, `PriceReadDto`).
- **AC-1.2.2 (MongoDB Partial Unique Index & Decimal128):** `shopping_carts` enforces a partial unique index on `{ customerId: 1 }` where `status == "ACTIVE"`, `@Version` optimistic locking, and `Decimal128` conversion for `BigDecimal` prices.
- **AC-1.2.3 (Simple Derived ORM Query):** Retrieving the customer's active cart uses the descriptive Spring Data repository interface method `findByCustomerIdAndStatus(String customerId, CartStatus status)` in `repository.ShoppingCartRepository`.
- **AC-1.2.4 (REST Endpoints & ProblemDetail):** `GET /api/v1/carts/{customerId}`, `POST /api/v1/carts/{customerId}/items`, `DELETE /api/v1/carts/{customerId}/items/{itemId}`, `DELETE /api/v1/carts/{customerId}`, and `POST /api/v1/carts/{customerId}/checkout` return expected status codes (`200 OK`, `204 No Content`) and RFC 9457 `ProblemDetail` on `400`/`404`/`409`.

### Story 1.3: Offer Sales and Retargeting Statistics Aggregation Pipeline

**As a** commercial and marketing analyst,
**I want** to query offer statistics by `offerId`, `action`, and `[from, to]` time period (`FR-7`),
**So that** I can track confirmed sales (`soldCount`), active/evicted cart funnel counts (`activeCartCount`, `evictedCartCount`, `totalAddedCount`), and retrieve `retargetingCustomerIds` for sending discount offers (`AD-4`, `AD-5`, `AD-6`).

**Acceptance Criteria:**
- **AC-1.3.1 (Indexed MongoDB Aggregation Pipeline):** `CustomOfferStatisticsRepositoryImpl` in `repository.impl` executes a native MongoDB aggregation pipeline (`$match` -> `$unwind` -> `$match` -> `$group` -> `$project` with `$setDifference`) backed by compound index `{ "items.offerId": 1, "items.action": 1, "items.addedAt": 1, "status": 1 }`.
- **AC-1.3.2 (Retargeting Lead Exclusion):** Customers who placed the offer in an `ACTIVE` or `EVICTED` cart during `[from, to]` are returned in `retargetingCustomerIds`, unless they also completed a `CHECKED_OUT` purchase of that offer during `[from, to]`.
- **AC-1.3.3 (Docker & Testcontainers Verification):** `docker-compose.yml` is provided for local startup, and end-to-end Testcontainers (`@ServiceConnection`) integration tests in `src/test/java/com/retailsystem/cart/integration` verify both the full cart lifecycle and the statistics aggregation pipeline.

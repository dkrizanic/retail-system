# Epic 1 Context: Shopping Cart Lifecycle & Offer Retargeting Statistics

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Deliver the complete Shopping Cart microservice that manages active customer shopping carts, enforces polymorphic price and item validation rules, transitions carts through `ACTIVE`, `CHECKED_OUT`, and `EVICTED` states, and provides high-performance MongoDB aggregation statistics on sold offers, active/evicted cart funnel counts, and customer IDs eligible for discount retargeting.

## Stories

- Story 1.1: Project Foundation and Rich Domain Entities
- Story 1.2: MongoDB Persistence and Cart Lifecycle REST API
- Story 1.3: Offer Sales and Retargeting Statistics Aggregation Pipeline

## Requirements & Constraints

- **FR-1 (Get Cart):** `GET /api/v1/carts/{customerId}` returns the customer's `ACTIVE` shopping cart (`200 OK`), or `404 Not Found` (`ProblemDetail`) when no `ACTIVE` cart exists.
- **FR-2 (Add Item):** `POST /api/v1/carts/{customerId}/items` adds a validated cart item to the customer's `ACTIVE` cart (creating an `ACTIVE` cart automatically if none exists), assigning a UUID `itemId` and UTC `addedAt` timestamp.
- **FR-3 (Remove Item):** `DELETE /api/v1/carts/{customerId}/items/{itemId}` removes the specified item from the customer's `ACTIVE` cart (`200 OK`), or returns `404 Not Found`.
- **FR-4 (Evict Cart):** `DELETE /api/v1/carts/{customerId}` transitions the customer's `ACTIVE` cart to `EVICTED` (recording `completedAt` or deleting if empty) and returns `204 No Content`, preserving cart intent for abandonment analytics.
- **FR-5 (Checkout Cart):** `POST /api/v1/carts/{customerId}/checkout` transitions a non-empty `ACTIVE` cart to `CHECKED_OUT` with `completedAt`, marking its items as sold (`200 OK`), or returns `409 Conflict` if the cart is empty.
- **FR-6 (Domain Invariants BR1 & BR2):**
  - Every `CartItem` requires a non-blank `offerId`, a valid `ItemAction` (`ADD`, `MODIFY`, `DELETE`), and at least one `Price` (`prices.size >= 1`).
  - `OneTimePrice` (`ONE_TIME`) requires `value > 0` (`BigDecimal`).
  - `RecurringPrice` (`RECURRING`) requires `recurrences > 0` (`int`), a valid `RecurrenceUnit` (`DAYS`, `MONTHS`, `YEARS`), and `value > 0` (`BigDecimal`).
- **FR-7 (Offer Sales & Retargeting Statistics):** `GET /api/v1/statistics/offers?offerId={id}&action={action}&from={isoInstant}&to={isoInstant}` returns `soldCount` (`CHECKED_OUT`), `activeCartCount` (`ACTIVE`), `evictedCartCount` (`EVICTED`), `totalAddedCount`, and `retargetingCustomerIds` (distinct customers who added the offer in `ACTIVE` or `EVICTED` state during `[from, to]` and did not complete a `CHECKED_OUT` purchase of that offer during `[from, to]`).

## Technical Decisions

- **Stack:** Java `26` (`25 LTS` compatible), Spring Boot `4.1.2`, Maven (`mvnw`), MongoDB `8.0` (Docker & Testcontainers with `@ServiceConnection`).
- **AD-1 (Rich Entities & Isolated Enums):** Rich entities (`ShoppingCart`, `CartItem`, `Price`, `OneTimePrice`, `RecurringPrice`) live in `com.retailsystem.cart.entity` and enums (`ItemAction`, `RecurrenceUnit`, `CartStatus`) live in `com.retailsystem.cart.enums`, without duplicate model/document mapper bloat. All invariants throw `DomainValidationException`.
- **AD-2 (Separated Write & Read DTOs):** Inbound payloads use immutable records in `dto.write` (`AddCartItemRequest`, sealed `PriceWriteDto`); outbound payloads use immutable records in `dto.read` (`ShoppingCartResponse`, `CartItemResponse`, sealed `PriceReadDto`, `OfferStatisticsResponse`).
- **AD-3 (Interface-Driven Boundaries):** Service interfaces (`CartService`, `StatisticsService`) live in `service` with implementations in `service.impl`; repository interfaces (`ShoppingCartRepository`, `CustomOfferStatisticsRepository`) live in `repository` with custom aggregation implementation in `repository.impl`.
- **AD-4 (Dual Query Strategy):** Simple cart lookups use Spring Data derived methods (`findByCustomerIdAndStatus`); statistics queries use `MongoTemplate` aggregation pipeline (`$match` -> `$unwind` -> `$match` -> `$group` -> `$project` with `$setDifference`).
- **AD-5 (MongoDB Schema & Indexes):** Single `shopping_carts` collection with partial unique index on `{ customerId: 1 }` where `status == "ACTIVE"`, compound index on `{ "items.offerId": 1, "items.action": 1, "items.addedAt": 1, "status": 1 }`, `@Version` optimistic locking, and `Decimal128 <-> BigDecimal` converters.
- **AD-6 (Code & Testing Discipline):** Zero inline AI comments or redundant Javadocs in `.java` files. Unit tests in `src/test/java/com/retailsystem/cart/unit` cover entity invariants without Spring context; integration tests in `src/test/java/com/retailsystem/cart/integration` use Testcontainers MongoDB.

## Cross-Story Dependencies

- Story 1.1 defines the Maven build, rich entities (`ShoppingCart`, `CartItem`, `Price`), enums, exceptions, and repository interfaces required by Story 1.2 and Story 1.3.
- Story 1.2 implements the MongoDB persistence, indexes, and cart lifecycle endpoints that populate the `shopping_carts` collection queried by Story 1.3.

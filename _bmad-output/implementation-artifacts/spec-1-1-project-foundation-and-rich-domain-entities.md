---
title: 'Story 1.1: Project Foundation and Rich Domain Entities'
type: 'feature'
created: '2026-11-16'
status: 'ready-for-dev'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/AGENTS.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** The repository currently lacks the Maven build configuration (`Spring Boot 4.1.2`, `Java 26`) and the rich domain entities and enums needed to enforce shopping cart, cart item, and polymorphic price invariants (`BR1`, `BR2`, `FR-6`) and cart lifecycle state transitions (`ACTIVE`, `CHECKED_OUT`, `EVICTED`).

**Approach:** Initialize the Spring Boot `4.1.2` / Java `26` Maven project with Maven Wrapper (`mvnw`) and implement the rich entities in `com.retailsystem.cart.entity` (`ShoppingCart`, `CartItem`, sealed `Price`, `OneTimePrice`, `RecurringPrice`), isolated enums in `com.retailsystem.cart.enums` (`ItemAction`, `RecurrenceUnit`, `CartStatus`), domain exceptions in `com.retailsystem.cart.exception`, repository interfaces in `com.retailsystem.cart.repository`, and fast unit tests in `src/test/java/com/retailsystem/cart/unit`.

## Boundaries & Constraints

**Always:**
- Enforce `AD-1`: Place `ShoppingCart`, `CartItem`, `Price`, `OneTimePrice`, and `RecurringPrice` in `com.retailsystem.cart.entity`, and `ItemAction`, `RecurrenceUnit`, and `CartStatus` in `com.retailsystem.cart.enums`, with zero duplicate model/document mapper bloat.
- Enforce `BR1` & `BR2` inside entity constructors/methods (`offerId` and `customerId` non-blank, `prices` non-empty with no null elements, `Price.value > 0`, `RecurringPrice.recurrences > 0`, non-null `ItemAction` and `RecurrenceUnit`), throwing `DomainValidationException` on violation.
- Enforce `AD-3`: Define repository interfaces (`ShoppingCartRepository`, `CustomOfferStatisticsRepository`) in `com.retailsystem.cart.repository`.
- Enforce `AD-6`: Write zero AI inline comments, unfinished task markers, or redundant Javadocs in any `.java` source or test file, and place unit tests in `src/test/java/com/retailsystem/cart/unit`.

**Never:**
- Do not add REST controllers or service/aggregation implementations in Story 1.1 (those belong to Story 1.2 and Story 1.3).
- Do not start a Spring application context in Story 1.1 unit tests.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Create valid `OneTimePrice` | `value = 499.99` | Immutable `OneTimePrice` created | N/A |
| Invalid `OneTimePrice` value | `value = null`, `0`, or `-10.00` | Construction rejected | Throws `DomainValidationException` |
| Create valid `RecurringPrice` | `recurrences = 12`, `unit = MONTHS`, `value = 29.99` | Immutable `RecurringPrice` created | N/A |
| Invalid `RecurringPrice` attributes | `recurrences <= 0`, `unit = null`, or `value <= 0` | Construction rejected | Throws `DomainValidationException` |
| Create `CartItem` without prices (`BR1`) | `offerId = "tv-1"`, `action = ADD`, `prices = []` or `null` | Construction rejected | Throws `DomainValidationException` |
| Add item to `ACTIVE` `ShoppingCart` | Valid `offerId`, `action`, `prices` | Item appended with generated `itemId` and UTC `addedAt`; `updatedAt` refreshed | N/A |
| Remove existing item from `ACTIVE` cart | Existing `itemId` | Item removed from cart; `updatedAt` refreshed | N/A |
| Remove unknown item from `ACTIVE` cart | Unknown `itemId` | Removal rejected | Throws `ResourceNotFoundException` |
| Evict `ACTIVE` cart | `ACTIVE` cart | `status` transitions to `EVICTED`, `completedAt` set | N/A |
| Checkout non-empty `ACTIVE` cart | `ACTIVE` cart with `>= 1` item | `status` transitions to `CHECKED_OUT`, `completedAt` set | N/A |
| Checkout empty `ACTIVE` cart | `ACTIVE` cart with `0` items | Checkout rejected | Throws `InvalidCartStateException` |
| Mutate non-`ACTIVE` cart | Cart in `CHECKED_OUT` or `EVICTED` status | Any `addItem`, `removeItem`, `evict`, or `checkout` call rejected | Throws `InvalidCartStateException` |

</frozen-after-approval>

## Code Map

- `pom.xml` -- Maven project configuration (`spring-boot-starter-parent:4.1.2`, `java.version: 26`, web, validation, data-mongodb, springdoc-openapi, testcontainers).
- `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` -- Maven Wrapper scripts for reproducible builds.
- `src/main/java/com/retailsystem/cart/ShoppingCartApplication.java` -- Spring Boot entry point.
- `src/main/java/com/retailsystem/cart/exception/DomainValidationException.java` -- Thrown when `BR1`/`BR2` or query parameters are invalid.
- `src/main/java/com/retailsystem/cart/exception/ResourceNotFoundException.java` -- Thrown when a cart or cart item is not found.
- `src/main/java/com/retailsystem/cart/exception/InvalidCartStateException.java` -- Thrown when an invalid lifecycle transition is attempted.
- `src/main/java/com/retailsystem/cart/enums/ItemAction.java` -- Enum (`ADD`, `MODIFY`, `DELETE`).
- `src/main/java/com/retailsystem/cart/enums/RecurrenceUnit.java` -- Enum (`DAYS`, `MONTHS`, `YEARS`).
- `src/main/java/com/retailsystem/cart/enums/CartStatus.java` -- Enum (`ACTIVE`, `CHECKED_OUT`, `EVICTED`).
- `src/main/java/com/retailsystem/cart/entity/Price.java` -- Sealed interface permitting `OneTimePrice` and `RecurringPrice`.
- `src/main/java/com/retailsystem/cart/entity/OneTimePrice.java` -- Record implementing `Price` with `BR2` validation.
- `src/main/java/com/retailsystem/cart/entity/RecurringPrice.java` -- Record implementing `Price` with `BR2` validation.
- `src/main/java/com/retailsystem/cart/entity/CartItem.java` -- Rich entity enforcing `BR1` and `BR2`.
- `src/main/java/com/retailsystem/cart/entity/ShoppingCart.java` -- Rich `@Document` entity managing cart items, lifecycle status, and optimistic lock `@Version`.
- `src/main/java/com/retailsystem/cart/repository/CustomOfferStatisticsRepository.java` -- Custom repository interface for offer statistics queries.
- `src/main/java/com/retailsystem/cart/repository/ShoppingCartRepository.java` -- Spring Data `MongoRepository` interface with `findByCustomerIdAndStatus`.
- `src/test/java/com/retailsystem/cart/unit/PriceAndCartItemValidationTest.java` -- Unit tests for `Price`, `OneTimePrice`, `RecurringPrice`, and `CartItem` invariants.
- `src/test/java/com/retailsystem/cart/unit/ShoppingCartTest.java` -- Unit tests for `ShoppingCart` lifecycle transitions.

## Tasks & Acceptance

**Execution:**
- [ ] `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties`, `src/main/java/com/retailsystem/cart/ShoppingCartApplication.java` -- Initialize Spring Boot `4.1.2` / Java `26` Maven project with Maven Wrapper -- Provides verified build and dependency management across stories.
- [ ] `src/main/java/com/retailsystem/cart/exception/DomainValidationException.java`, `src/main/java/com/retailsystem/cart/exception/ResourceNotFoundException.java`, `src/main/java/com/retailsystem/cart/exception/InvalidCartStateException.java` -- Create domain runtime exceptions -- Enables expressive domain error signaling mapped to HTTP `ProblemDetail` in Story 1.2.
- [ ] `src/main/java/com/retailsystem/cart/enums/ItemAction.java`, `src/main/java/com/retailsystem/cart/enums/RecurrenceUnit.java`, `src/main/java/com/retailsystem/cart/enums/CartStatus.java`, `src/main/java/com/retailsystem/cart/entity/Price.java`, `src/main/java/com/retailsystem/cart/entity/OneTimePrice.java`, `src/main/java/com/retailsystem/cart/entity/RecurringPrice.java` -- Implement isolated enums and sealed polymorphic `Price` value objects -- Enforces `BR2` price invariants at construction time.
- [ ] `src/main/java/com/retailsystem/cart/entity/CartItem.java`, `src/main/java/com/retailsystem/cart/entity/ShoppingCart.java` -- Implement `CartItem` and `ShoppingCart` rich entities -- Enforces `BR1`, `BR2`, and cart lifecycle transitions (`addItem`, `removeItem`, `evict`, `checkout`).
- [ ] `src/main/java/com/retailsystem/cart/repository/CustomOfferStatisticsRepository.java`, `src/main/java/com/retailsystem/cart/repository/ShoppingCartRepository.java` -- Define repository interfaces -- Establishes clean interface boundaries (`AD-3`, `AD-4`).
- [ ] `src/test/java/com/retailsystem/cart/unit/PriceAndCartItemValidationTest.java`, `src/test/java/com/retailsystem/cart/unit/ShoppingCartTest.java` -- Write pure JUnit 5 unit tests covering all I/O & Edge-Case Matrix scenarios -- Verifies `AC-1.1.1` through `AC-1.1.4` without Spring context startup.

**Acceptance Criteria:**
- Given a `CartItem` creation request, when `prices` is null or empty, then `DomainValidationException` is thrown (`AC-1.1.1`).
- Given `OneTimePrice` or `RecurringPrice` construction, when `value <= 0`, `recurrences <= 0`, `unit` is null, or `offerId`/`customerId` is blank, then `DomainValidationException` is thrown (`AC-1.1.2`).
- Given an `ACTIVE` `ShoppingCart`, when `addItem`, `removeItem`, `evict`, or `checkout` is called, then state invariants are enforced and invalid transitions throw `InvalidCartStateException` or `ResourceNotFoundException` (`AC-1.1.3`).
- Given the unit tests in `com.retailsystem.cart.unit`, when executed via `./mvnw test`, then all unit tests pass with zero Spring context startup and zero AI comments (`AC-1.1.4`).

## Implementation Notes

## Spec Change Log

## Review Triage Log

## Verification

**Commands:**
- `.\mvnw.cmd test` -- expected: `BUILD SUCCESS` with all unit tests passing.

# PRD Addendum: Technical & Architectural Context

This addendum captures the technical constraints, architectural patterns, and engineering standards contributed during discovery so downstream BMad workflows (`bmad-architecture`, `bmad-create-epics-and-stories`, `bmad-build`) preserve them verbatim.

---

## 1. Technology Stack Decisions

- **Java Version**: Java `26` (Temurin `26.0.1` installed locally; compatible with Java `25 LTS`).
- **Framework**: Spring Boot `4.1.2` (Spring Framework 7, Jakarta EE 11, Jackson 3).
- **Build Tool**: Maven with Maven Wrapper (`mvnw` / `mvnw.cmd`).
- **Database**: MongoDB `8.0 LTS` / `9.0` via Docker (`docker-compose.yml`) and Testcontainers (`@ServiceConnection`).

---

## 2. Architectural Pattern & Coding Rules (`AGENTS.md`)

1. **Pragmatic Layered Architecture with Rich Entities**:
   - `controller`: REST controllers (`CartController`, `StatisticsController`).
   - `dto`: Separated **Write Models (`dto.write`)** and **Read Models (`dto.read`)**.
   - `service` & `service.impl`: Interfaces (`CartService`, `StatisticsService`) in `service` and implementations (`CartServiceImpl`, `StatisticsServiceImpl`) in `service.impl`.
   - `entity`: Rich entities (`ShoppingCart` `@Document`, `CartItem`, and sealed `Price` value objects `OneTimePrice`, `RecurringPrice`) enforcing business invariants directly without duplicate model/document mapper bloat.
   - `enums`: Dedicated package for domain enums (`ItemAction`, `RecurrenceUnit`, `CartStatus`).
   - `repository` & `repository.impl`:
     - **Simple queries**: Spring Data MongoDB repository interface (`ShoppingCartRepository`) with descriptive derived methods (`findByCustomerIdAndStatus`).
     - **Complex queries**: Custom repository interface (`CustomOfferStatisticsRepository`) and implementation (`CustomOfferStatisticsRepositoryImpl`) in `repository.impl` using `MongoTemplate` aggregation pipeline for `FR-7` statistics.
   - `exception`: Domain exceptions and `GlobalExceptionHandler` returning RFC 9457 `ProblemDetail`.
   - `config`: `MongoConfig` for indexes and `Decimal128` monetary converters.
2. **Zero AI Comments in Code**:
   - No inline comments, no redundant Javadocs, no unfinished task markers in `.java` source or test files.
3. **Pragmatic Testing**:
   - Unit tests (`src/test/.../unit`) strictly for entity validation and cart lifecycle rules (`Price`, `CartItem`, `ShoppingCart`).
   - Integration tests (`src/test/.../integration`) with Testcontainers MongoDB for end-to-end API flows and statistics aggregation.

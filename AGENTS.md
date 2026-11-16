# Agent Rules & Engineering Standards

This repository follows the **BMAD (Breakthrough Method for Agile AI-Driven Development)** workflow. All requirements, architectural decisions, and task breakdowns live in `_bmad-output/` and `docs/`.

---

## 1. Source of Truth & Code Comments

- **Zero AI Comments in Code**: Do not write explanatory comments, Javadocs that merely repeat method names, unfinished task markers, or step-by-step inline narration in source code or tests.
- **Self-Documenting Code**: Express intent through clear domain naming, small focused methods, plain entity types, and expressive tests.
- **Markdown is the Source of Truth**: All architectural rationale, business rules, domain constraints, and edge-case explanations belong in `_bmad-output/` and `docs/`.

---

## 2. Architecture & Code Quality Standards

- **Stack**: Java 26 (25 LTS compatible), Spring Boot 4.1.x, Maven, and MongoDB (8.0 LTS / 9.0).
- **Architecture Pattern**: **Pragmatic Layered Architecture with Plain Entities**:
  - `controller`: REST controllers handling HTTP routing and status codes.
  - `dto`: Separated **Write Models (`dto.write`)** for request payloads and **Read Models (`dto.read`)** for response payloads (using immutable Java `record` types).
  - `service` & `service.impl`: Service interfaces in `service` (`CartService`, `StatisticsService`) and their implementations in `service.impl` (`CartServiceImpl`, `StatisticsServiceImpl`).
  - `entity`: Plain data entities (`ShoppingCart` `@Document`, `CartItem`, and `Price` records) with no business logic, validation, or lifecycle methods. They are persisted directly without duplicate model/document mapper bloat.
  - `enums`: Isolated package for domain enums (`ItemAction`, `RecurrenceUnit`, `CartStatus`).
  - `repository` & `repository.impl`:
    - `repository`: Spring Data MongoDB repository interfaces using descriptive derived query methods (e.g., `findByCustomerIdAndStatus`) for simple queries, plus custom statistics repository interface (`CustomOfferStatisticsRepository`).
    - `repository.impl`: Custom repository implementation (`CustomOfferStatisticsRepositoryImpl`) using `MongoTemplate` aggregation pipeline for complex analytical queries.
  - `exception`: Domain exceptions and `GlobalExceptionHandler` returning RFC 9457 `ProblemDetail` responses.
  - `config`: MongoDB index and `Decimal128` converter configuration.
- **Docker & Testcontainers**:
  - Provide `docker-compose.yml` for local MongoDB and service execution.
  - Use **Testcontainers** (`@ServiceConnection` with MongoDB container) for integration tests.
- **Performance & Scalability**:
  - Push filtering and aggregation to MongoDB using indexed aggregation pipelines (never load large datasets into memory to count or filter).
  - Design stateless application instances ready for horizontal scaling.
  - Use `BigDecimal` (stored as `Decimal128` in MongoDB) for all monetary values.

---

## 3. Testing Strategy (Pragmatic, High-Value Only)

- **No Test Bloat**: Do not write redundant tests for framework behavior, getters/setters, every HTTP status code permutation, or trivial one-line annotations.
- **Unit Tests (`src/test/.../unit`)**:
  - Focus strictly on **validation logic** in `validation` (`CartValidator`, `StatisticsValidator`: price rules, item requiring at least one price, cart state rules).
  - Fast, isolated, zero Spring context startup.
- **Integration Tests (`src/test/.../integration`)**:
  - Focus on **end-to-end API flows and database queries** (adding/removing items, evicting/checking out a cart, and verifying the MongoDB statistics aggregation over time periods using Testcontainers).
  - One cohesive happy-path + critical failure test per major flow rather than dozens of micro-tests for every field validation.

---

## 4. Communication, Git & Naming Conventions

- **Plain English**: Write all documentation, commit messages, PR descriptions, and API responses in clear, direct, plain English. Avoid filler buzzwords.
- **Commit Messages**: Follow **Conventional Commits** in plain, concise lower-case English:
  - Format: `<type>(<scope>): <short summary in imperative mood>`
  - Allowed types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`
  - Rules:
    - Subject line max 72 characters, no trailing period.
    - Body (optional): 1–3 bullet points explaining *why* the change was made, never *what* files changed.
- **PR / Story Descriptions**:
  - **Goal**: One sentence in plain English stating what business capability this delivers.
  - **Changes**: Short bullet list of key architectural or functional changes.
  - **Verification**: Which unit or integration tests verify the change.

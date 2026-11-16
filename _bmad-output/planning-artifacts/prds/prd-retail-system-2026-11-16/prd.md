---
title: Shopping Cart Microservice
status: final
created: 2026-11-16
updated: 2026-11-16
---

# PRD: Shopping Cart Microservice

## 0. Document Purpose

This PRD defines the product capabilities, domain rules, user journeys, and operational boundaries for the **Shopping Cart Microservice** within the digital retail platform. It is written for product stakeholders, system architects, and backend engineers, and serves as the authoritative requirement baseline for downstream BMad workflows (`bmad-architecture`, `bmad-create-epics-and-stories`, and `bmad-build`). Vocabulary is anchored in **§3 Glossary**, functional requirements (`FR-1` through `FR-7`) are grouped by feature in **§4 Features**, and inferred decisions are tagged inline with `[ASSUMPTION]` and indexed in **§9 Assumptions Index**. Technical implementation choices and architectural trade-offs are captured in the companion [addendum.md](./addendum.md).

---

## 1. Vision

The **Shopping Cart Microservice** is the core order-capture component of a digital retail system that enables customers to purchase products and manage subscriptions across digital channels. Operating in a microservice ecosystem alongside an external **Product Catalog**, this service manages active customer **Shopping Carts**, enforces strict pricing and item validation rules, and transitions carts through their lifecycle from active shopping to checkout or eviction.

Beyond basic cart storage, a retail business loses significant revenue when customers place offers into a **Shopping Cart** and leave without completing a purchase. If a cart service simply deletes evicted carts or fails to distinguish between items added to a cart and offers actually sold, the business is blind to both true sales performance and cart abandonment.

By tracking the complete **Cart Status** lifecycle (`ACTIVE`, `CHECKED_OUT`, `EVICTED`) and recording item-level timestamps, the **Shopping Cart Microservice** delivers two high-value outcomes from a single bounded context: accurate time-windowed **Offer Statistics** for completed sales, and actionable **Retargeting Leads** identifying customers who placed an **Offer** in their cart without purchasing so commercial teams can reach out with targeted discounts.

---

## 2. Target User

### 2.1 Jobs To Be Done
- **Digital Retail Customer (via API client / channel frontend)**:
  - Assemble a basket of new purchases (`ADD`), subscription upgrades (`MODIFY`), or cancellations (`DELETE`) with clear one-time and recurring pricing before committing to an order.
  - Modify the basket by removing individual items or clearing the entire cart to start fresh.
- **Commercial & Marketing Analyst**:
  - Measure how many **Offers** of a specific identifier and **Item Action** were sold (`CHECKED_OUT`) during any time period.
  - Identify how many **Offers** are sitting in `ACTIVE` or `EVICTED` carts during a time period and retrieve the associated **Customer IDs** (`Retargeting Leads`) to send promotional discount offers.
- **Platform / Engineering Reviewer**:
  - Verify a clean, self-documenting microservice boundary that validates every mandatory domain invariant and scales horizontally without in-memory data bottlenecks.

### 2.2 Non-Users (v1)
- **Catalog Merchandisers**: Product names, descriptions, images, and base catalog pricing definitions live exclusively in the external **Product Catalog** service.
- **End Consumers Interacting via Browser UI**: This service exposes backend REST APIs only; graphical user interfaces are out of scope.

### 2.3 Key User Journeys

- **UJ-1. Marko configures a home entertainment bundle and completes checkout.**
  - **Persona + context:** Marko, an existing digital telecom customer, is upgrading his broadband plan and buying a new Philips TV through the digital channel.
  - **Entry state:** Authenticated on the digital channel with `customerId = "cust-marko-101"` and no existing `ACTIVE` **Shopping Cart**.
  - **Path:**
    1. Marko adds a Philips TV **Offer** (`offerId = "tv-philips-55"`, `action = ADD`) with a **One-Time Price** (`value = 599.99`). The system automatically creates an `ACTIVE` **Shopping Cart** and adds the **Cart Item**.
    2. Marko adds a fiber upgrade **Offer** (`offerId = "fiber-1gbps"`, `action = MODIFY`) with both a **One-Time Price** (activation fee `19.99`) and a **Recurring Price** (`24 MONTHS` at `34.99`).
    3. Marko retrieves his `ACTIVE` **Shopping Cart** to review both **Cart Items** and their **Prices**.
    4. Marko submits checkout for his **Shopping Cart**.
  - **Climax:** The **Shopping Cart** transitions from `ACTIVE` to `CHECKED_OUT` with a completion timestamp, confirming both **Offers** as sold.
  - **Resolution:** Marko's order items are permanently recorded as sold for **Offer Statistics**, and any subsequent item addition for Marko will open a fresh `ACTIVE` **Shopping Cart**.
  - **Edge case:** If Marko attempts to check out when he has no `ACTIVE` **Shopping Cart**, the system rejects the request with `404 Not Found`.

- **UJ-2. Ana abandons a cart and becomes a Retargeting Lead for a discount.**
  - **Persona + context:** Ana (`customerId = "cust-ana-202"`) is browsing a premium streaming package (`offerId = "stream-max"`, `action = ADD`, `Recurring Price` of `12 MONTHS` at `14.99`) but hesitates on price.
  - **Entry state:** Ana adds `"stream-max"` to her `ACTIVE` **Shopping Cart**, then decides to clear/evict her cart before buying.
  - **Path:**
    1. Ana adds the `"stream-max"` **Cart Item** to her `ACTIVE` **Shopping Cart**.
    2. Ana removes an unwanted accessory **Cart Item** by its `itemId`, leaving `"stream-max"` in the cart.
    3. Ana evicts her **Shopping Cart** (`DELETE /api/v1/carts/cust-ana-202`).
    4. Later that day, Elena (a Growth Marketing Analyst) queries **Offer Statistics** for `offerId = "stream-max"`, `action = ADD` over today's time window.
  - **Climax:** Instead of losing Ana's intent when the cart was evicted, the **Shopping Cart** transitioned to `EVICTED`, and Elena's statistics query returns `soldCount = 0`, `evictedCartCount = 1`, and `retargetingCustomerIds = ["cust-ana-202"]`.
  - **Resolution:** Ana has no `ACTIVE` **Shopping Cart** blocking future shopping, and the marketing team has her `customerId` to send a targeted discount code.

---

## 3. Glossary

- **Shopping Cart** — The aggregate root representing a customer's basket of **Cart Items**. Identified by a unique `cartId` and associated with a single **Customer ID** and **Cart Status**. A customer may have at most one `ACTIVE` **Shopping Cart** at any given time.
- **Customer ID (`customerId`)** — Mandatory non-blank string identifier uniquely identifying the retail customer who owns a **Shopping Cart**.
- **Cart Status (`status`)** — Lifecycle state of a **Shopping Cart**:
  - `ACTIVE`: The customer's current mutable basket.
  - `CHECKED_OUT`: A finalized **Shopping Cart** whose **Cart Items** have been sold.
  - `EVICTED`: A cleared/abandoned **Shopping Cart** that is no longer active for the customer but retained for funnel analytics and **Retargeting Leads**.
- **Cart Item** — A line item inside a **Shopping Cart** identified by a unique `itemId`. Contains an **Offer ID**, an **Item Action**, a non-empty list of **Prices**, and an `addedAt` UTC timestamp.
- **Offer ID (`offerId`)** — Mandatory non-blank string identifier referencing a commercial product or subscription offer in the external **Product Catalog**.
- **Product Catalog** — External microservice responsible for product metadata (names, descriptions, specs). Out of scope for this service.
- **Item Action (`action`)** — Mandatory enum defining the commercial operation performed for a **Cart Item**:
  - `ADD`: Purchasing a new item or subscription.
  - `MODIFY`: Upgrading or changing an existing item or subscription.
  - `DELETE`: Removing or cancelling an existing item or subscription.
- **Price** — Polymorphic value object attached to a **Cart Item**. Must be either a **One-Time Price** (`ONE_TIME`) or a **Recurring Price** (`RECURRING`). Every **Cart Item** must have at least one **Price**.
- **One-Time Price (`ONE_TIME`)** — A **Price** consisting solely of a mandatory positive monetary `value` (`BigDecimal > 0`).
- **Recurring Price (`RECURRING`)** — A **Price** consisting of a mandatory positive integer `recurrences` (`> 0`), a mandatory **Recurrence Unit** (`unit`), and a mandatory positive monetary `value` (`BigDecimal > 0`).
- **Recurrence Unit (`unit`)** — Mandatory enum specifying the time unit for a **Recurring Price**: `DAYS`, `MONTHS`, or `YEARS` `[ASSUMPTION: A1]`.
- **Offer Statistics** — Time-windowed analytical summary for a given **Offer ID**, **Item Action**, and `[from, to]` period, reporting `soldCount`, `activeCartCount`, `evictedCartCount`, `totalAddedCount`, and `retargetingCustomerIds`.
- **Retargeting Leads (`retargetingCustomerIds`)** — Distinct **Customer IDs** who added a matching **Offer ID** and **Item Action** during the queried time window in an `ACTIVE` or `EVICTED` **Shopping Cart** and did not complete a `CHECKED_OUT` purchase of that offer during the window.

---

## 4. Features

### 4.1 Active Shopping Cart Management
**Description:** Enables digital channels to retrieve a customer's current `ACTIVE` **Shopping Cart**, add **Cart Items** with validated **Prices**, remove individual **Cart Items**, and evict or check out the **Shopping Cart**. Realizes **UJ-1** and **UJ-2**.

**Functional Requirements:**

#### FR-1: Get Active Shopping Cart Content
An API client can retrieve the current `ACTIVE` **Shopping Cart** for a given **Customer ID** (`GET /api/v1/carts/{customerId}`). Realizes **UJ-1**.

**Consequences (testable):**
- Returns `HTTP 200 OK` with the `ACTIVE` **Shopping Cart** (`cartId`, `customerId`, `status`, `items`, `createdAt`, `updatedAt`) when an `ACTIVE` cart exists for `customerId`.
- Returns `HTTP 404 Not Found` (RFC 9457 `ProblemDetail`) when no `ACTIVE` **Shopping Cart** exists for `customerId` `[ASSUMPTION: A2]`.

#### FR-2: Add Cart Item to Shopping Cart
An API client can add a **Cart Item** containing an **Offer ID**, **Item Action**, and one or more **Prices** to a customer's `ACTIVE` **Shopping Cart** (`POST /api/v1/carts/{customerId}/items`). Realizes **UJ-1**, **UJ-2**.

**Consequences (testable):**
- If no `ACTIVE` **Shopping Cart** exists for `customerId`, the system creates a new `ACTIVE` **Shopping Cart** automatically and appends the **Cart Item**.
- Assigns a unique `itemId` and records the current UTC `addedAt` timestamp on the new **Cart Item**.
- Returns `HTTP 200 OK` with the updated `ACTIVE` **Shopping Cart**.
- Rejects any request violating mandatory attribute or price rules (`FR-6`) with `HTTP 400 Bad Request`.

#### FR-3: Remove Cart Item from Shopping Cart
An API client can remove a specific **Cart Item** by `itemId` from a customer's `ACTIVE` **Shopping Cart** (`DELETE /api/v1/carts/{customerId}/items/{itemId}`). Realizes **UJ-2**.

**Consequences (testable):**
- Removes the matching **Cart Item** from the customer's `ACTIVE` **Shopping Cart** and returns `HTTP 200 OK` with the updated **Shopping Cart** `[ASSUMPTION: A3]`.
- Returns `HTTP 404 Not Found` if the customer has no `ACTIVE` **Shopping Cart** or if `itemId` does not exist in that cart.

#### FR-4: Evict Shopping Cart
An API client can evict (clear/abandon) a customer's `ACTIVE` **Shopping Cart** (`DELETE /api/v1/carts/{customerId}`). Realizes **UJ-2**.

**Consequences (testable):**
- Transitions the customer's `ACTIVE` **Shopping Cart** to `EVICTED` status and records `completedAt` (or deletes the document if it contained zero items), returning `HTTP 204 No Content`.
- Subsequent `GET /api/v1/carts/{customerId}` requests return `HTTP 404 Not Found` until a new **Cart Item** is added.
- Returns `HTTP 404 Not Found` if the customer has no `ACTIVE` **Shopping Cart** to evict.

#### FR-5: Checkout Shopping Cart
An API client can finalize a customer's `ACTIVE` **Shopping Cart** as sold (`POST /api/v1/carts/{customerId}/checkout`). Realizes **UJ-1**.

**Consequences (testable):**
- Transitions a non-empty `ACTIVE` **Shopping Cart** to `CHECKED_OUT` status, sets `completedAt`, and returns `HTTP 200 OK` with the finalized **Shopping Cart**.
- Returns `HTTP 404 Not Found` if no `ACTIVE` **Shopping Cart** exists for `customerId`.
- Returns `HTTP 409 Conflict` (or `400 Bad Request`) if the `ACTIVE` **Shopping Cart** has zero **Cart Items**.

---

### 4.2 Domain Invariants & Polymorphic Price Validation
**Description:** Enforces strict domain invariants (`BR1` and `BR2`) across all **Cart Items** and **Prices** both at API deserialization and inside the core domain model. Realizes **UJ-1**, **UJ-2**.

**Functional Requirements:**

#### FR-6: Mandatory Item and Polymorphic Price Validation
The system validates that every **Cart Item** and **Price** has all mandatory attributes populated with valid values before creating or mutating domain state. Realizes **UJ-1**, **UJ-2**.

**Consequences (testable):**
- Rejects a **Cart Item** with a null/blank `offerId`, null/invalid `action` (outside `ADD`, `MODIFY`, `DELETE`), or null/empty `prices` list (`size < 1`) with `HTTP 400 Bad Request`.
- Rejects a **One-Time Price** (`ONE_TIME`) with a null or non-positive `value` (`value <= 0`) with `HTTP 400 Bad Request` `[ASSUMPTION: A4]`.
- Rejects a **Recurring Price** (`RECURRING`) with `recurrences <= 0`, null/invalid `unit` (outside `DAYS`, `MONTHS`, `YEARS`), or null/non-positive `value` (`value <= 0`) with `HTTP 400 Bad Request`.

---

### 4.3 Offer Sales & Retargeting Statistics
**Description:** Provides analytical statistics on how many **Offers** of a given **Offer ID** and **Item Action** were sold (`CHECKED_OUT`), left in `ACTIVE` carts, or abandoned in `EVICTED` carts during a specified time window, along with **Retargeting Leads**. Realizes **UJ-1**, **UJ-2**.

**Functional Requirements:**

#### FR-7: Query Offer Sales and Cart Funnel Statistics by Period
An API client can query statistics for a specific `offerId`, `action`, and UTC time window `[from, to]` (`GET /api/v1/statistics/offers?offerId={id}&action={action}&from={isoInstant}&to={isoInstant}`). Realizes **UJ-1**, **UJ-2**.

**Consequences (testable):**
- Returns `HTTP 200 OK` with an **Offer Statistics** payload containing:
  - `offerId`, `action`, `from`, `to`
  - `soldCount`: Count of matching **Cart Items** (where `addedAt` is within `[from, to]`) belonging to `CHECKED_OUT` **Shopping Carts** `[ASSUMPTION: A5]`.
  - `activeCartCount`: Count of matching **Cart Items** within `[from, to]` in `ACTIVE` **Shopping Carts**.
  - `evictedCartCount`: Count of matching **Cart Items** within `[from, to]` in `EVICTED` **Shopping Carts**.
  - `totalAddedCount`: Sum of `soldCount + activeCartCount + evictedCartCount`.
  - `retargetingCustomerIds`: Distinct list of `customerId`s who have matching items in `ACTIVE` or `EVICTED` carts during `[from, to]` and did not purchase (`CHECKED_OUT`) the offer during `[from, to]`.
- Returns `0` counts and an empty `retargetingCustomerIds` list when no matching records exist in `[from, to]`.
- Returns `HTTP 400 Bad Request` if `offerId` is blank, `action` is invalid, `from`/`to` are missing, or `from` is after `to` (`from > to`).

---

## 5. Cross-Cutting NFRs

- **NFR-1 (Database-Side Aggregation Performance):** All `FR-7` statistics queries must execute inside MongoDB using indexed aggregation pipelines without loading full **Shopping Cart** collections into JVM memory.
- **NFR-2 (Single Active Cart Concurrency Integrity):** A MongoDB partial unique index on `{ customerId: 1 }` where `status == "ACTIVE"` and optimistic locking (`@Version`) must prevent duplicate `ACTIVE` carts or lost item updates under concurrent requests.
- **NFR-3 (Monetary Precision):** All `Price.value` monetary amounts must use exact decimal arithmetic (`BigDecimal` in Java mapped to BSON `Decimal128` in MongoDB).
- **NFR-4 (Standardized Error Contract):** Every `4xx`/`5xx` API error must return an RFC 9457 `application/problem+json` (`ProblemDetail`) response.

---

## 6. Non-Goals (Explicit)

- **No Product Catalog Sync or Validation:** The service does not call or replicate the external **Product Catalog** to verify whether an `offerId` exists or whether client-supplied **Prices** match catalog base prices.
- **No Payment Processing or Inventory Reservation:** Checkout (`FR-5`) records the commercial state transition of the **Shopping Cart** only; payment capture and stock locking belong to downstream order/payment microservices.
- **No Automated Discount Dispatch:** The service surfaces `retargetingCustomerIds` via the statistics endpoint (`FR-7`) so marketing/CRM services can send discounts; it does not send emails or SMS messages itself.

---

## 7. MVP Scope

### 7.1 In Scope
- REST API endpoints covering `FR-1` through `FR-7`.
- Pure DDD domain layer enforcing all `BR1` and `BR2` invariants.
- MongoDB persistence with partial unique index for `ACTIVE` carts and compound index for `FR-7` statistics aggregation.
- Automated unit tests for domain validation and Testcontainers integration tests for API and aggregation flows.
- `docker-compose.yml` for local one-command startup.

### 7.2 Out of Scope for MVP
- Frontend UI (`[NON-GOAL for MVP]`).
- Multi-currency conversion (all prices represent the platform's base currency).
- External message broker event publishing (e.g., Kafka `CartCheckedOutEvent` — deferred to v2 when downstream Order Management is integrated).

---

## 8. Success Metrics

**Primary**
- **SM-1**: **100% Domain Invariant Enforcement** — Zero invalid **Cart Items** or **Prices** (missing attributes, empty price lists, non-positive values) can be persisted via API or constructed in domain unit tests. Validates `FR-2`, `FR-6`.
- **SM-2**: **Accurate Sales & Retargeting Attribution** — 100% accuracy in distinguishing `soldCount` (`CHECKED_OUT`), `activeCartCount` (`ACTIVE`), `evictedCartCount` (`EVICTED`), and `retargetingCustomerIds` across time windows in integration tests. Validates `FR-4`, `FR-5`, `FR-7`.

**Secondary**
- **SM-3**: **Indexed Query Efficiency** — `FR-7` aggregation queries use MongoDB compound index prefix matching with zero full-collection in-memory scans in the JVM. Validates `FR-7`, `NFR-1`.

**Counter-metrics (do not optimize)**
- **SM-C1**: **Unit Test Count / Code Coverage Percentage** — Do not inflate test count with trivial getter/setter tests, framework annotation tests, or exhaustive HTTP status permutations (`AGENTS.md` rule). Counterbalances `SM-1`.

---

## 9. Open Questions

No phase-blocking open questions remain.

---

## 10. Assumptions Index

- **A1 (§3, §4.2):** **Recurrence Unit** is modeled as an explicit enum (`DAYS`, `MONTHS`, `YEARS`) paired with positive integer `recurrences` (e.g., `12 MONTHS`, `7 DAYS`), ensuring strict validation under `BR2`.
- **A2 (§4.1 FR-1):** Requesting `GET /api/v1/carts/{customerId}` when no `ACTIVE` cart exists returns `404 Not Found` rather than creating an empty cart on a read (`GET`) operation.
- **A3 (§4.1 FR-3):** Removing the last remaining **Cart Item** via `DELETE /api/v1/carts/{customerId}/items/{itemId}` leaves the `ACTIVE` **Shopping Cart** open with an empty `items` list; full cart eviction uses `DELETE /api/v1/carts/{customerId}` (`FR-4`).
- **A4 (§4.2 FR-6):** Monetary `value` on both **One-Time Price** and **Recurring Price** must be strictly positive (`value > 0`).
- **A5 (§4.3 FR-7):** Time-window filtering `[from, to]` in `FR-7` evaluates `CartItem.addedAt` (when the offer action was captured) across cart lifecycle states (`CHECKED_OUT`, `ACTIVE`, `EVICTED`), ensuring both checkout-based sales counts and immediate cart-addition counts are accurately reflected in the same time window.

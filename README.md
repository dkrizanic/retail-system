# Shopping Cart Microservice

A stateless Spring Boot 4.1 / Java 26 microservice backed by MongoDB 8.0 for managing customer shopping carts and running real-time offer sales and retargeting analytics.

---

## 1. Architecture & Design Decisions

The service uses a **Pragmatic Layered Architecture with Plain Entities**:

```text
com.retailsystem.cart
├── config          # MongoDB custom Decimal128 converters and index initialization
├── controller      # REST controllers (CartController, StatisticsController)
├── dto
│   ├── write       # Pure Java record request payloads with Bean Validation
│   └── read        # Pure Java record response payloads
├── entity          # Clean domain entities (ShoppingCart, CartItem, Price)
├── enums           # Domain enums (CartStatus, ItemAction, PriceType, RecurrenceUnit)
├── exception       # Domain exceptions mapped to RFC 9457 ProblemDetail responses
├── mapper          # MapStruct compile-time DTO <-> Entity mapper (CartMapper)
├── repository      # Spring Data MongoDB repositories (ShoppingCartRepository, CustomOfferStatisticsRepository)
│   └── impl        # Custom MongoTemplate aggregation pipeline (CustomOfferStatisticsRepositoryImpl)
├── service         # Service interfaces (CartService, StatisticsService)
│   └── impl        # Stateless service implementations (CartServiceImpl, StatisticsServiceImpl)
└── validation      # Dedicated validators (CartValidator, StatisticsValidator)
```

### Key Engineering Highlights

- **Dedicated Validation & MapStruct Mapping Layers**: `dto.write` and `dto.read` records are pure data structures, `CartMapper` (`@Mapper` via MapStruct) generates compile-time mappings, `ShoppingCart`/`CartItem`/`Price` are plain data holders, and `CartValidator` / `StatisticsValidator` in `validation` enforce input bounds, `BR1`/`BR2`, cart state transitions, and time-window invariants.
- **Separated Write and Read Models**: Inbound commands (`dto.write`) and outbound queries (`dto.read`) use distinct Java `record` types (`ONE_TIME` vs `RECURRING` via `PriceType`).
- **Exact Monetary Precision**: All price amounts use `BigDecimal` in Java and are stored as BSON `Decimal128` in MongoDB via custom converters.
- **Single Active Cart Guarantee & Optimistic Concurrency**:
  - A partial unique index (`idx_active_customer_cart`) on `{ customerId: 1 }` with filter `{ status: "ACTIVE" }` guarantees at the database level that a customer can have at most one `ACTIVE` cart at any time, while preserving unlimited historical `CHECKED_OUT` and `EVICTED` carts.
  - `@Version` optimistic locking with automatic bounded concurrency retries across cart mutations protects concurrent updates and returns `409 Conflict` (`ProblemDetail`) when retries are exhausted.
- **In-Database Offer Statistics & Retargeting Pipeline**:
  - Backed by compound multikey index `idx_offer_statistics` on `{ "items.offerId": 1, "items.action": 1, "items.addedAt": 1, "status": 1 }`.
  - Executes a 5-stage MongoDB aggregation pipeline (`$match` -> `$unwind` -> `$match` -> `$group` by `customerId` -> `$facet` for counts and bounded `$limit` retargeting leads) directly inside MongoDB with `allowDiskUse(true)` and `maxTime(5s)`.
  - Computes `soldCount`, `activeCartCount`, `evictedCartCount`, `totalAddedCount`, and `retargetingCustomerIds` (customers who placed the offer in an `ACTIVE` or `EVICTED` cart during `[from, to]` and did **not** purchase it in a `CHECKED_OUT` cart during the same period).

---

## 2. REST API Endpoints

When the application is running, interactive OpenAPI / Swagger documentation is available at:
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

### Cart Lifecycle (`CartController`)

| Method | Path | Description | Status Codes |
|--------|------|-------------|--------------|
| `GET` | `/api/v1/carts/{customerId}` | Retrieve the customer's `ACTIVE` shopping cart | `200 OK`, `404 Not Found` |
| `POST` | `/api/v1/carts/{customerId}/items` | Add an item to the customer's `ACTIVE` cart (lazily creates cart if none exists) | `200 OK`, `400 Bad Request`, `409 Conflict` |
| `DELETE` | `/api/v1/carts/{customerId}/items/{itemId}` | Remove an item from the customer's `ACTIVE` cart | `200 OK`, `404 Not Found` |
| `DELETE` | `/api/v1/carts/{customerId}` | Evict (`EVICTED`) the customer's `ACTIVE` cart while retaining history | `204 No Content`, `404 Not Found` |
| `POST` | `/api/v1/carts/{customerId}/checkout` | Transition a non-empty `ACTIVE` cart to `CHECKED_OUT` | `200 OK`, `404 Not Found`, `409 Conflict` |

### Offer Sales & Retargeting Statistics (`StatisticsController`)

| Method | Path | Query Parameters | Status Codes |
|--------|------|------------------|--------------|
| `GET` | `/api/v1/statistics/offers` | `offerId` (`String`), `action` (`ADD`\|`MODIFY`\|`DELETE`), `from` (`ISO-8601 Instant`), `to` (`ISO-8601 Instant`), optional `retargetingLimit` (`1`–`50000`, default `10000`) | `200 OK`, `400 Bad Request` |

#### Example: Add Item to Cart

```bash
curl -X POST http://localhost:8080/api/v1/carts/cust-101/items \
  -H "Content-Type: application/json" \
  -d '{
    "offerId": "fiber-1gbps",
    "action": "ADD",
    "prices": [
      { "type": "ONE_TIME", "value": 49.99 },
      { "type": "RECURRING", "recurrences": 12, "unit": "MONTHS", "value": 29.99 }
    ]
  }'
```

#### Example: Query Offer Statistics & Retargeting Leads

```bash
curl "http://localhost:8080/api/v1/statistics/offers?offerId=fiber-1gbps&action=ADD&from=2026-01-01T00:00:00Z&to=2026-12-31T23:59:59Z"
```

Example response:

```json
{
  "offerId": "fiber-1gbps",
  "action": "ADD",
  "from": "2026-01-01T00:00:00Z",
  "to": "2026-12-31T23:59:59Z",
  "soldCount": 14,
  "activeCartCount": 3,
  "evictedCartCount": 5,
  "totalAddedCount": 22,
  "retargetingCustomerIds": ["cust-102", "cust-108"]
}
```

---

## 3. Running & Testing

### Run with Docker Compose

```bash
docker compose up --build
```

This starts MongoDB 8.0 on port `27017` and the Shopping Cart microservice on port `8080`.

### Run Automated Tests

The test suite includes fast domain unit tests (`src/test/java/com/retailsystem/cart/unit`) and Testcontainers MongoDB 8.0 integration tests (`src/test/java/com/retailsystem/cart/integration`):

```bash
./mvnw test
```

On Windows PowerShell:

```powershell
.\mvnw.cmd test
```

### Run Large-Dataset Stress & Aggregation Benchmark

With the Docker Compose stack running (`docker compose up -d`), run the stress test script (`100,000` carts / `200,000+` items + `1,998` concurrent HTTP requests + `30` aggregation queries):

```bash
uv run scripts/stress_test.py
```

**Recorded Benchmark Results (`100,666` carts / `201,332` items on single local container)**:
- **Bulk MongoDB Seed**: `100,000` carts (`200,000` items) inserted in `6.05s` (`16,539 carts/sec`).
- **Concurrent Cart Lifecycle HTTP API (`32` threads, `1,998` requests)**: `415.6 req/s` (`p50: 51.70ms`, `p95: 94.01ms`, `0` errors).
- **Statistics Aggregation Pipeline (`GET /api/v1/statistics/offers` over `100,666` carts)**: `p50: 96.34ms`, `p95: 172.88ms` (computing `soldCount = 7,001`, `activeCartCount = 6,667`, `evictedCartCount = 334`, `totalAddedCount = 14,002`, and `7,001` deduplicated `retargetingCustomerIds`).

import concurrent.futures
import json
import random
import statistics
import subprocess
import time
import urllib.request
import urllib.error
from datetime import datetime, timezone

BASE_URL = "http://localhost:8080"
TOTAL_SEED_CARTS = 100_000
CONCURRENT_HTTP_REQUESTS = 2_000
CONCURRENCY_WORKERS = 32


def seed_large_dataset_in_mongodb(total_carts: int) -> float:
    print(f"\n[1/3] Seeding {total_carts:,} shopping carts (~{total_carts * 2:,} items) into MongoDB...")
    mongosh_script = f"""
    const total = {total_carts};
    const batchSize = 10000;
    const offers = ["tv-philips-55", "fiber-1gbps", "stream-max", "router-mesh-6e", "mobile-5g-unlimited"];
    const actions = ["ADD", "MODIFY", "DELETE"];
    const statuses = ["ACTIVE", "CHECKED_OUT", "EVICTED"];

    db.shopping_carts.deleteMany({{ customerId: /^stress-seed-/ }});

    let inserted = 0;
    for (let b = 0; b < total / batchSize; b++) {{
        const batch = [];
        for (let i = 0; i < batchSize; i++) {{
            const idx = b * batchSize + i;
            const status = statuses[idx % 3];
            const ts = new Date(Date.UTC(2026, 5, 1 + (idx % 28), idx % 24, idx % 60, 0));
            const offer1 = offers[idx % offers.length];
            const offer2 = offers[(idx + 1) % offers.length];
            const action1 = actions[idx % actions.length];
            const action2 = actions[(idx + 2) % actions.length];

            batch.push({{
                _id: "seed-cart-" + idx,
                customerId: "stress-seed-cust-" + idx,
                status: status,
                createdAt: ts,
                updatedAt: ts,
                completedAt: status === "ACTIVE" ? null : ts,
                version: Long("0"),
                items: [
                    {{
                        itemId: "seed-item-" + idx + "-1",
                        offerId: offer1,
                        action: action1,
                        addedAt: ts,
                        prices: [
                            {{ type: "ONE_TIME", value: NumberDecimal("499.99") }}
                        ]
                    }},
                    {{
                        itemId: "seed-item-" + idx + "-2",
                        offerId: offer2,
                        action: action2,
                        addedAt: ts,
                        prices: [
                            {{ type: "RECURRING", value: NumberDecimal("29.99"), recurrences: 24, unit: "MONTHS" }}
                        ]
                    }}
                ]
            }});
        }}
        db.shopping_carts.insertMany(batch, {{ ordered: false }});
        inserted += batch.length;
    }}
    print("Inserted carts: " + inserted);
    """
    start = time.perf_counter()
    subprocess.run(
        [
            "docker", "exec", "-i", "shopping-cart-mongodb",
            "mongosh", "shopping_cart_db", "--quiet", "--eval", mongosh_script
        ],
        check=True
    )
    elapsed = time.perf_counter() - start
    print(f"      Seeded {total_carts:,} carts ({total_carts * 2:,} items) in {elapsed:.2f}s ({total_carts / elapsed:,.0f} carts/sec)")
    return elapsed


def http_request(method: str, path: str, body: dict | None = None) -> tuple[int, float, dict | None]:
    url = f"{BASE_URL}{path}"
    data = json.dumps(body).encode("utf-8") if body is not None else None
    headers = {"Content-Type": "application/json"} if body is not None else {}
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    start = time.perf_counter()
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            raw = resp.read()
            latency_ms = (time.perf_counter() - start) * 1000.0
            parsed = json.loads(raw.decode("utf-8")) if raw else None
            return resp.status, latency_ms, parsed
    except urllib.error.HTTPError as e:
        latency_ms = (time.perf_counter() - start) * 1000.0
        return e.code, latency_ms, None


def run_customer_flow(worker_id: int) -> list[float]:
    latencies = []
    customer_id = f"stress-http-cust-{worker_id}-{random.randint(100000, 999999)}"
    payload = {
        "offerId": "tv-philips-55",
        "action": "ADD",
        "prices": [
            {"type": "ONE_TIME", "value": 599.99},
            {"type": "RECURRING", "value": 19.99, "recurrences": 12, "unit": "MONTHS"}
        ]
    }

    status, lat, _ = http_request("POST", f"/api/v1/carts/{customer_id}/items", payload)
    assert status == 200, f"Expected 200 on addItem, got {status}"
    latencies.append(lat)

    status, lat, _ = http_request("GET", f"/api/v1/carts/{customer_id}")
    assert status == 200, f"Expected 200 on getCart, got {status}"
    latencies.append(lat)

    if worker_id % 2 == 0:
        status, lat, _ = http_request("POST", f"/api/v1/carts/{customer_id}/checkout")
        assert status == 200, f"Expected 200 on checkout, got {status}"
    else:
        status, lat, _ = http_request("DELETE", f"/api/v1/carts/{customer_id}")
        assert status == 204, f"Expected 204 on evict, got {status}"
    latencies.append(lat)

    return latencies


def percentile(sorted_values: list[float], pct: float) -> float:
    if not sorted_values:
        return 0.0
    idx = min(int(len(sorted_values) * pct / 100.0), len(sorted_values) - 1)
    return sorted_values[idx]


def run_concurrent_http_stress() -> dict:
    flows = CONCURRENT_HTTP_REQUESTS // 3
    print(f"\n[2/3] Running {flows * 3:,} concurrent HTTP API requests ({flows:,} full cart lifecycles across {CONCURRENCY_WORKERS} threads)...")
    all_latencies: list[float] = []
    start = time.perf_counter()

    with concurrent.futures.ThreadPoolExecutor(max_workers=CONCURRENCY_WORKERS) as pool:
        futures = [pool.submit(run_customer_flow, i) for i in range(flows)]
        for fut in concurrent.futures.as_completed(futures):
            all_latencies.extend(fut.result())

    total_time = time.perf_counter() - start
    all_latencies.sort()
    total_reqs = len(all_latencies)
    rps = total_reqs / total_time

    metrics = {
        "total_requests": total_reqs,
        "total_time_s": total_time,
        "rps": rps,
        "avg_ms": statistics.mean(all_latencies),
        "p50_ms": percentile(all_latencies, 50),
        "p95_ms": percentile(all_latencies, 95),
        "p99_ms": percentile(all_latencies, 99),
    }
    print(
        f"      Completed {total_reqs:,} HTTP requests in {total_time:.2f}s | "
        f"Throughput: {rps:,.1f} req/s | "
        f"p50: {metrics['p50_ms']:.2f}ms | p95: {metrics['p95_ms']:.2f}ms | p99: {metrics['p99_ms']:.2f}ms"
    )
    return metrics


def benchmark_statistics_aggregation() -> dict:
    print(f"\n[3/3] Benchmarking MongoDB Aggregation Pipeline (GET /api/v1/statistics/offers) over 100,000+ carts...")
    query_path = (
        "/api/v1/statistics/offers"
        "?offerId=tv-philips-55&action=ADD"
        "&from=2026-06-01T00:00:00Z&to=2026-12-31T23:59:59Z"
    )
    latencies = []
    sample_response = None
    for _ in range(30):
        status, lat, body = http_request("GET", query_path)
        assert status == 200, f"Expected 200 on statistics query, got {status}"
        latencies.append(lat)
        sample_response = body

    latencies.sort()
    stats_metrics = {
        "runs": len(latencies),
        "avg_ms": statistics.mean(latencies),
        "p50_ms": percentile(latencies, 50),
        "p95_ms": percentile(latencies, 95),
        "p99_ms": percentile(latencies, 99),
        "soldCount": sample_response["soldCount"],
        "activeCartCount": sample_response["activeCartCount"],
        "evictedCartCount": sample_response["evictedCartCount"],
        "totalAddedCount": sample_response["totalAddedCount"],
        "retargetingLeadsCount": len(sample_response["retargetingCustomerIds"]),
    }
    print(
        f"      30 Aggregation Queries over 100K+ carts | "
        f"p50: {stats_metrics['p50_ms']:.2f}ms | p95: {stats_metrics['p95_ms']:.2f}ms | p99: {stats_metrics['p99_ms']:.2f}ms"
    )
    print(
        f"      Aggregated Counts -> soldCount: {stats_metrics['soldCount']:,}, "
        f"activeCartCount: {stats_metrics['activeCartCount']:,}, "
        f"evictedCartCount: {stats_metrics['evictedCartCount']:,}, "
        f"totalAddedCount: {stats_metrics['totalAddedCount']:,}, "
        f"retargetingCustomerIds: {stats_metrics['retargetingLeadsCount']:,} customers"
    )
    return stats_metrics


if __name__ == "__main__":
    print("=== SHOPPING CART MICROSERVICE STRESS & LARGE DATASET BENCHMARK ===")
    seed_large_dataset_in_mongodb(TOTAL_SEED_CARTS)
    run_concurrent_http_stress()
    benchmark_statistics_aggregation()
    print("\n=== STRESS TEST COMPLETED SUCCESSFULLY (0 ERRORS) ===")

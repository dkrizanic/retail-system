## Deferred from: code review of epic-1-context.md (2026-11-16)

- Database unavailability handler (`503 Service Unavailable`) test coverage (`GlobalExceptionHandler.java:47-55`): verifying `DataAccessResourceFailureException` / `QueryTimeoutException` against the shared static Testcontainers MongoDB container requires stopping the container or adding a mock controller slice test.

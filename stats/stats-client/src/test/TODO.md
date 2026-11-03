# TODO: stats-client test scenarios

These scenarios were migrated from `main-service/src/test`. Implement tests later; no test code is included here.

- Delegate a hit to the Feign client with the same payload.
- Swallow a Feign exception raised while sending a hit.
- Format start and end timestamps and pass dates, URI filters, and the `unique` flag to the stats endpoint.
- Treat a `null` `unique` value as `false`.
- Convert a 4xx response into `IllegalArgumentException` and preserve the Feign exception as the cause.
- Convert a 5xx response into `IllegalStateException` and preserve the Feign exception as the cause.
- Convert a network failure into `IllegalStateException` and preserve the Feign exception as the cause.

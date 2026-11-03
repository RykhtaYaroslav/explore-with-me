# TODO: compilation-service test scenarios

These scenarios were migrated from `main-service/src/test`. Implement tests later; no test code is included here.

- When `pinned` is omitted, return all compilations using the requested page and map them to DTOs.
- When `pinned` is provided, return only compilations matching that value and map them to DTOs.
- Return a DTO when a compilation ID exists.
- Fail with `NotFoundException` and the expected message when a compilation ID does not exist.

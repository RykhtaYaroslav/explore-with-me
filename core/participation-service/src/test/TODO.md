# TODO: participation-service test scenarios

These scenarios were migrated from `main-service/src/test`. Implement tests later; no test code is included here.

- Create a request for a published event with no participant limit and moderation disabled; return a confirmed request DTO with the saved ID and correct requester/event IDs.
- Fail with `ConflictException` when the requester is also the event initiator.
- Fail with `ConflictException` when the event's participant limit has already been reached.
- Cancel an existing request owned by the requester and return it with `CANCELED` status.

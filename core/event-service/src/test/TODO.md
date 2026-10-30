# TODO: event-service test scenarios

These scenarios were migrated from `main-service/src/test`. Implement tests later; no test code is included here.

## Category service (3 scenarios)

- Fetch an existing category by ID and verify the returned ID and name.
- Fetch a missing category by ID and verify `NotFoundException` and its message.
- Fetch a paged category list and verify the returned DTOs and repository call.

## Create event (3 scenarios)

- Create an event from a populated request; verify its generated ID, copied fields, `PENDING` state, zero views and confirmed requests, initiator, participant limit, and category.
- Fail with `NotFoundException` when the initiator does not exist; do not save or call unrelated collaborators.
- Fail with `NotFoundException` when the selected category does not exist; do not save or call unrelated collaborators.

## Update event by initiator (11 scenarios)

- Update all request fields, category, requested state, views, and confirmed-request count.
- Treat an update request with every field `null` as a no-op while retaining state and statistics.
- Update only the title and preserve other fields.
- Update the category when a valid category ID is supplied.
- Set state to `PENDING` for `SEND_TO_REVIEW`.
- Allow updating an event whose old date is within two hours when a new valid date is supplied.
- Fail with `NotFoundException` when the user does not exist.
- Fail with `NotFoundException` when the event does not exist or is not owned by that user.
- Fail with `EventUpdateException` when trying to update a published event.
- Fail with `EventUpdateException` when the old event date is within two hours and no replacement date is supplied.
- Fail with `NotFoundException` when the requested category does not exist.

## Get requests for an initiator's event (4 scenarios)

- Return mapped requests for a valid initiator and event.
- Return an empty list and do not map requests when the event has no requests.
- Fail with `NotFoundException` when the user does not exist; do not query the event or requests.
- Fail with `NotFoundException` when the event does not exist for that initiator; do not query requests.

## Find an event by initiator and event IDs (4 scenarios)

- Return the full event DTO, including mapped event data, views, and confirmed-request count.
- Return the full event DTO with zero statistics.
- Fail with `NotFoundException` when the user does not exist; do not call downstream repositories or collaborators.
- Fail with `NotFoundException` when that user does not own the requested event; do not fetch stats or other data.

## Change request statuses for an event (9 scenarios)

- Confirm requested pending applications when the participant limit has room.
- When confirmation reaches the exact limit, reject all other pending applications for the event.
- When the batch exceeds the remaining capacity, confirm only the requests that fit and reject the rest.
- Reject the explicitly requested pending applications and return them in the rejected result list.
- Fail with `EventUpdateException` when the participant limit is zero.
- Fail with `EventUpdateException` when request moderation is disabled.
- Fail with `EventUpdateException` when the participant limit has already been reached.
- Fail with `NotFoundException` when one or more requested application IDs are missing.
- Fail with `EventUpdateException` when a requested application is not in `PENDING` state.

## Event statistics collector (4 active scenarios; 2 previously commented out)

- Query view counts for event URIs and map returned URI IDs to their hit counts.
- Load confirmed-request counts for event IDs and map them to their counts.
- Parse the numeric event ID from the end of a stats URI.
- Use the default minimum date ten years ago when an event has no `createdOn` date.
- [Previously commented out] Enrich short event DTOs with view and confirmed-request counts.
- [Previously commented out] Enrich full event DTOs with view and confirmed-request counts.

# TODO: user-service test scenarios

No user-service tests were present in `main-service/src/test`. Add tests for the current service behavior:

- Create a user and return the saved user DTO with an initial rating of zero.
- Reject creation with `ConflictException` when the email is already registered.
- List users with pagination when IDs are omitted.
- Return only users matching the supplied ID list.
- Include review-service average scores in user DTOs and use zero when no score is returned.
- Delete an existing user; fail with `NotFoundException` when the ID is missing.

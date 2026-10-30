# TODO: review-service test scenarios

No review-service tests were present in `main-service/src/test`. Add tests for the current review behavior:

- Create an event review for a confirmed participant after the configured post-event delay.
- Reject an event review before the delay, from a nonparticipant, or when the rater has already reviewed that event.
- Create an organizer review when the target is the event organizer and the rater is an eligible confirmed participant.
- Reject an organizer review when the target is not the organizer, the rater is not an eligible participant, the delay has not passed, or a duplicate review exists.
- Return `NotFoundException` when a referenced rater, organizer, or event does not exist.
- Return event reviews ordered by descending ID and resolve each rater.
- Return organizer reviews ordered by descending ID and resolve raters/events, reusing resolved related objects within one response.

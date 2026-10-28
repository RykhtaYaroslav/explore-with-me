package ru.practicum.core.event.dto.request;

public enum StateActionUser {
    SEND_TO_REVIEW, // отправить отменённое событие обратно на модерацию
    CANCEL_REVIEW // отменить модерацию события
}

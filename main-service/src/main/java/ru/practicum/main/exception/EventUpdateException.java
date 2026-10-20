package ru.practicum.main.exception;

import org.springframework.http.HttpStatus;

public class EventUpdateException extends BaseAppException {
    public EventUpdateException(String message) {
        super(message, HttpStatus.CONFLICT, "Ошибка при обновлении события");
    }
}

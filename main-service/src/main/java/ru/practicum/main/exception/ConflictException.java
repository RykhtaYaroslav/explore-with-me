package ru.practicum.main.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BaseAppException {
    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT, "Нарушена целостность данных или бизнес-правило");
    }
}

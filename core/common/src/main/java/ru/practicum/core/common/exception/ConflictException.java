package ru.practicum.core.common.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BaseAppException {
    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT, "A data integrity constraint or business rule was violated");
    }
}

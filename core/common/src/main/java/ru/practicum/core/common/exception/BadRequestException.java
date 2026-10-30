package ru.practicum.core.common.exception;

import org.springframework.http.HttpStatus;

// Class 'BadRequestException' is never used. Мб удалить его тогда?
public class BadRequestException extends BaseAppException {
    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "Invalid request");
    }
}

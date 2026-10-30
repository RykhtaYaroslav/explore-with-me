package ru.practicum.core.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BaseAppException {
    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "The requested resource was not found");
    }
}

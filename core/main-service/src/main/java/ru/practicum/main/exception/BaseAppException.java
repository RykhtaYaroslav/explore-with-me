package ru.practicum.main.exception;

import org.springframework.http.HttpStatus;

public class BaseAppException extends ru.practicum.core.common.exception.BaseAppException {
    public BaseAppException(String message, HttpStatus status, String reason) {
        super(message, status, reason);
    }
}

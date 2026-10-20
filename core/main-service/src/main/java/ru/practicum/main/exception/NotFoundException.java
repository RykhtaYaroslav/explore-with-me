package ru.practicum.main.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BaseAppException {
    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND, "Искомый объект не был найден");
    }
}
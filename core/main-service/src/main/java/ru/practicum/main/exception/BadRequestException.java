package ru.practicum.main.exception;

import org.springframework.http.HttpStatus;

// Class 'BadRequestException' is never used. Мб удалить его тогда?
public class BadRequestException extends BaseAppException {
    public BadRequestException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "Некорректный запрос");
    }
}

package ru.practicum.stats.error;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(BadRequestException e) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.name())
                .reason("Некорректный запрос")
                .message(e.getMessage())
                .timestamp(LocalDateTime.now())
                .errors(getErrorsList(e))
                .build();
        log.error("Получена ошибка 400 Bad Request: {}", e.getMessage(), e);
        return errorResponse;
    }

    private List<String> getErrorsList(Exception e) {
        List<String> errors = new ArrayList<>();
        errors.add(e.getMessage());
        for (StackTraceElement ste : e.getStackTrace()) {
            errors.add(ste.toString());
        }
        return errors;
    }
}

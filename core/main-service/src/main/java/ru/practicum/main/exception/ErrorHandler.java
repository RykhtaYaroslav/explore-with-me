package ru.practicum.main.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

import static java.time.LocalDateTime.now;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    /**
     * Unified handler for all custom application business exceptions.
     *
     * @param e the caught {@link BaseAppException}
     * @return a {@link ResponseEntity} containing the populated {@link ErrorResponse} and the corresponding HTTP status
     */
    @ExceptionHandler(BaseAppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(BaseAppException e) {
        log.error("Получена ошибка {}: {}", e.getStatus(), e.getMessage(), e);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(e.getStatus().name())
                .reason(e.getReason())
                .message(e.getMessage())
                .timestamp(now())
                .build();

        return new ResponseEntity<>(errorResponse, e.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> String.format("%s: %s", error.getField(), error.getDefaultMessage()))
                .collect(Collectors.joining("; "));

        return buildValidationResponse(message);
    }


    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> String.format("%s: %s", violation.getPropertyPath(), violation.getMessage()))
                .collect(Collectors.joining("; "));

        return buildValidationResponse(message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(org.springframework.web.bind.MissingServletRequestParameterException e) {
        String message = String.format("Required request parameter '%s' is not present", e.getParameterName());
        log.error("Получена ошибка 400 Bad Request (Missing Parameter): {}", message);

        return buildValidationResponse(message);
    }

    private ResponseEntity<ErrorResponse> buildValidationResponse(String message) {
        log.error("Получена ошибка 400 Bad Request (Validation): {}", message);

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.name())
                .reason("Ошибка валидации данных")
                .message(message)
                .timestamp(now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAnyException(Exception e) {
        // Собираем сообщение ошибки и хотя бы первую строчку стектрейса
        String rootCause = e.getMessage();
        if (e.getCause() != null) {
            rootCause += " -> " + e.getCause().getMessage();
        }

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.name())
                .reason(e.getClass().getName()) // Покажет точный класс ошибки (например, NullPointerException)
                .message(rootCause)            // Покажет детали
                .timestamp(now())
                .build();

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

package ru.practicum.main.util.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDateTime;

public class FutureAfterHoursValidator implements ConstraintValidator<FutureAfterHours, LocalDateTime> {
    private int hours;

    @Override
    public void initialize(FutureAfterHours constraintAnnotation) {
        this.hours = constraintAnnotation.hours();
    }

    @Override
    public boolean isValid(LocalDateTime time, ConstraintValidatorContext constraintValidatorContext) {
        if (time == null) {
            return true;
        }
        return time.isAfter(LocalDateTime.now().plusHours(hours));
    }
}

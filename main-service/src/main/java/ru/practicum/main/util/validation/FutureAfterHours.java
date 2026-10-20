package ru.practicum.main.util.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = FutureAfterHoursValidator.class)
// Класс содержащий логику
@Target({ElementType.FIELD, ElementType.PARAMETER})
// Можно ставить над полями и параметрами
@Retention(RetentionPolicy.RUNTIME) // Аннотация доступна во время выполнения
@SuppressWarnings("unused")
public @interface FutureAfterHours {
    String message() default "Event date must be within the specified future hours";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    int hours();
}

package com.antlia.movimanu.interfaces.rest.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = MovementValueValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMovementValue {
    String message() default "Informe um valor decimal diferente de zero, com até 16 dígitos inteiros e duas casas usando ponto.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

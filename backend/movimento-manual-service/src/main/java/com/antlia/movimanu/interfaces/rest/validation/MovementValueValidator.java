package com.antlia.movimanu.interfaces.rest.validation;

import java.math.BigDecimal;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MovementValueValidator implements ConstraintValidator<ValidMovementValue, String> {
    private static final Pattern FORMAT = Pattern.compile("-?[0-9]{1,16}(?:\\.[0-9]{1,2})?");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (!FORMAT.matcher(value).matches()) {
            return false;
        }
        return new BigDecimal(value).signum() != 0;
    }
}

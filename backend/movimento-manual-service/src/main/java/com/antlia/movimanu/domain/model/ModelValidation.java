package com.antlia.movimanu.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

final class ModelValidation {
    private static final int MAX_DESCRIPTION_LENGTH = 50;
    private static final long MAX_MOVEMENT_NUMBER = 999_999_999_999_999_999L;
    private static final BigDecimal MAX_MOVEMENT_VALUE = new BigDecimal("9999999999999999.99");

    private ModelValidation() {
    }

    static String requiredCode(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " é obrigatório.");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " deve conter até " + maxLength + " caracteres.");
        }
        return value;
    }

    static String optionalText(String value, int maxLength, String fieldName) {
        if (value != null && value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + " deve conter até " + maxLength + " caracteres.");
        }
        return value;
    }

    static String description(String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("A descrição é obrigatória.");
        }
        if (value.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException("A descrição deve conter até 50 caracteres.");
        }
        if (value.chars().allMatch(character -> character == ' ')) {
            throw new IllegalArgumentException("A descrição deve conter ao menos um caractere diferente de espaço.");
        }
        return value;
    }

    static int month(int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("O mês deve estar entre 1 e 12.");
        }
        return month;
    }

    static int year(int year) {
        if (year < 1 || year > 9999) {
            throw new IllegalArgumentException("O ano deve estar entre 1 e 9999.");
        }
        return year;
    }

    static Long movementNumber(Long movementNumber) {
        if (movementNumber == null || movementNumber < 1 || movementNumber > MAX_MOVEMENT_NUMBER) {
            throw new IllegalArgumentException("O número do lançamento deve estar entre 1 e 18 dígitos.");
        }
        return movementNumber;
    }

    static BigDecimal movementValue(BigDecimal movementValue) {
        if (movementValue == null) {
            throw new IllegalArgumentException("O valor do movimento é obrigatório.");
        }
        if (movementValue.scale() > 2) {
            throw new IllegalArgumentException("O valor do movimento deve conter até duas casas decimais.");
        }
        if (movementValue.signum() == 0) {
            throw new IllegalArgumentException("O valor do movimento não pode ser zero.");
        }
        if (movementValue.abs().compareTo(MAX_MOVEMENT_VALUE) > 0) {
            throw new IllegalArgumentException("O valor deve respeitar o limite NUMERIC(18,2).");
        }
        return movementValue.setScale(2, RoundingMode.UNNECESSARY);
    }

    static Instant movementAt(Instant movementAt) {
        if (movementAt == null) {
            throw new IllegalArgumentException("A data e hora do movimento são obrigatórias.");
        }
        return movementAt;
    }

    static String userCode(String userCode) {
        if (userCode == null || userCode.length() > 15) {
            throw new IllegalArgumentException("O código do usuário deve conter até 15 caracteres.");
        }
        return userCode;
    }
}

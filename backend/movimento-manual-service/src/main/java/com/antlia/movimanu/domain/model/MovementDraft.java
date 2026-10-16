package com.antlia.movimanu.domain.model;

import java.math.BigDecimal;

public record MovementDraft(
        int month,
        int year,
        String productCode,
        String cosifCode,
        String description,
        BigDecimal value) {
    public MovementDraft {
        month = ModelValidation.month(month);
        year = ModelValidation.year(year);
        productCode = ModelValidation.requiredCode(productCode, 4, "O código do produto");
        cosifCode = ModelValidation.requiredCode(cosifCode, 11, "O código COSIF");
        description = ModelValidation.description(description);
        value = ModelValidation.movementValue(value);
    }
}

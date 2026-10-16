package com.antlia.movimanu.domain.model;

public record ManualMovementId(
        int month,
        int year,
        Long launchNumber,
        String productCode,
        String cosifCode) {
    public ManualMovementId {
        month = ModelValidation.month(month);
        year = ModelValidation.year(year);
        launchNumber = ModelValidation.movementNumber(launchNumber);
        productCode = ModelValidation.requiredCode(productCode, 4, "O código do produto");
        cosifCode = ModelValidation.requiredCode(cosifCode, 11, "O código COSIF");
    }
}

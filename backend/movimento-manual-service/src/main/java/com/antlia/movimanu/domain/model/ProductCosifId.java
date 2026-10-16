package com.antlia.movimanu.domain.model;

public record ProductCosifId(String productCode, String cosifCode) {
    public ProductCosifId {
        productCode = ModelValidation.requiredCode(productCode, 4, "O código do produto");
        cosifCode = ModelValidation.requiredCode(cosifCode, 11, "O código COSIF");
    }
}

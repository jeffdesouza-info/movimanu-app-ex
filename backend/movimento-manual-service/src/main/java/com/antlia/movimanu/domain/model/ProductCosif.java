package com.antlia.movimanu.domain.model;

public record ProductCosif(ProductCosifId id, String classificationCode) {
    public ProductCosif {
        if (id == null) {
            throw new IllegalArgumentException("O identificador produto/COSIF é obrigatório.");
        }
        classificationCode = ModelValidation.optionalText(
                classificationCode,
                6,
                "A classificação COSIF");
    }
}

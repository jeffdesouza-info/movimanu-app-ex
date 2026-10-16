package com.antlia.movimanu.domain.model;

public record Product(String code, String description) {
    public Product {
        code = ModelValidation.requiredCode(code, 4, "O código do produto");
        description = ModelValidation.optionalText(description, 30, "A descrição do produto");
    }
}

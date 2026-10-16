package com.antlia.movimanu.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record ManualMovement(
        ManualMovementId id,
        String description,
        Instant movementAt,
        String userCode,
        BigDecimal value) {
    public ManualMovement {
        if (id == null) {
            throw new IllegalArgumentException("O identificador do movimento é obrigatório.");
        }
        description = ModelValidation.description(description);
        movementAt = ModelValidation.movementAt(movementAt);
        userCode = ModelValidation.userCode(userCode);
        value = ModelValidation.movementValue(value);
    }
}

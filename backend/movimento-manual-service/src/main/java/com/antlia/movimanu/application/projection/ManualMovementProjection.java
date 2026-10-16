package com.antlia.movimanu.application.projection;

import java.math.BigDecimal;
import java.time.Instant;

public record ManualMovementProjection(
        int month,
        int year,
        String productCode,
        String cosifCode,
        String description,
        BigDecimal value,
        String productDescription,
        Long launchNumber,
        Instant movementAt,
        String userCode) {
}

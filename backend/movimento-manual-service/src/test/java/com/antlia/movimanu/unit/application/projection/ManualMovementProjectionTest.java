package com.antlia.movimanu.unit.application.projection;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.antlia.movimanu.application.projection.ManualMovementProjection;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ManualMovementProjectionTest {
    @Test
    void exposesAllMovementQueryFieldsIncludingProductDescription() {
        var movementAt = Instant.parse("2026-01-02T03:04:00Z");
        var projection = new ManualMovementProjection(
                1,
                2026,
                "0007",
                "00000000001",
                "Ajuste manual",
                new BigDecimal("150.50"),
                "Produto",
                4L,
                movementAt,
                "TESTE");

        assertEquals(1, projection.month());
        assertEquals(2026, projection.year());
        assertEquals("0007", projection.productCode());
        assertEquals("00000000001", projection.cosifCode());
        assertEquals("Ajuste manual", projection.description());
        assertEquals(new BigDecimal("150.50"), projection.value());
        assertEquals("Produto", projection.productDescription());
        assertEquals(4L, projection.launchNumber());
        assertEquals(movementAt, projection.movementAt());
        assertEquals("TESTE", projection.userCode());
    }
}

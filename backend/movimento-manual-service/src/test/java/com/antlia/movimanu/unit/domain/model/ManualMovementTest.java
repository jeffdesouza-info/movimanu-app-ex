package com.antlia.movimanu.unit.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.antlia.movimanu.domain.model.ManualMovement;
import com.antlia.movimanu.domain.model.ManualMovementId;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ManualMovementTest {
    @Test
    void representsPersistedMovementWithDirectValueTypes() {
        var id = new ManualMovementId(1, 2026, 1L, "0007", "00000000001");
        var movementAt = Instant.parse("2026-01-02T03:04:00Z");
        var movement = new ManualMovement(id, "  Ajuste manual  ", movementAt, "TESTE", new BigDecimal("150.5"));

        assertEquals(id, movement.id());
        assertEquals("  Ajuste manual  ", movement.description());
        assertEquals(movementAt, movement.movementAt());
        assertEquals("TESTE", movement.userCode());
        assertEquals(new BigDecimal("150.50"), movement.value());
    }

    @Test
    void validatesRequiredFieldsAndMovementLimits() {
        var id = new ManualMovementId(1, 2026, 1L, "1", "1");
        var description = "Ajuste";
        var movementAt = Instant.parse("2026-01-02T03:04:00Z");
        var value = new BigDecimal("1.00");

        assertThrows(IllegalArgumentException.class,
                () -> new ManualMovement(null, description, movementAt, "TESTE", value));
        assertThrows(IllegalArgumentException.class,
                () -> new ManualMovement(id, null, movementAt, "TESTE", value));
        assertThrows(IllegalArgumentException.class,
                () -> new ManualMovement(id, description, null, "TESTE", value));
        assertThrows(IllegalArgumentException.class,
                () -> new ManualMovement(id, description, movementAt, null, value));
        assertThrows(IllegalArgumentException.class,
                () -> new ManualMovement(id, description, movementAt, "1".repeat(16), value));
        assertThrows(IllegalArgumentException.class,
                () -> new ManualMovement(id, description, movementAt, "TESTE", new BigDecimal("1.000")));
    }

    @Test
    void appliesDescriptionAndAmountRulesToPersistedMovements() {
        var id = new ManualMovementId(1, 2026, 1L, "1", "1");
        var movementAt = Instant.parse("2026-01-02T03:04:00Z");

        assertThrows(IllegalArgumentException.class,
                () -> new ManualMovement(id, "   ", movementAt, "TESTE", new BigDecimal("1.00")));
        assertThrows(IllegalArgumentException.class,
                () -> new ManualMovement(id, "a".repeat(51), movementAt, "TESTE", new BigDecimal("1.00")));
        assertThrows(IllegalArgumentException.class,
                () -> new ManualMovement(id, "Ajuste", movementAt, "TESTE", BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new ManualMovement(id, "Ajuste", movementAt, "TESTE",
                        new BigDecimal("10000000000000000.00")));
    }
}

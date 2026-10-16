package com.antlia.movimanu.unit.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.antlia.movimanu.domain.model.ManualMovementId;
import org.junit.jupiter.api.Test;

class ManualMovementIdTest {
    @Test
    void containsDirectCompositeKeyFieldsAndPreservesCodes() {
        var id = new ManualMovementId(1, 9999, 1L, "0007", "00000000001");

        assertEquals(1, id.month());
        assertEquals(9999, id.year());
        assertEquals(1L, id.launchNumber());
        assertEquals("0007", id.productCode());
        assertEquals("00000000001", id.cosifCode());
        assertEquals(999_999_999_999_999_999L,
                new ManualMovementId(12, 1, 999_999_999_999_999_999L, "1", "1").launchNumber());
    }

    @Test
    void rejectsMonthYearLaunchAndCodeValuesOutsideTheirLimits() {
        assertThrows(IllegalArgumentException.class, () -> new ManualMovementId(0, 2026, 1L, "1", "1"));
        assertThrows(IllegalArgumentException.class, () -> new ManualMovementId(13, 2026, 1L, "1", "1"));
        assertThrows(IllegalArgumentException.class, () -> new ManualMovementId(1, 0, 1L, "1", "1"));
        assertThrows(IllegalArgumentException.class, () -> new ManualMovementId(1, 10000, 1L, "1", "1"));
        assertThrows(IllegalArgumentException.class, () -> new ManualMovementId(1, 2026, null, "1", "1"));
        assertThrows(IllegalArgumentException.class, () -> new ManualMovementId(1, 2026, 0L, "1", "1"));
        assertThrows(IllegalArgumentException.class, () -> new ManualMovementId(1, 2026, -1L, "1", "1"));
        assertThrows(IllegalArgumentException.class,
                () -> new ManualMovementId(1, 2026, 1_000_000_000_000_000_000L, "1", "1"));
        assertThrows(IllegalArgumentException.class, () -> new ManualMovementId(1, 2026, 1L, "12345", "1"));
        assertThrows(IllegalArgumentException.class, () -> new ManualMovementId(1, 2026, 1L, "1", "1".repeat(12)));
    }
}

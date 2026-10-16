package com.antlia.movimanu.unit.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.antlia.movimanu.domain.model.MovementDraft;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MovementDraftTest {
    @Test
    void holdsSixDirectFieldsAndPreservesDescriptionAndLeadingZeros() {
        var draft = new MovementDraft(1, 9999, "0007", "00000000001", "  Ajuste manual  ", new BigDecimal("-150.00"));

        assertEquals(1, draft.month());
        assertEquals(9999, draft.year());
        assertEquals("0007", draft.productCode());
        assertEquals("00000000001", draft.cosifCode());
        assertEquals("  Ajuste manual  ", draft.description());
        assertEquals(new BigDecimal("-150.00"), draft.value());
    }

    @Test
    void validatesMonthAndYearBoundaries() {
        assertEquals(12, validDraft(12, 1, "1", "1", "Ajuste", "1").month());
        assertThrows(IllegalArgumentException.class, () -> validDraft(0, 2026, "1", "1", "A", "1"));
        assertThrows(IllegalArgumentException.class, () -> validDraft(13, 2026, "1", "1", "A", "1"));
        assertThrows(IllegalArgumentException.class, () -> validDraft(1, 0, "1", "1", "A", "1"));
        assertThrows(IllegalArgumentException.class, () -> validDraft(1, 10000, "1", "1", "A", "1"));
    }

    @Test
    void validatesRequiredProductAndCosifCodesAndTheirMaximumLengths() {
        assertThrows(IllegalArgumentException.class, () -> validDraft(1, 2026, null, "1", "A", "1"));
        assertThrows(IllegalArgumentException.class, () -> validDraft(1, 2026, " ", "1", "A", "1"));
        assertThrows(IllegalArgumentException.class, () -> validDraft(1, 2026, "12345", "1", "A", "1"));
        assertThrows(IllegalArgumentException.class, () -> validDraft(1, 2026, "1", null, "A", "1"));
        assertThrows(IllegalArgumentException.class, () -> validDraft(1, 2026, "1", " ", "A", "1"));
        assertThrows(IllegalArgumentException.class, () -> validDraft(1, 2026, "1", "1".repeat(12), "A", "1"));
    }

    @Test
    void validatesDescriptionAndPreservesSpacesAtTheFiftyCharacterLimit() {
        String description = " " + "a".repeat(48) + " ";
        assertEquals(description, validDraft(1, 2026, "1", "1", description, "1").description());

        assertThrows(IllegalArgumentException.class, () -> validDraft(1, 2026, "1", "1", null, "1"));
        assertThrows(IllegalArgumentException.class, () -> validDraft(1, 2026, "1", "1", "", "1"));
        assertThrows(IllegalArgumentException.class, () -> validDraft(1, 2026, "1", "1", "   ", "1"));
        assertThrows(IllegalArgumentException.class,
                () -> validDraft(1, 2026, "1", "1", "a".repeat(51), "1"));
    }

    @Test
    void normalizesExactMonetaryValuesToTwoPlacesAfterCheckingOriginalScale() {
        assertEquals(new BigDecimal("150.00"), validDraft(1, 2026, "1", "1", "A", "150").value());
        assertEquals(new BigDecimal("150.50"), validDraft(1, 2026, "1", "1", "A", "150.5").value());
        assertEquals(new BigDecimal("-150.00"), validDraft(1, 2026, "1", "1", "A", "-150.00").value());
        assertEquals(new BigDecimal("9999999999999999.99"),
                validDraft(1, 2026, "1", "1", "A", "9999999999999999.99").value());
    }

    @Test
    void rejectsNullZeroExcessScaleAndValuesBeyondNumericEighteenTwo() {
        assertThrows(IllegalArgumentException.class, () -> validDraft(1, 2026, "1", "1", "A", null));
        assertThrows(IllegalArgumentException.class, () -> validDraft(1, 2026, "1", "1", "A", "0.00"));
        assertThrows(IllegalArgumentException.class, () -> validDraft(1, 2026, "1", "1", "A", "-0.00"));
        assertThrows(IllegalArgumentException.class, () -> validDraft(1, 2026, "1", "1", "A", "1.000"));
        assertThrows(IllegalArgumentException.class,
                () -> validDraft(1, 2026, "1", "1", "A", "10000000000000000.00"));
    }

    private MovementDraft validDraft(int month, int year, String productCode, String cosifCode,
                                     String description, String value) {
        return new MovementDraft(
                month,
                year,
                productCode,
                cosifCode,
                description,
                value == null ? null : new BigDecimal(value));
    }
}

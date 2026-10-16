package com.antlia.movimanu.unit.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.antlia.movimanu.domain.model.ProductCosifId;
import org.junit.jupiter.api.Test;

class ProductCosifIdTest {
    @Test
    void preservesDirectCodesIncludingLeadingZerosAndAllowsShortCodes() {
        var id = new ProductCosifId("07", "000123");

        assertEquals("07", id.productCode());
        assertEquals("000123", id.cosifCode());
        assertEquals("0001", new ProductCosifId("0001", "1").productCode());
    }

    @Test
    void rejectsMissingBlankOrOverlongCodes() {
        assertThrows(IllegalArgumentException.class, () -> new ProductCosifId(null, "1"));
        assertThrows(IllegalArgumentException.class, () -> new ProductCosifId("1", null));
        assertThrows(IllegalArgumentException.class, () -> new ProductCosifId(" ", "1"));
        assertThrows(IllegalArgumentException.class, () -> new ProductCosifId("12345", "1"));
        assertThrows(IllegalArgumentException.class, () -> new ProductCosifId("1", "1".repeat(12)));
    }
}

package com.antlia.movimanu.unit.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.antlia.movimanu.domain.model.ProductCosif;
import com.antlia.movimanu.domain.model.ProductCosifId;
import org.junit.jupiter.api.Test;

class ProductCosifTest {
    @Test
    void keepsTheCompositeIdAndOptionalClassification() {
        var id = new ProductCosifId("0001", "00000000001");
        var cosif = new ProductCosif(id, "123456");

        assertEquals(id, cosif.id());
        assertEquals("123456", cosif.classificationCode());
        assertNull(new ProductCosif(id, null).classificationCode());
    }

    @Test
    void rejectsMissingIdAndClassificationOverSixCharacters() {
        assertThrows(IllegalArgumentException.class, () -> new ProductCosif(null, null));
        assertThrows(IllegalArgumentException.class, () -> new ProductCosif(
                new ProductCosifId("1", "1"),
                "1234567"));
    }
}

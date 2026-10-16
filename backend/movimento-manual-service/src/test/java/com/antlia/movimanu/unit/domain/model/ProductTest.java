package com.antlia.movimanu.unit.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.antlia.movimanu.domain.model.Product;
import org.junit.jupiter.api.Test;

class ProductTest {
    @Test
    void acceptsProductCodesUpToFourCharactersAndOptionalDescription() {
        assertEquals("7", new Product("7", null).code());
        assertEquals("0007", new Product("0007", "Produto").code());
        assertEquals("Produto", new Product("0007", "Produto").description());
        assertNull(new Product("1", null).description());
        assertEquals("a".repeat(30), new Product("1", "a".repeat(30)).description());
    }

    @Test
    void rejectsMissingBlankOrOverlongCodeAndOverlongDescription() {
        assertThrows(IllegalArgumentException.class, () -> new Product(null, null));
        assertThrows(IllegalArgumentException.class, () -> new Product("  ", null));
        assertThrows(IllegalArgumentException.class, () -> new Product("12345", null));
        assertThrows(IllegalArgumentException.class, () -> new Product("1", "a".repeat(31)));
    }
}

package com.antlia.movimanu.unit.application.port;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.antlia.movimanu.application.port.MovementPage;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MovementPageTest {
    @Test
    void keepsRequestedPageFixedSizeAndRealTotalsIncludingEmptyOutOfRangePages() {
        var firstPage = new MovementPage<>(List.of("a", "b", "c", "d", "e", "f"), 0, 7);
        var outOfRangePage = new MovementPage<>(List.of(), 2, 7);
        var emptyPage = new MovementPage<>(List.of(), 0, 0);

        assertEquals(6, firstPage.size());
        assertEquals(2, firstPage.totalPages());
        assertEquals(2, outOfRangePage.page());
        assertEquals(7, outOfRangePage.totalElements());
        assertEquals(2, outOfRangePage.totalPages());
        assertEquals(0, emptyPage.totalPages());
    }

    @Test
    void returnsAnImmutableCopyOfContent() {
        var source = new ArrayList<>(List.of("movement"));
        var page = new MovementPage<>(source, 0, 1);
        source.clear();

        assertEquals(List.of("movement"), page.content());
        assertThrows(UnsupportedOperationException.class, () -> page.content().add("other"));
    }

    @Test
    void rejectsInvalidPageContentAndTotals() {
        assertThrows(IllegalArgumentException.class, () -> new MovementPage<>(null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new MovementPage<>(List.of(), -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new MovementPage<>(List.of("a"), 0, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MovementPage<>(List.of("a", "b", "c", "d", "e", "f", "g"), 0, 7));
    }
}

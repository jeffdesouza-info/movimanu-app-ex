package com.antlia.movimanu.unit.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.antlia.movimanu.application.port.MovementPage;
import com.antlia.movimanu.application.projection.ManualMovementProjection;
import com.antlia.movimanu.infrastructure.persistence.adapter.JpaManualMovementQueryAdapter;
import com.antlia.movimanu.infrastructure.persistence.repository.ManualMovementJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaManualMovementQueryAdapterTest {
    private final ManualMovementJpaRepository repository = mock(ManualMovementJpaRepository.class);
    private final JpaManualMovementQueryAdapter adapter = new JpaManualMovementQueryAdapter(repository);

    @Test
    void mapsTheRequestedPageAndKeepsTheFixedSizeAndTotal() {
        ManualMovementProjection movement = new ManualMovementProjection(
                1,
                2026,
                "0001",
                "10000000001",
                "Ajuste",
                new BigDecimal("150.00"),
                "Produto A",
                1L,
                Instant.parse("2026-01-05T12:00:00Z"),
                "TESTE");
        when(repository.count()).thenReturn(7L);
        when(repository.findProjectedPage(PageRequest.of(0, MovementPage.SIZE)))
                .thenReturn(new PageImpl<>(List.of(movement), PageRequest.of(0, MovementPage.SIZE), 7));

        MovementPage<ManualMovementProjection> result = adapter.findPage(0);

        assertThat(result.content()).containsExactly(movement);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(6);
        assertThat(result.totalElements()).isEqualTo(7);
        assertThat(result.totalPages()).isEqualTo(2);
        verify(repository).findProjectedPage(PageRequest.of(0, 6));
    }

    @Test
    void preservesEmptyOutOfRangePagesAndTheActualTotal() {
        when(repository.count()).thenReturn(7L);

        MovementPage<ManualMovementProjection> result = adapter.findPage(2);

        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(7);
        assertThat(result.totalPages()).isEqualTo(2);
        verify(repository, never()).findProjectedPage(PageRequest.of(2, MovementPage.SIZE));
    }

    @ParameterizedTest
    @ValueSource(ints = {357913942, Integer.MAX_VALUE})
    void returnsRealTotalsForOffsetsBeyondIntegerMaxWithoutQueryingThePage(int page) {
        when(repository.count()).thenReturn(7L);

        MovementPage<ManualMovementProjection> result = adapter.findPage(page);

        assertThat((long) page * MovementPage.SIZE).isGreaterThan(Integer.MAX_VALUE);
        assertThat(result.content()).isEmpty();
        assertThat(result.page()).isEqualTo(page);
        assertThat(result.size()).isEqualTo(6);
        assertThat(result.totalElements()).isEqualTo(7);
        assertThat(result.totalPages()).isEqualTo(2);
        verify(repository, never()).findProjectedPage(PageRequest.of(page, MovementPage.SIZE));
    }

    @Test
    void delegatesHighestNumberQueriesIncludingEmptyPeriods() {
        when(repository.findHighestNumberFor(12, 2025)).thenReturn(Optional.of(4L));
        when(repository.findHighestNumberFor(1, 2026)).thenReturn(Optional.empty());

        assertThat(adapter.findHighestNumberFor(12, 2025)).contains(4L);
        assertThat(adapter.findHighestNumberFor(1, 2026)).isEmpty();
    }
}

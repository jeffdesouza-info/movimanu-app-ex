package com.antlia.movimanu.unit.application.usecase;

import java.util.List;

import com.antlia.movimanu.application.port.ManualMovementQueryPort;
import com.antlia.movimanu.application.port.MovementPage;
import com.antlia.movimanu.application.projection.ManualMovementProjection;
import com.antlia.movimanu.application.usecase.FindManualMovementsUseCase;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FindManualMovementsUseCaseTest {
    private final ManualMovementQueryPort manualMovementQueryPort = mock(ManualMovementQueryPort.class);
    private final FindManualMovementsUseCase useCase = new FindManualMovementsUseCase(manualMovementQueryPort);

    @Test
    void returnsTheRequestedPage() {
        MovementPage<ManualMovementProjection> result = new MovementPage<>(List.of(), 1, 7);
        when(manualMovementQueryPort.findPage(1)).thenReturn(result);

        assertThat(useCase.execute(1)).isSameAs(result);
        verify(manualMovementQueryPort).findPage(1);
    }

    @Test
    void returnsAnEmptyFirstPageWhenThereAreNoMovements() {
        MovementPage<ManualMovementProjection> result = new MovementPage<>(List.of(), 0, 0);
        when(manualMovementQueryPort.findPage(0)).thenReturn(result);

        assertThat(useCase.execute(0)).isEqualTo(result);
    }

    @Test
    void rejectsNegativePagesBeforeCallingThePort() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> useCase.execute(-1))
                .withMessage("A página não pode ser negativa.");
        verify(manualMovementQueryPort, never()).findPage(-1);
    }

    @Test
    void requiresAMovementQueryPort() {
        assertThatNullPointerException()
                .isThrownBy(() -> new FindManualMovementsUseCase(null))
                .withMessage("A consulta de movimentos é obrigatória.");
    }
}

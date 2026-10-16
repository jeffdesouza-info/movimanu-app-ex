package com.antlia.movimanu.unit.application.usecase;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.function.Supplier;

import com.antlia.movimanu.application.exception.ManualMovementSequenceCollisionException;
import com.antlia.movimanu.application.exception.ManualMovementSequenceConflictException;
import com.antlia.movimanu.application.exception.ProductCosifNotFoundException;
import com.antlia.movimanu.application.port.ManualMovementInclusionPort;
import com.antlia.movimanu.application.port.ManualMovementQueryPort;
import com.antlia.movimanu.application.port.ProductCosifQueryPort;
import com.antlia.movimanu.application.port.TransactionExecutionPort;
import com.antlia.movimanu.application.usecase.IncludeManualMovementUseCase;
import com.antlia.movimanu.domain.model.ManualMovement;
import com.antlia.movimanu.domain.model.ManualMovementId;
import com.antlia.movimanu.domain.model.MovementDraft;
import com.antlia.movimanu.domain.model.ProductCosifId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IncludeManualMovementUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-10-16T12:34:56Z");
    private static final MovementDraft DRAFT = new MovementDraft(
            10, 2026, "0001", "10000000001", "Ajuste manual", new BigDecimal("150.00"));

    private final ManualMovementInclusionPort inclusionPort = mock(ManualMovementInclusionPort.class);
    private final ManualMovementQueryPort movementQueryPort = mock(ManualMovementQueryPort.class);
    private final ProductCosifQueryPort productCosifQueryPort = mock(ProductCosifQueryPort.class);
    private final TransactionExecutionPort transactionExecutionPort = new TransactionExecutionPort() {
        @Override
        public <T> T executeInNewTransaction(Supplier<T> operation) {
            return operation.get();
        }
    };
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final IncludeManualMovementUseCase useCase = new IncludeManualMovementUseCase(
            inclusionPort, movementQueryPort, productCosifQueryPort, transactionExecutionPort, clock);

    IncludeManualMovementUseCaseTest() {
        when(productCosifQueryPort.existsById(new ProductCosifId("0001", "10000000001"))).thenReturn(true);
        when(movementQueryPort.findHighestNumberFor(10, 2026)).thenReturn(Optional.empty());
    }

    @Test
    void assignsTheFirstNumberAndPersistsWithTheUtcInstantAndFixedUser() {
        ManualMovement movement = movement(1L);
        when(inclusionPort.insert(movement)).thenReturn(movement);

        assertThat(useCase.execute(DRAFT)).isEqualTo(movement);

        verify(inclusionPort).insert(movement);
    }

    @Test
    void continuesAfterTheHighestNumberAndPersistsTheNextNumber() {
        ManualMovement movement = movement(4L);
        when(movementQueryPort.findHighestNumberFor(10, 2026)).thenReturn(Optional.of(3L));
        when(inclusionPort.insert(movement)).thenReturn(movement);

        assertThat(useCase.execute(DRAFT)).isEqualTo(movement);
        verify(inclusionPort).insert(movement);
    }

    @Test
    void rejectsAnUnlinkedProductCosifPairBeforeReadingOrPersisting() {
        when(productCosifQueryPort.existsById(new ProductCosifId("0001", "10000000001"))).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(DRAFT)).isInstanceOf(ProductCosifNotFoundException.class);

        verify(movementQueryPort, never()).findHighestNumberFor(10, 2026);
        verify(inclusionPort, never()).insert(any());
    }

    @Test
    void rejectsAnIncrementBeyondTheEighteenDigitLimit() {
        when(movementQueryPort.findHighestNumberFor(10, 2026))
                .thenReturn(Optional.of(999_999_999_999_999_999L));

        assertThatThrownBy(() -> useCase.execute(DRAFT)).isInstanceOf(ManualMovementSequenceConflictException.class);

        verify(inclusionPort, never()).insert(any());
    }

    @Test
    void retriesOnlyASequenceCollisionAndReturnsTheSuccessfulAttempt() {
        ManualMovement movement = movement(1L);
        when(inclusionPort.insert(movement))
                .thenThrow(new ManualMovementSequenceCollisionException(new RuntimeException("colisão")))
                .thenReturn(movement);

        assertThat(useCase.execute(DRAFT)).isEqualTo(movement);

        verify(inclusionPort, org.mockito.Mockito.times(2)).insert(movement);
    }

    @Test
    void returnsConflictAfterThreeSequenceCollisions() {
        when(inclusionPort.insert(movement(1L)))
                .thenThrow(new ManualMovementSequenceCollisionException(new RuntimeException("colisão")));

        assertThatThrownBy(() -> useCase.execute(DRAFT))
                .isInstanceOf(ManualMovementSequenceConflictException.class)
                .hasCauseInstanceOf(ManualMovementSequenceCollisionException.class);

        verify(inclusionPort, org.mockito.Mockito.times(3)).insert(movement(1L));
    }

    @Test
    void doesNotRetryOtherFailures() {
        IllegalStateException failure = new IllegalStateException("falha de persistência");
        when(inclusionPort.insert(movement(1L))).thenThrow(failure);

        assertThatThrownBy(() -> useCase.execute(DRAFT)).isSameAs(failure);

        verify(inclusionPort).insert(movement(1L));
    }

    @Test
    void requiresItsPortsClockAndDraft() {
        assertThatNullPointerException()
                .isThrownBy(() -> new IncludeManualMovementUseCase(
                        null, movementQueryPort, productCosifQueryPort, transactionExecutionPort, clock))
                .withMessage("A porta de inclusão é obrigatória.");
        assertThatNullPointerException()
                .isThrownBy(() -> new IncludeManualMovementUseCase(
                        inclusionPort, null, productCosifQueryPort, transactionExecutionPort, clock))
                .withMessage("A consulta de movimentos é obrigatória.");
        assertThatNullPointerException()
                .isThrownBy(() -> new IncludeManualMovementUseCase(
                        inclusionPort, movementQueryPort, null, transactionExecutionPort, clock))
                .withMessage("A consulta de COSIFs é obrigatória.");
        assertThatNullPointerException()
                .isThrownBy(() -> new IncludeManualMovementUseCase(
                        inclusionPort, movementQueryPort, productCosifQueryPort, null, clock))
                .withMessage("A fronteira transacional é obrigatória.");
        assertThatNullPointerException()
                .isThrownBy(() -> new IncludeManualMovementUseCase(
                        inclusionPort, movementQueryPort, productCosifQueryPort, transactionExecutionPort, null))
                .withMessage("O relógio é obrigatório.");
        assertThatNullPointerException()
                .isThrownBy(() -> useCase.execute(null))
                .withMessage("Os dados do movimento são obrigatórios.");
    }

    private ManualMovement movement(long launchNumber) {
        return new ManualMovement(
                new ManualMovementId(10, 2026, launchNumber, "0001", "10000000001"),
                "Ajuste manual",
                NOW,
                "TESTE",
                new BigDecimal("150.00"));
    }
}

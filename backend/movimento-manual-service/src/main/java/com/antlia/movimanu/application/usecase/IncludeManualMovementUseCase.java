package com.antlia.movimanu.application.usecase;

import java.time.Clock;
import java.util.Objects;

import com.antlia.movimanu.application.exception.ManualMovementSequenceCollisionException;
import com.antlia.movimanu.application.exception.ManualMovementSequenceConflictException;
import com.antlia.movimanu.application.exception.ProductCosifNotFoundException;
import com.antlia.movimanu.application.port.ManualMovementInclusionPort;
import com.antlia.movimanu.application.port.ManualMovementQueryPort;
import com.antlia.movimanu.application.port.ProductCosifQueryPort;
import com.antlia.movimanu.application.port.TransactionExecutionPort;
import com.antlia.movimanu.domain.model.ManualMovement;
import com.antlia.movimanu.domain.model.ManualMovementId;
import com.antlia.movimanu.domain.model.MovementDraft;
import com.antlia.movimanu.domain.model.ProductCosifId;

public class IncludeManualMovementUseCase {
    private static final int MAX_ATTEMPTS = 3;
    private static final long MAX_MOVEMENT_NUMBER = 999_999_999_999_999_999L;
    private static final String USER_CODE = "TESTE";

    private final ManualMovementInclusionPort inclusionPort;
    private final ManualMovementQueryPort movementQueryPort;
    private final ProductCosifQueryPort productCosifQueryPort;
    private final TransactionExecutionPort transactionExecutionPort;
    private final Clock clock;

    public IncludeManualMovementUseCase(
            ManualMovementInclusionPort inclusionPort,
            ManualMovementQueryPort movementQueryPort,
            ProductCosifQueryPort productCosifQueryPort,
            TransactionExecutionPort transactionExecutionPort,
            Clock clock) {
        this.inclusionPort = Objects.requireNonNull(inclusionPort, "A porta de inclusão é obrigatória.");
        this.movementQueryPort = Objects.requireNonNull(movementQueryPort, "A consulta de movimentos é obrigatória.");
        this.productCosifQueryPort = Objects.requireNonNull(productCosifQueryPort, "A consulta de COSIFs é obrigatória.");
        this.transactionExecutionPort = Objects.requireNonNull(
                transactionExecutionPort,
                "A fronteira transacional é obrigatória.");
        this.clock = Objects.requireNonNull(clock, "O relógio é obrigatório.");
    }

    public ManualMovement execute(MovementDraft draft) {
        Objects.requireNonNull(draft, "Os dados do movimento são obrigatórios.");
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return transactionExecutionPort.executeInNewTransaction(() -> includeOnce(draft));
            } catch (ManualMovementSequenceCollisionException collision) {
                if (attempt >= MAX_ATTEMPTS) {
                    throw new ManualMovementSequenceConflictException(collision);
                }
            }
        }
    }

    private ManualMovement includeOnce(MovementDraft draft) {
        ProductCosifId productCosifId = new ProductCosifId(draft.productCode(), draft.cosifCode());
        if (!productCosifQueryPort.existsById(productCosifId)) {
            throw new ProductCosifNotFoundException(draft.productCode(), draft.cosifCode());
        }

        long highestNumber = movementQueryPort.findHighestNumberFor(draft.month(), draft.year()).orElse(0L);
        if (highestNumber >= MAX_MOVEMENT_NUMBER) {
            throw new ManualMovementSequenceConflictException();
        }

        ManualMovement movement = new ManualMovement(
                new ManualMovementId(
                        draft.month(),
                        draft.year(),
                        highestNumber + 1,
                        draft.productCode(),
                        draft.cosifCode()),
                draft.description(),
                clock.instant(),
                USER_CODE,
                draft.value());
        return inclusionPort.insert(movement);
    }
}

package com.antlia.movimanu.application.usecase;

import java.util.Objects;

import com.antlia.movimanu.application.port.ManualMovementQueryPort;
import com.antlia.movimanu.application.port.MovementPage;
import com.antlia.movimanu.application.projection.ManualMovementProjection;

public class FindManualMovementsUseCase {
    private final ManualMovementQueryPort manualMovementQueryPort;

    public FindManualMovementsUseCase(ManualMovementQueryPort manualMovementQueryPort) {
        this.manualMovementQueryPort = Objects.requireNonNull(
                manualMovementQueryPort,
                "A consulta de movimentos é obrigatória.");
    }

    public MovementPage<ManualMovementProjection> execute(int page) {
        if (page < 0) {
            throw new IllegalArgumentException("A página não pode ser negativa.");
        }
        return manualMovementQueryPort.findPage(page);
    }
}

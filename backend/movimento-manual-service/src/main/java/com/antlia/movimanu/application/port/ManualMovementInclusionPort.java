package com.antlia.movimanu.application.port;

import com.antlia.movimanu.domain.model.ManualMovement;

public interface ManualMovementInclusionPort {
    ManualMovement insert(ManualMovement movement);
}

package com.antlia.movimanu.application.port;

import com.antlia.movimanu.application.projection.ManualMovementProjection;
import java.util.Optional;

public interface ManualMovementQueryPort {
    MovementPage<ManualMovementProjection> findPage(int page);

    Optional<Long> findHighestNumberFor(int month, int year);
}

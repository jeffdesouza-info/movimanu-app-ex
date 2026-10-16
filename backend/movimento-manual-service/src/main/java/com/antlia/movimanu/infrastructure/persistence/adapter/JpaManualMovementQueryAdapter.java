package com.antlia.movimanu.infrastructure.persistence.adapter;

import java.util.List;

import com.antlia.movimanu.application.port.ManualMovementQueryPort;
import com.antlia.movimanu.application.port.MovementPage;
import com.antlia.movimanu.application.projection.ManualMovementProjection;
import com.antlia.movimanu.infrastructure.persistence.mapper.ManualMovementMapper;
import com.antlia.movimanu.infrastructure.persistence.repository.ManualMovementJpaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaManualMovementQueryAdapter implements ManualMovementQueryPort {
    private final ManualMovementJpaRepository repository;
    private final ManualMovementMapper mapper;

    public JpaManualMovementQueryAdapter(ManualMovementJpaRepository repository, ManualMovementMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public MovementPage<ManualMovementProjection> findPage(int page) {
        long offset = (long) page * MovementPage.SIZE;
        long totalElements = repository.count();
        if (offset >= totalElements) {
            return new MovementPage<>(List.of(), page, totalElements);
        }

        var result = repository.findMovementPage(PageRequest.of(page, MovementPage.SIZE));
        return new MovementPage<>(result.getContent().stream().map(mapper::toProjection).toList(), page, totalElements);
    }

    @Override
    public java.util.Optional<Long> findHighestNumberFor(int month, int year) {
        return repository.findHighestNumberFor(month, year);
    }
}

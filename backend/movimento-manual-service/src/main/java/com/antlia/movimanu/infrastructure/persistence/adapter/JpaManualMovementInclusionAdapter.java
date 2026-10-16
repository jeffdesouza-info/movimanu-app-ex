package com.antlia.movimanu.infrastructure.persistence.adapter;

import java.util.Locale;

import com.antlia.movimanu.application.exception.ManualMovementSequenceCollisionException;
import com.antlia.movimanu.application.exception.ProductCosifNotFoundException;
import com.antlia.movimanu.application.port.ManualMovementInclusionPort;
import com.antlia.movimanu.domain.model.ManualMovement;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntity;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntityId;
import com.antlia.movimanu.infrastructure.persistence.mapper.ManualMovementMapper;
import com.antlia.movimanu.infrastructure.persistence.repository.ProductCosifJpaRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaManualMovementInclusionAdapter implements ManualMovementInclusionPort {
    private static final String SEQUENCE_CONSTRAINT = "UK_MOVIMENTO_MANUAL_PERIODO_LANCAMENTO";

    private final EntityManager entityManager;
    private final ProductCosifJpaRepository productCosifRepository;
    private final ManualMovementMapper mapper;

    public JpaManualMovementInclusionAdapter(
            EntityManager entityManager,
            ProductCosifJpaRepository productCosifRepository,
            ManualMovementMapper mapper) {
        this.entityManager = entityManager;
        this.productCosifRepository = productCosifRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public ManualMovement insert(ManualMovement movement) {
        var id = movement.id();
        ProductCosifEntity productCosif = productCosifRepository.findById(
                        new ProductCosifEntityId(id.productCode(), id.cosifCode()))
                .orElseThrow(() -> new ProductCosifNotFoundException(id.productCode(), id.cosifCode()));

        try {
            var entity = mapper.toEntity(movement, productCosif);
            entityManager.persist(entity);
            entityManager.flush();
            return mapper.toDomain(entity);
        } catch (RuntimeException exception) {
            if (isSequenceCollision(exception)) {
                throw new ManualMovementSequenceCollisionException(exception);
            }
            throw exception;
        }
    }

    private boolean isSequenceCollision(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                    && isSequenceConstraintName(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private boolean isSequenceConstraintName(String constraintName) {
        if (constraintName == null) {
            return false;
        }
        if (isH2MovementPrimaryKeyName(constraintName)) {
            return true;
        }
        String localName = constraintName.substring(constraintName.lastIndexOf('.') + 1)
                .toUpperCase(Locale.ROOT);
        if (SEQUENCE_CONSTRAINT.equals(localName)) {
            return true;
        }
        String h2IndexPrefix = SEQUENCE_CONSTRAINT + "_INDEX_";
        return localName.startsWith(h2IndexPrefix)
                && localName.substring(h2IndexPrefix.length()).chars().allMatch(Character::isDigit);
    }

    private boolean isH2MovementPrimaryKeyName(String constraintName) {
        String[] parts = constraintName.trim().toUpperCase(Locale.ROOT).split("\\s+");
        if (parts.length != 3 || !"INDEX".equals(parts[1])) {
            return false;
        }
        String primaryKeyName = parts[0].substring(parts[0].lastIndexOf('.') + 1);
        String indexName = parts[2].substring(parts[2].lastIndexOf('.') + 1);
        return "PK_MOVIMENTO_MANUAL".equals(primaryKeyName) && indexName.matches("PRIMARY_KEY_\\d+");
    }
}

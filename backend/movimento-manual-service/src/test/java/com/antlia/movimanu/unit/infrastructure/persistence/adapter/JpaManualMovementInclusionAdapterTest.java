package com.antlia.movimanu.unit.infrastructure.persistence.adapter;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;

import com.antlia.movimanu.application.exception.ManualMovementSequenceCollisionException;
import com.antlia.movimanu.application.exception.ProductCosifNotFoundException;
import com.antlia.movimanu.domain.model.ManualMovement;
import com.antlia.movimanu.domain.model.ManualMovementId;
import com.antlia.movimanu.infrastructure.persistence.adapter.JpaManualMovementInclusionAdapter;
import com.antlia.movimanu.infrastructure.persistence.entity.ManualMovementEntity;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntity;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntityId;
import com.antlia.movimanu.infrastructure.persistence.mapper.ManualMovementMapper;
import com.antlia.movimanu.infrastructure.persistence.repository.ProductCosifJpaRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaManualMovementInclusionAdapterTest {
    private static final Instant MOVEMENT_AT = Instant.parse("2026-10-16T12:34:56Z");

    private final EntityManager entityManager = mock(EntityManager.class);
    private final ProductCosifJpaRepository productCosifRepository = mock(ProductCosifJpaRepository.class);
    private final ManualMovementMapper mapper = mock(ManualMovementMapper.class);
    private final JpaManualMovementInclusionAdapter adapter = new JpaManualMovementInclusionAdapter(
            entityManager, productCosifRepository, mapper);
    private final ManualMovement movement = new ManualMovement(
            new ManualMovementId(10, 2026, 4L, "0001", "10000000001"),
            "Ajuste manual",
            MOVEMENT_AT,
            "TESTE",
            new BigDecimal("150.00"));
    private final ProductCosifEntity productCosif = mock(ProductCosifEntity.class);
    private final ManualMovementEntity entity = mock(ManualMovementEntity.class);

    @Test
    void persistsTheMovementForItsExistingProductCosif() {
        prepareCosif();
        ManualMovement expected = movement;
        when(mapper.toEntity(movement, productCosif)).thenReturn(entity);
        when(mapper.toDomain(entity)).thenReturn(expected);

        assertThat(adapter.insert(movement)).isEqualTo(expected);

        verify(productCosifRepository).findById(new ProductCosifEntityId("0001", "10000000001"));
        var persistenceOrder = inOrder(entityManager);
        persistenceOrder.verify(entityManager).persist(entity);
        persistenceOrder.verify(entityManager).flush();
        verify(entityManager, never()).merge(any(ManualMovementEntity.class));
        verify(mapper).toEntity(movement, productCosif);
    }

    @Test
    void rejectsTheMovementWhenItsProductCosifIsMissing() {
        when(productCosifRepository.findById(new ProductCosifEntityId("0001", "10000000001")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.insert(movement)).isInstanceOf(ProductCosifNotFoundException.class);

        verify(entityManager, never()).persist(any());
    }

    @Test
    void translatesTheH2IndexForTheNamedSequenceConstraintIntoARetrySignal() {
        preparePersistenceFailure();
        doThrow(integrityFailure("PUBLIC.UK_MOVIMENTO_MANUAL_PERIODO_LANCAMENTO_INDEX_3"))
                .when(entityManager).flush();

        assertThatThrownBy(() -> adapter.insert(movement))
                .isInstanceOf(ManualMovementSequenceCollisionException.class)
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void recognizesTheExactSequenceConstraintName() {
        preparePersistenceFailure();
        doThrow(integrityFailure("UK_MOVIMENTO_MANUAL_PERIODO_LANCAMENTO"))
                .when(entityManager).flush();

        assertThatThrownBy(() -> adapter.insert(movement)).isInstanceOf(ManualMovementSequenceCollisionException.class);
    }

    @Test
    void recognizesTheH2MovementPrimaryKeyNameReportedForTheSameSequenceConflict() {
        preparePersistenceFailure();
        doThrow(integrityFailure("PUBLIC.PK_MOVIMENTO_MANUAL INDEX PUBLIC.PRIMARY_KEY_3"))
                .when(entityManager).flush();

        assertThatThrownBy(() -> adapter.insert(movement))
                .isInstanceOf(ManualMovementSequenceCollisionException.class);
    }

    @Test
    void propagatesMissingOrDifferentConstraintNamesWithoutMarkingThemAsSequenceCollisions() {
        preparePersistenceFailure();
        DataIntegrityViolationException missingName = integrityFailure(null);
        DataIntegrityViolationException differentName = integrityFailure(
                "UK_MOVIMENTO_MANUAL_PERIODO_LANCAMENTO_INDEX_X");
        DataIntegrityViolationException differentPrimaryKey = integrityFailure(
                "PUBLIC.PK_PRODUCT INDEX PUBLIC.PRIMARY_KEY_1");
        doThrow(missingName, differentName, differentPrimaryKey).when(entityManager).flush();

        assertThatThrownBy(() -> adapter.insert(movement)).isSameAs(missingName);
        assertThatThrownBy(() -> adapter.insert(movement)).isSameAs(differentName);
        assertThatThrownBy(() -> adapter.insert(movement)).isSameAs(differentPrimaryKey);
    }

    @Test
    void propagatesOtherPersistenceErrorsWithoutMarkingThemAsSequenceCollisions() {
        preparePersistenceFailure();
        DataIntegrityViolationException failure = new DataIntegrityViolationException("outra restrição");
        doThrow(failure).when(entityManager).flush();

        assertThatThrownBy(() -> adapter.insert(movement)).isSameAs(failure);
    }

    private void prepareCosif() {
        when(productCosifRepository.findById(new ProductCosifEntityId("0001", "10000000001")))
                .thenReturn(Optional.of(productCosif));
    }

    private void preparePersistenceFailure() {
        prepareCosif();
        when(mapper.toEntity(movement, productCosif)).thenReturn(entity);
    }

    private DataIntegrityViolationException integrityFailure(String constraintName) {
        ConstraintViolationException violation = new ConstraintViolationException(
                "unique collision", new SQLException("unique collision"), "insert", constraintName);
        return new DataIntegrityViolationException("constraint violation", violation);
    }
}

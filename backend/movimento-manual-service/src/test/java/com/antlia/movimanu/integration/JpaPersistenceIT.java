package com.antlia.movimanu.integration;

import java.math.BigDecimal;
import java.time.Instant;

import com.antlia.movimanu.domain.model.ManualMovement;
import com.antlia.movimanu.domain.model.ManualMovementId;
import com.antlia.movimanu.infrastructure.persistence.entity.ManualMovementEntity;
import com.antlia.movimanu.infrastructure.persistence.entity.ManualMovementEntityId;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntity;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntityId;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductEntity;
import com.antlia.movimanu.infrastructure.persistence.mapper.ManualMovementMapper;
import com.antlia.movimanu.infrastructure.persistence.mapper.ProductMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
@Sql("/fixtures/persistence-fixtures.sql")
class JpaPersistenceIT {

    @PersistenceContext
    private EntityManager entityManager;

    private final ProductMapper productMapper = new ProductMapper();
    private final ManualMovementMapper movementMapper = new ManualMovementMapper();

    @Test
    void mapsEntitiesAndPersistsTheComposedRelationshipsAndSmallDateTimeValue() {
        ProductEntity product = entityManager.find(ProductEntity.class, "0001");
        assertThat(productMapper.toDomain(product).description()).isEqualTo("Produto de fixture A");

        ProductCosifEntityId cosifId = new ProductCosifEntityId("0001", "10000000001");
        ProductCosifEntity cosif = entityManager.find(ProductCosifEntity.class, cosifId);
        assertThat(cosif.getProduct().getCode()).isEqualTo("0001");

        ManualMovement movement = new ManualMovement(
                new ManualMovementId(2, 2025, 1L, "0001", "10000000001"),
                "Ajuste de teste",
                Instant.parse("2025-02-03T12:34:29.999Z"),
                "TESTE",
                new BigDecimal("150.25"));
        entityManager.persist(movementMapper.toEntity(movement, cosif));
        entityManager.flush();
        entityManager.clear();

        ManualMovementEntityId movementId = new ManualMovementEntityId(
                2, 2025, 1L, "0001", "10000000001");
        ManualMovementEntity persisted = entityManager.find(ManualMovementEntity.class, movementId);

        assertThat(persisted.getId()).isEqualTo(movementId);
        assertThat(persisted.getDescription()).isEqualTo("Ajuste de teste");
        assertThat(persisted.getMovementAt()).isEqualTo(Instant.parse("2025-02-03T12:35:00Z"));
        assertThat(persisted.getUserCode()).isEqualTo("TESTE");
        assertThat(persisted.getValue()).isEqualByComparingTo("150.25");
        assertThat(persisted.getProductCosif().getId()).isEqualTo(cosifId);
        assertThat(movementMapper.toDomain(persisted).movementAt())
                .isEqualTo(Instant.parse("2025-02-03T12:35:00Z"));
    }

    @Test
    void rejectsRepeatedLaunchNumberForTheSamePeriodAcrossProducts() {
        ProductCosifEntity firstCosif = entityManager.find(
                ProductCosifEntity.class,
                new ProductCosifEntityId("0001", "10000000001"));
        ProductCosifEntity secondCosif = entityManager.find(
                ProductCosifEntity.class,
                new ProductCosifEntityId("0002", "20000000001"));

        ManualMovement first = movement(1L, "0001", "10000000001");
        ManualMovement second = movement(1L, "0002", "20000000001");
        entityManager.persist(movementMapper.toEntity(first, firstCosif));
        entityManager.persist(movementMapper.toEntity(second, secondCosif));

        assertThatThrownBy(entityManager::flush).isInstanceOf(PersistenceException.class);
    }

    private ManualMovement movement(Long launchNumber, String productCode, String cosifCode) {
        return new ManualMovement(
                new ManualMovementId(3, 2025, launchNumber, productCode, cosifCode),
                "Inclusão concorrente de teste",
                Instant.parse("2025-03-01T10:00:00Z"),
                "TESTE",
                new BigDecimal("1.00"));
    }
}

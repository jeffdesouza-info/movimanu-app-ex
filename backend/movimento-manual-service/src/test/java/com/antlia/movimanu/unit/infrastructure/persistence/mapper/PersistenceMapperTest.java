package com.antlia.movimanu.unit.infrastructure.persistence.mapper;

import java.math.BigDecimal;
import java.time.Instant;

import com.antlia.movimanu.domain.model.ManualMovement;
import com.antlia.movimanu.domain.model.ManualMovementId;
import com.antlia.movimanu.domain.model.Product;
import com.antlia.movimanu.domain.model.ProductCosif;
import com.antlia.movimanu.domain.model.ProductCosifId;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntity;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntityId;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductEntity;
import com.antlia.movimanu.infrastructure.persistence.mapper.ManualMovementMapper;
import com.antlia.movimanu.infrastructure.persistence.mapper.ProductCosifMapper;
import com.antlia.movimanu.infrastructure.persistence.mapper.ProductMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PersistenceMapperTest {

    private final ProductMapper productMapper = new ProductMapper();
    private final ProductCosifMapper productCosifMapper = new ProductCosifMapper();
    private final ManualMovementMapper movementMapper = new ManualMovementMapper();

    @Test
    void mapsProductDataBetweenDomainAndJpaEntity() {
        Product product = new Product("0001", "Produto de teste");

        ProductEntity entity = productMapper.toEntity(product);

        assertThat(entity.getCode()).isEqualTo("0001");
        assertThat(entity.getDescription()).isEqualTo("Produto de teste");
        assertThat(entity.getStatus()).isNull();
        assertThat(productMapper.toDomain(new ProductEntity("0001", "Produto de teste", "A")))
                .isEqualTo(product);
    }

    @Test
    void mapsProductCosifDataAndRequiresItsMatchingProduct() {
        ProductCosif productCosif = new ProductCosif(
                new ProductCosifId("0001", "10000000001"), "100001");
        ProductEntity productEntity = new ProductEntity("0001", "Produto de teste", "A");

        ProductCosifEntity entity = productCosifMapper.toEntity(productCosif, productEntity);

        assertThat(entity.getId()).isEqualTo(new ProductCosifEntityId("0001", "10000000001"));
        assertThat(entity.getClassificationCode()).isEqualTo("100001");
        assertThat(entity.getStatus()).isNull();
        assertThat(entity.getProduct()).isSameAs(productEntity);
        assertThat(productCosifMapper.toDomain(entity)).isEqualTo(productCosif);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> productCosifMapper.toEntity(
                        productCosif,
                        new ProductEntity("0002", "Outro produto", "A")));
    }

    @Test
    void mapsManualMovementAndNormalizesItsTimestampForJpa() {
        ProductCosifEntity productCosifEntity = new ProductCosifEntity(
                new ProductCosifEntityId("0001", "10000000001"),
                "100001",
                "A",
                new ProductEntity("0001", "Produto de teste", "A"));
        ManualMovement movement = new ManualMovement(
                new ManualMovementId(2, 2025, 1L, "0001", "10000000001"),
                "Ajuste de teste",
                Instant.parse("2025-02-03T12:34:29.999Z"),
                "TESTE",
                new BigDecimal("150.25"));

        var entity = movementMapper.toEntity(movement, productCosifEntity);

        assertThat(entity.getId().getMonth()).isEqualTo(2);
        assertThat(entity.getId().getYear()).isEqualTo(2025);
        assertThat(entity.getId().getLaunchNumber()).isEqualTo(1L);
        assertThat(entity.getDescription()).isEqualTo("Ajuste de teste");
        assertThat(entity.getMovementAt()).isEqualTo(Instant.parse("2025-02-03T12:35:00Z"));
        assertThat(entity.getUserCode()).isEqualTo("TESTE");
        assertThat(entity.getValue()).isEqualByComparingTo("150.25");
        assertThat(entity.getProductCosif()).isSameAs(productCosifEntity);
        assertThat(movementMapper.toDomain(entity)).isEqualTo(new ManualMovement(
                movement.id(),
                movement.description(),
                Instant.parse("2025-02-03T12:35:00Z"),
                movement.userCode(),
                movement.value()));

        ProductCosifEntity otherCosif = new ProductCosifEntity(
                new ProductCosifEntityId("0002", "20000000001"),
                "200001",
                "A",
                new ProductEntity("0002", "Outro produto", "A"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> movementMapper.toEntity(movement, otherCosif));
    }
}

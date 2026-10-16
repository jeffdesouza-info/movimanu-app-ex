package com.antlia.movimanu.unit.infrastructure.persistence.entity;

import com.antlia.movimanu.infrastructure.persistence.entity.ManualMovementEntityId;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntityId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceEntityIdTest {

    @Test
    void comparesProductCosifIdentifiersByTheirComponents() {
        ProductCosifEntityId first = new ProductCosifEntityId("0001", "10000000001");
        ProductCosifEntityId same = new ProductCosifEntityId("0001", "10000000001");

        assertThat(first.getProductCode()).isEqualTo("0001");
        assertThat(first.getCosifCode()).isEqualTo("10000000001");
        assertThat(first).isEqualTo(first).isEqualTo(same);
        assertThat(first).isNotEqualTo(new ProductCosifEntityId("0002", "10000000001"));
        assertThat(first).isNotEqualTo(new Object());
        assertThat(first.hashCode()).isEqualTo(same.hashCode());
    }

    @Test
    void comparesManualMovementIdentifiersByTheirComponents() {
        ManualMovementEntityId first = new ManualMovementEntityId(2, 2025, 1L, "0001", "10000000001");
        ManualMovementEntityId same = new ManualMovementEntityId(2, 2025, 1L, "0001", "10000000001");

        assertThat(first.getMonth()).isEqualTo(2);
        assertThat(first.getYear()).isEqualTo(2025);
        assertThat(first.getLaunchNumber()).isEqualTo(1L);
        assertThat(first.getProductCode()).isEqualTo("0001");
        assertThat(first.getCosifCode()).isEqualTo("10000000001");
        assertThat(first).isEqualTo(first).isEqualTo(same);
        assertThat(first).isNotEqualTo(new ManualMovementEntityId(2, 2025, 2L, "0001", "10000000001"));
        assertThat(first).isNotEqualTo(new Object());
        assertThat(first.hashCode()).isEqualTo(same.hashCode());
    }

    @Test
    void exposesTheJpaNoArgumentConstructorForManualMovementId() throws ReflectiveOperationException {
        var constructor = ManualMovementEntityId.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        ManualMovementEntityId id = constructor.newInstance();

        assertThat(id.getMonth()).isZero();
        assertThat(id.getYear()).isZero();
        assertThat(id.getLaunchNumber()).isNull();
        assertThat(id.getProductCode()).isNull();
        assertThat(id.getCosifCode()).isNull();
    }
}

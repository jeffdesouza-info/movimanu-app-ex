package com.antlia.movimanu.unit.infrastructure.persistence.adapter;

import java.util.List;

import com.antlia.movimanu.domain.model.ProductCosif;
import com.antlia.movimanu.domain.model.ProductCosifId;
import com.antlia.movimanu.infrastructure.persistence.adapter.JpaProductCosifQueryAdapter;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntity;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntityId;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductEntity;
import com.antlia.movimanu.infrastructure.persistence.repository.ProductCosifJpaRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaProductCosifQueryAdapterTest {
    private final ProductCosifJpaRepository repository = mock(ProductCosifJpaRepository.class);
    private final JpaProductCosifQueryAdapter adapter = new JpaProductCosifQueryAdapter(repository);

    @Test
    void mapsCosifsForTheRequestedProduct() {
        ProductEntity product = new ProductEntity("0001", "Produto A", "A");
        when(repository.findAllByProductCode("0001")).thenReturn(List.of(
                new ProductCosifEntity(
                        new ProductCosifEntityId("0001", "10000000001"),
                        "100001",
                        "A",
                        product)));

        assertThat(adapter.findAllByProduct("0001")).containsExactly(new ProductCosif(
                new ProductCosifId("0001", "10000000001"),
                "100001"));
        verify(repository).findAllByProductCode("0001");
    }

    @Test
    void returnsAnEmptyCosifList() {
        when(repository.findAllByProductCode("0003")).thenReturn(List.of());

        assertThat(adapter.findAllByProduct("0003")).isEmpty();
    }
}

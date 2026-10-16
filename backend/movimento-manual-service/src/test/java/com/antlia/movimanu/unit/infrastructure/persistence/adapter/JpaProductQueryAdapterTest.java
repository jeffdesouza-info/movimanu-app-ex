package com.antlia.movimanu.unit.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import com.antlia.movimanu.domain.model.Product;
import com.antlia.movimanu.infrastructure.persistence.adapter.JpaProductQueryAdapter;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductEntity;
import com.antlia.movimanu.infrastructure.persistence.repository.ProductJpaRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JpaProductQueryAdapterTest {
    private final ProductJpaRepository repository = mock(ProductJpaRepository.class);
    private final JpaProductQueryAdapter adapter = new JpaProductQueryAdapter(repository);

    @Test
    void mapsAllProductsToDomainInRepositoryOrder() {
        when(repository.findAllByOrderByCodeAsc()).thenReturn(List.of(
                new ProductEntity("0001", "Produto A", "A"),
                new ProductEntity("0002", "Produto B", "A")));

        assertThat(adapter.findAll()).containsExactly(
                new Product("0001", "Produto A"),
                new Product("0002", "Produto B"));
        verify(repository).findAllByOrderByCodeAsc();
    }

    @Test
    void mapsAProductByCodeAndPreservesAbsence() {
        when(repository.findById("0001"))
                .thenReturn(Optional.of(new ProductEntity("0001", "Produto A", "A")));
        when(repository.findById("9999")).thenReturn(Optional.empty());

        assertThat(adapter.findByCode("0001")).contains(new Product("0001", "Produto A"));
        assertThat(adapter.findByCode("9999")).isEmpty();
    }

    @Test
    void returnsAnEmptyProductList() {
        when(repository.findAllByOrderByCodeAsc()).thenReturn(List.of());

        assertThat(adapter.findAll()).isEmpty();
    }
}

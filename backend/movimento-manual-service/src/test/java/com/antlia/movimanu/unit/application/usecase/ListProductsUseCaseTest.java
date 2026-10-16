package com.antlia.movimanu.unit.application.usecase;

import java.util.List;

import com.antlia.movimanu.application.port.ProductQueryPort;
import com.antlia.movimanu.application.usecase.ListProductsUseCase;
import com.antlia.movimanu.domain.model.Product;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListProductsUseCaseTest {
    private final ProductQueryPort productQueryPort = mock(ProductQueryPort.class);
    private final ListProductsUseCase useCase = new ListProductsUseCase(productQueryPort);

    @Test
    void returnsProductsFromTheQueryPort() {
        List<Product> products = List.of(new Product("0001", "Produto A"));
        when(productQueryPort.findAll()).thenReturn(products);

        assertThat(useCase.execute()).isEqualTo(products);
        verify(productQueryPort).findAll();
    }

    @Test
    void returnsAnEmptyListWhenThereAreNoProducts() {
        when(productQueryPort.findAll()).thenReturn(List.of());

        assertThat(useCase.execute()).isEmpty();
    }

    @Test
    void requiresAProductQueryPort() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ListProductsUseCase(null))
                .withMessage("A consulta de produtos é obrigatória.");
    }
}

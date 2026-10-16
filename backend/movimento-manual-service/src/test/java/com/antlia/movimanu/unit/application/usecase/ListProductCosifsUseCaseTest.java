package com.antlia.movimanu.unit.application.usecase;

import java.util.List;
import java.util.Optional;

import com.antlia.movimanu.application.exception.ProductNotFoundException;
import com.antlia.movimanu.application.port.ProductCosifQueryPort;
import com.antlia.movimanu.application.port.ProductQueryPort;
import com.antlia.movimanu.application.usecase.ListProductCosifsUseCase;
import com.antlia.movimanu.domain.model.Product;
import com.antlia.movimanu.domain.model.ProductCosif;
import com.antlia.movimanu.domain.model.ProductCosifId;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListProductCosifsUseCaseTest {
    private final ProductQueryPort productQueryPort = mock(ProductQueryPort.class);
    private final ProductCosifQueryPort productCosifQueryPort = mock(ProductCosifQueryPort.class);
    private final ListProductCosifsUseCase useCase = new ListProductCosifsUseCase(
            productQueryPort,
            productCosifQueryPort);

    @Test
    void returnsCosifsForAnExistingProduct() {
        String productCode = "0001";
        List<ProductCosif> cosifs = List.of(new ProductCosif(
                new ProductCosifId(productCode, "10000000001"),
                "100001"));
        when(productQueryPort.findByCode(productCode)).thenReturn(Optional.of(new Product(productCode, "Produto A")));
        when(productCosifQueryPort.findAllByProduct(productCode)).thenReturn(cosifs);

        assertThat(useCase.execute(productCode)).isEqualTo(cosifs);
        verify(productCosifQueryPort).findAllByProduct(productCode);
    }

    @Test
    void returnsAnEmptyListForAnExistingProductWithoutCosifs() {
        String productCode = "0001";
        when(productQueryPort.findByCode(productCode)).thenReturn(Optional.of(new Product(productCode, "Produto A")));
        when(productCosifQueryPort.findAllByProduct(productCode)).thenReturn(List.of());

        assertThat(useCase.execute(productCode)).isEmpty();
    }

    @Test
    void reportsAnUnknownProductWithoutQueryingItsCosifs() {
        String productCode = "9999";
        when(productQueryPort.findByCode(productCode)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(productCode))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Produto não encontrado: 9999");
        verify(productCosifQueryPort, never()).findAllByProduct(productCode);
    }

    @Test
    void requiresBothQueryPorts() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ListProductCosifsUseCase(null, productCosifQueryPort))
                .withMessage("A consulta de produtos é obrigatória.");
        assertThatNullPointerException()
                .isThrownBy(() -> new ListProductCosifsUseCase(productQueryPort, null))
                .withMessage("A consulta de COSIFs é obrigatória.");
    }
}

package com.antlia.movimanu.application.usecase;

import java.util.List;
import java.util.Objects;

import com.antlia.movimanu.application.exception.ProductNotFoundException;
import com.antlia.movimanu.application.port.ProductCosifQueryPort;
import com.antlia.movimanu.application.port.ProductQueryPort;
import com.antlia.movimanu.domain.model.ProductCosif;

public class ListProductCosifsUseCase {
    private final ProductQueryPort productQueryPort;
    private final ProductCosifQueryPort productCosifQueryPort;

    public ListProductCosifsUseCase(
            ProductQueryPort productQueryPort,
            ProductCosifQueryPort productCosifQueryPort) {
        this.productQueryPort = Objects.requireNonNull(productQueryPort, "A consulta de produtos é obrigatória.");
        this.productCosifQueryPort = Objects.requireNonNull(
                productCosifQueryPort,
                "A consulta de COSIFs é obrigatória.");
    }

    public List<ProductCosif> execute(String productCode) {
        productQueryPort.findByCode(productCode)
                .orElseThrow(() -> new ProductNotFoundException(productCode));
        return productCosifQueryPort.findAllByProduct(productCode);
    }
}

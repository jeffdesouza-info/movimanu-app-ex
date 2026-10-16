package com.antlia.movimanu.application.usecase;

import java.util.List;
import java.util.Objects;

import com.antlia.movimanu.application.port.ProductQueryPort;
import com.antlia.movimanu.domain.model.Product;

public class ListProductsUseCase {
    private final ProductQueryPort productQueryPort;

    public ListProductsUseCase(ProductQueryPort productQueryPort) {
        this.productQueryPort = Objects.requireNonNull(productQueryPort, "A consulta de produtos é obrigatória.");
    }

    public List<Product> execute() {
        return productQueryPort.findAll();
    }
}

package com.antlia.movimanu.application.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String productCode) {
        super("Produto não encontrado: " + productCode);
    }
}

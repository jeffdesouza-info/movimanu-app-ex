package com.antlia.movimanu.application.exception;

public class ProductCosifNotFoundException extends RuntimeException {
    public ProductCosifNotFoundException(String productCode, String cosifCode) {
        super("Vínculo produto/COSIF não encontrado: " + productCode + "/" + cosifCode);
    }
}

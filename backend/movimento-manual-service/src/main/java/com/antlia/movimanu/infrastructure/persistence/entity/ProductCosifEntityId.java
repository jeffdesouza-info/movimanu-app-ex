package com.antlia.movimanu.infrastructure.persistence.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ProductCosifEntityId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "COD_PRODUTO", nullable = false, columnDefinition = "CHAR(4)")
    private String productCode;

    @Column(name = "COD_COSIF", nullable = false, columnDefinition = "CHAR(11)")
    private String cosifCode;

    protected ProductCosifEntityId() {
    }

    public ProductCosifEntityId(String productCode, String cosifCode) {
        this.productCode = productCode;
        this.cosifCode = cosifCode;
    }

    public String getProductCode() {
        return productCode;
    }

    public String getCosifCode() {
        return cosifCode;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ProductCosifEntityId that)) {
            return false;
        }
        return Objects.equals(productCode, that.productCode)
                && Objects.equals(cosifCode, that.cosifCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productCode, cosifCode);
    }
}

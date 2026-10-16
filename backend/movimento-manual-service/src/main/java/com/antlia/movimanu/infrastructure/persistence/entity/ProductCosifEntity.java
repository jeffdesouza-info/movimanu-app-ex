package com.antlia.movimanu.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "PRODUTO_COSIF")
public class ProductCosifEntity {

    @EmbeddedId
    private ProductCosifEntityId id;

    @Column(name = "COD_CLASSIFICACAO", columnDefinition = "CHAR(6)")
    private String classificationCode;

    @Column(name = "STA_STATUS", columnDefinition = "CHAR(1)")
    private String status;

    @MapsId("productCode")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "COD_PRODUTO", nullable = false, columnDefinition = "CHAR(4)")
    private ProductEntity product;

    protected ProductCosifEntity() {
    }

    public ProductCosifEntity(
            ProductCosifEntityId id,
            String classificationCode,
            String status,
            ProductEntity product) {
        this.id = id;
        this.classificationCode = classificationCode;
        this.status = status;
        this.product = product;
    }

    public ProductCosifEntityId getId() {
        return id;
    }

    public String getClassificationCode() {
        return classificationCode;
    }

    public String getStatus() {
        return status;
    }

    public ProductEntity getProduct() {
        return product;
    }
}

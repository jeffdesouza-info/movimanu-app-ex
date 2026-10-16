package com.antlia.movimanu.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "PRODUTO")
public class ProductEntity {

    @Id
    @Column(name = "COD_PRODUTO", nullable = false, columnDefinition = "CHAR(4)")
    private String code;

    @Column(name = "DES_PRODUTO", length = 30)
    private String description;

    @Column(name = "STA_STATUS", columnDefinition = "CHAR(1)")
    private String status;

    protected ProductEntity() {
    }

    public ProductEntity(String code, String description, String status) {
        this.code = code;
        this.description = description;
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }
}

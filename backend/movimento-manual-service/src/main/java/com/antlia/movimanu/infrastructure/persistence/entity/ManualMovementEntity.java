package com.antlia.movimanu.infrastructure.persistence.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.antlia.movimanu.infrastructure.persistence.converter.SmallDateTimeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "MOVIMENTO_MANUAL", uniqueConstraints = {
        @UniqueConstraint(
                name = "UK_MOVIMENTO_MANUAL_PERIODO_LANCAMENTO",
                columnNames = {"DAT_ANO", "DAT_MES", "NUM_LANCAMENTO"})
})
public class ManualMovementEntity {

    @EmbeddedId
    private ManualMovementEntityId id;

    @Column(name = "DES_DESCRICAO", nullable = false, length = 50)
    private String description;

    @Column(name = "DAT_MOVIMENTO", nullable = false, columnDefinition = "SMALLDATETIME")
    @Convert(converter = SmallDateTimeConverter.class)
    private Instant movementAt;

    @Column(name = "COD_USUARIO", nullable = false, length = 15)
    private String userCode;

    @Column(name = "VAL_VALOR", nullable = false, precision = 18, scale = 2)
    private BigDecimal value;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(
                    name = "COD_PRODUTO",
                    referencedColumnName = "COD_PRODUTO",
                    insertable = false,
                    updatable = false),
            @JoinColumn(
                    name = "COD_COSIF",
                    referencedColumnName = "COD_COSIF",
                    insertable = false,
                    updatable = false)
    })
    private ProductCosifEntity productCosif;

    protected ManualMovementEntity() {
    }

    public ManualMovementEntity(
            ManualMovementEntityId id,
            String description,
            Instant movementAt,
            String userCode,
            BigDecimal value,
            ProductCosifEntity productCosif) {
        this.id = id;
        this.description = description;
        this.movementAt = movementAt;
        this.userCode = userCode;
        this.value = value;
        this.productCosif = productCosif;
    }

    public ManualMovementEntityId getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Instant getMovementAt() {
        return movementAt;
    }

    public String getUserCode() {
        return userCode;
    }

    public BigDecimal getValue() {
        return value;
    }

    public ProductCosifEntity getProductCosif() {
        return productCosif;
    }
}

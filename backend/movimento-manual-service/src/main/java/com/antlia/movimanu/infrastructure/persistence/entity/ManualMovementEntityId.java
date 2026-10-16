package com.antlia.movimanu.infrastructure.persistence.entity;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class ManualMovementEntityId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "DAT_MES", nullable = false, columnDefinition = "NUMERIC(2,0)")
    private int month;

    @Column(name = "DAT_ANO", nullable = false, columnDefinition = "NUMERIC(4,0)")
    private int year;

    @Column(name = "NUM_LANCAMENTO", nullable = false, columnDefinition = "NUMERIC(18,0)")
    private Long launchNumber;

    @Column(name = "COD_PRODUTO", nullable = false, columnDefinition = "CHAR(4)")
    private String productCode;

    @Column(name = "COD_COSIF", nullable = false, columnDefinition = "CHAR(11)")
    private String cosifCode;

    protected ManualMovementEntityId() {
    }

    public ManualMovementEntityId(
            int month,
            int year,
            Long launchNumber,
            String productCode,
            String cosifCode) {
        this.month = month;
        this.year = year;
        this.launchNumber = launchNumber;
        this.productCode = productCode;
        this.cosifCode = cosifCode;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public Long getLaunchNumber() {
        return launchNumber;
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
        if (!(other instanceof ManualMovementEntityId that)) {
            return false;
        }
        return month == that.month
                && year == that.year
                && Objects.equals(launchNumber, that.launchNumber)
                && Objects.equals(productCode, that.productCode)
                && Objects.equals(cosifCode, that.cosifCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(month, year, launchNumber, productCode, cosifCode);
    }
}

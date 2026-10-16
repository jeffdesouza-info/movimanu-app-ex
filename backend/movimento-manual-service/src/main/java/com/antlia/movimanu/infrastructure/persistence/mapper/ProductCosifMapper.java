package com.antlia.movimanu.infrastructure.persistence.mapper;

import java.util.Objects;

import com.antlia.movimanu.domain.model.ProductCosif;
import com.antlia.movimanu.domain.model.ProductCosifId;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntity;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntityId;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductEntity;

public class ProductCosifMapper {

    public ProductCosif toDomain(ProductCosifEntity entity) {
        Objects.requireNonNull(entity, "A entidade produto/COSIF é obrigatória.");
        ProductCosifEntityId id = entity.getId();
        return new ProductCosif(
                new ProductCosifId(id.getProductCode(), id.getCosifCode()),
                entity.getClassificationCode());
    }

    public ProductCosifEntity toEntity(ProductCosif productCosif, ProductEntity product) {
        Objects.requireNonNull(productCosif, "O produto/COSIF de domínio é obrigatório.");
        Objects.requireNonNull(product, "A entidade de produto é obrigatória.");
        if (!productCosif.id().productCode().equals(product.getCode())) {
            throw new IllegalArgumentException("O produto associado deve corresponder ao identificador do COSIF.");
        }
        return new ProductCosifEntity(
                new ProductCosifEntityId(productCosif.id().productCode(), productCosif.id().cosifCode()),
                productCosif.classificationCode(),
                null,
                product);
    }
}

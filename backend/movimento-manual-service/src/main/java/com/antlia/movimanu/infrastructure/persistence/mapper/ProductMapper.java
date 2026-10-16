package com.antlia.movimanu.infrastructure.persistence.mapper;

import java.util.Objects;

import com.antlia.movimanu.domain.model.Product;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductEntity;

public class ProductMapper {

    public Product toDomain(ProductEntity entity) {
        Objects.requireNonNull(entity, "A entidade de produto é obrigatória.");
        return new Product(entity.getCode(), entity.getDescription());
    }

    public ProductEntity toEntity(Product product) {
        Objects.requireNonNull(product, "O produto de domínio é obrigatório.");
        return new ProductEntity(product.code(), product.description(), null);
    }
}

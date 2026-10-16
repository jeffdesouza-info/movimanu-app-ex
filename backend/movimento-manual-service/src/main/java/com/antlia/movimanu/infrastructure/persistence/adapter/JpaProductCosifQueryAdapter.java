package com.antlia.movimanu.infrastructure.persistence.adapter;

import java.util.List;

import com.antlia.movimanu.application.port.ProductCosifQueryPort;
import com.antlia.movimanu.domain.model.ProductCosif;
import com.antlia.movimanu.infrastructure.persistence.mapper.ProductCosifMapper;
import com.antlia.movimanu.infrastructure.persistence.repository.ProductCosifJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaProductCosifQueryAdapter implements ProductCosifQueryPort {
    private final ProductCosifJpaRepository repository;
    private final ProductCosifMapper mapper = new ProductCosifMapper();

    public JpaProductCosifQueryAdapter(ProductCosifJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ProductCosif> findAllByProduct(String productCode) {
        return repository.findAllByProductCode(productCode).stream().map(mapper::toDomain).toList();
    }
}

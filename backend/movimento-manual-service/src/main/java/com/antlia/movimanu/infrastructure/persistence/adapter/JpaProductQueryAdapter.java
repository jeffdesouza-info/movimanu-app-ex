package com.antlia.movimanu.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import com.antlia.movimanu.application.port.ProductQueryPort;
import com.antlia.movimanu.domain.model.Product;
import com.antlia.movimanu.infrastructure.persistence.mapper.ProductMapper;
import com.antlia.movimanu.infrastructure.persistence.repository.ProductJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaProductQueryAdapter implements ProductQueryPort {
    private final ProductJpaRepository repository;
    private final ProductMapper mapper = new ProductMapper();

    public JpaProductQueryAdapter(ProductJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Product> findAll() {
        return repository.findAllByOrderByCodeAsc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Product> findByCode(String productCode) {
        return repository.findById(productCode).map(mapper::toDomain);
    }
}

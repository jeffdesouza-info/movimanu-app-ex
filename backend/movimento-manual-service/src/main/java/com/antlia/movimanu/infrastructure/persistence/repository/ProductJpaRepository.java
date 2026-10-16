package com.antlia.movimanu.infrastructure.persistence.repository;

import java.util.List;

import com.antlia.movimanu.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<ProductEntity, String> {
    List<ProductEntity> findAllByOrderByCodeAsc();
}

package com.antlia.movimanu.infrastructure.persistence.repository;

import java.util.List;

import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntity;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductCosifJpaRepository extends JpaRepository<ProductCosifEntity, ProductCosifEntityId> {
    @Query("""
            select cosif
            from ProductCosifEntity cosif
            where cosif.id.productCode = :productCode
            order by cosif.id.cosifCode asc
            """)
    List<ProductCosifEntity> findAllByProductCode(@Param("productCode") String productCode);
}

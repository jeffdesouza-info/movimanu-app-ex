package com.antlia.movimanu.infrastructure.persistence.repository;

import java.util.Optional;

import com.antlia.movimanu.infrastructure.persistence.entity.ManualMovementEntity;
import com.antlia.movimanu.infrastructure.persistence.entity.ManualMovementEntityId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ManualMovementJpaRepository extends JpaRepository<ManualMovementEntity, ManualMovementEntityId> {
    @Query(value = """
            select movement
            from ManualMovementEntity movement
            join fetch movement.productCosif cosif
            join fetch cosif.product product
            order by movement.id.year desc,
                     movement.id.month desc,
                     movement.id.launchNumber desc,
                     movement.id.productCode asc,
                     movement.id.cosifCode asc
            """,
            countQuery = "select count(movement) from ManualMovementEntity movement")
    Page<ManualMovementEntity> findMovementPage(Pageable pageable);

    @Query("""
            select max(movement.id.launchNumber)
            from ManualMovementEntity movement
            where movement.id.month = :month and movement.id.year = :year
            """)
    Optional<Long> findHighestNumberFor(@Param("month") int month, @Param("year") int year);
}

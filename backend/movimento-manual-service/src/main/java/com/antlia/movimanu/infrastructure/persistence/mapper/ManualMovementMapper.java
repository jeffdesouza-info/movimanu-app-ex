package com.antlia.movimanu.infrastructure.persistence.mapper;

import java.util.Objects;

import com.antlia.movimanu.application.projection.ManualMovementProjection;
import com.antlia.movimanu.domain.model.ManualMovement;
import com.antlia.movimanu.domain.model.ManualMovementId;
import com.antlia.movimanu.infrastructure.persistence.converter.SmallDateTimeConverter;
import com.antlia.movimanu.infrastructure.persistence.entity.ManualMovementEntity;
import com.antlia.movimanu.infrastructure.persistence.entity.ManualMovementEntityId;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntity;
import com.antlia.movimanu.infrastructure.persistence.entity.ProductCosifEntityId;
import org.springframework.stereotype.Component;

@Component
public class ManualMovementMapper {
    public ManualMovement toDomain(ManualMovementEntity entity) {
        Objects.requireNonNull(entity, "A entidade do movimento é obrigatória.");
        ManualMovementEntityId id = entity.getId();
        return new ManualMovement(
                new ManualMovementId(
                        id.getMonth(),
                        id.getYear(),
                        id.getLaunchNumber(),
                        id.getProductCode(),
                        id.getCosifCode()),
                entity.getDescription(),
                entity.getMovementAt(),
                entity.getUserCode(),
                entity.getValue());
    }

    public ManualMovementProjection toProjection(ManualMovementEntity entity) {
        Objects.requireNonNull(entity, "A entidade do movimento é obrigatória.");
        ManualMovementEntityId id = entity.getId();
        ProductCosifEntity productCosif = entity.getProductCosif();
        return new ManualMovementProjection(
                id.getMonth(),
                id.getYear(),
                id.getProductCode(),
                id.getCosifCode(),
                entity.getDescription(),
                entity.getValue(),
                productCosif.getProduct().getDescription(),
                id.getLaunchNumber(),
                entity.getMovementAt(),
                entity.getUserCode());
    }

    public ManualMovementEntity toEntity(ManualMovement movement, ProductCosifEntity productCosif) {
        Objects.requireNonNull(movement, "O movimento de domínio é obrigatório.");
        Objects.requireNonNull(productCosif, "A entidade produto/COSIF é obrigatória.");
        ManualMovementId movementId = movement.id();
        ProductCosifEntityId cosifId = productCosif.getId();
        if (!movementId.productCode().equals(cosifId.getProductCode())
                || !movementId.cosifCode().equals(cosifId.getCosifCode())) {
            throw new IllegalArgumentException("O movimento deve referenciar o produto/COSIF informado.");
        }
        return new ManualMovementEntity(
                new ManualMovementEntityId(
                        movementId.month(),
                        movementId.year(),
                        movementId.launchNumber(),
                        movementId.productCode(),
                        movementId.cosifCode()),
                movement.description(),
                SmallDateTimeConverter.normalize(movement.movementAt()),
                movement.userCode(),
                movement.value(),
                productCosif);
    }
}

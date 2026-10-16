package com.antlia.movimanu.application.port;

import com.antlia.movimanu.domain.model.ProductCosif;
import java.util.List;

public interface ProductCosifQueryPort {
    List<ProductCosif> findAllByProduct(String productCode);
}

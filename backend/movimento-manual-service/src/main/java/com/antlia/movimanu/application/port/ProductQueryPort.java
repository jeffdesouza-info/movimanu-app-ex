package com.antlia.movimanu.application.port;

import com.antlia.movimanu.domain.model.Product;
import java.util.List;
import java.util.Optional;

public interface ProductQueryPort {
    List<Product> findAll();

    Optional<Product> findByCode(String productCode);
}

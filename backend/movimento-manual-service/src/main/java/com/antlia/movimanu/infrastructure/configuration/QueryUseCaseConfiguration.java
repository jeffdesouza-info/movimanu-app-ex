package com.antlia.movimanu.infrastructure.configuration;

import com.antlia.movimanu.application.port.ManualMovementQueryPort;
import com.antlia.movimanu.application.port.ProductCosifQueryPort;
import com.antlia.movimanu.application.port.ProductQueryPort;
import com.antlia.movimanu.application.usecase.FindManualMovementsUseCase;
import com.antlia.movimanu.application.usecase.ListProductCosifsUseCase;
import com.antlia.movimanu.application.usecase.ListProductsUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueryUseCaseConfiguration {
    @Bean
    ListProductsUseCase listProductsUseCase(ProductQueryPort productQueryPort) {
        return new ListProductsUseCase(productQueryPort);
    }

    @Bean
    ListProductCosifsUseCase listProductCosifsUseCase(
            ProductQueryPort productQueryPort,
            ProductCosifQueryPort productCosifQueryPort) {
        return new ListProductCosifsUseCase(productQueryPort, productCosifQueryPort);
    }

    @Bean
    FindManualMovementsUseCase findManualMovementsUseCase(ManualMovementQueryPort manualMovementQueryPort) {
        return new FindManualMovementsUseCase(manualMovementQueryPort);
    }
}

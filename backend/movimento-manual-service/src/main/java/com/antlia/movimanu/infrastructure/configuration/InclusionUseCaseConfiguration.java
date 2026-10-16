package com.antlia.movimanu.infrastructure.configuration;

import java.time.Clock;

import com.antlia.movimanu.application.port.ManualMovementInclusionPort;
import com.antlia.movimanu.application.port.ManualMovementQueryPort;
import com.antlia.movimanu.application.port.ProductCosifQueryPort;
import com.antlia.movimanu.application.port.TransactionExecutionPort;
import com.antlia.movimanu.application.usecase.IncludeManualMovementUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InclusionUseCaseConfiguration {
    @Bean
    Clock movementClock() {
        return Clock.systemUTC();
    }

    @Bean
    IncludeManualMovementUseCase includeManualMovementUseCase(
            ManualMovementInclusionPort manualMovementInclusionPort,
            ManualMovementQueryPort manualMovementQueryPort,
            ProductCosifQueryPort productCosifQueryPort,
            TransactionExecutionPort transactionExecutionPort,
            Clock movementClock) {
        return new IncludeManualMovementUseCase(
                manualMovementInclusionPort,
                manualMovementQueryPort,
                productCosifQueryPort,
                transactionExecutionPort,
                movementClock);
    }
}

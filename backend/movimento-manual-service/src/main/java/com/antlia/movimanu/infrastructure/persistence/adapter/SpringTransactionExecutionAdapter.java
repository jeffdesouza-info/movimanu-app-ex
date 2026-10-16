package com.antlia.movimanu.infrastructure.persistence.adapter;

import java.util.Objects;
import java.util.function.Supplier;

import com.antlia.movimanu.application.port.TransactionExecutionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class SpringTransactionExecutionAdapter implements TransactionExecutionPort {
    private final TransactionTemplate transactionTemplate;

    public SpringTransactionExecutionAdapter(PlatformTransactionManager transactionManager) {
        transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public <T> T executeInNewTransaction(Supplier<T> operation) {
        Objects.requireNonNull(operation, "A operação transacional é obrigatória.");
        return transactionTemplate.execute(status -> operation.get());
    }
}

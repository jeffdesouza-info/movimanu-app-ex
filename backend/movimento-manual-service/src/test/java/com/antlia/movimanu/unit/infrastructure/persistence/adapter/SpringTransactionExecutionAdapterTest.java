package com.antlia.movimanu.unit.infrastructure.persistence.adapter;

import com.antlia.movimanu.infrastructure.persistence.adapter.SpringTransactionExecutionAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;

class SpringTransactionExecutionAdapterTest {
    @Test
    void requiresAnOperation() {
        SpringTransactionExecutionAdapter adapter = new SpringTransactionExecutionAdapter(
                mock(PlatformTransactionManager.class));

        assertThatNullPointerException()
                .isThrownBy(() -> adapter.executeInNewTransaction(null))
                .withMessage("A operação transacional é obrigatória.");
    }
}

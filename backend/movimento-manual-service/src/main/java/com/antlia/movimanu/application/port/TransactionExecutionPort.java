package com.antlia.movimanu.application.port;

import java.util.function.Supplier;

public interface TransactionExecutionPort {
    <T> T executeInNewTransaction(Supplier<T> operation);
}

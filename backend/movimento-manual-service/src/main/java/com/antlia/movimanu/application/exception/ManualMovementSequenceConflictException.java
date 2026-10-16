package com.antlia.movimanu.application.exception;

public class ManualMovementSequenceConflictException extends RuntimeException {
    public ManualMovementSequenceConflictException() {
        super("Não foi possível gerar um número de lançamento disponível.");
    }

    public ManualMovementSequenceConflictException(Throwable cause) {
        super("Não foi possível gerar um número de lançamento disponível.", cause);
    }
}

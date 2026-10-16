package com.antlia.movimanu.application.exception;

public class ManualMovementSequenceCollisionException extends RuntimeException {
    public ManualMovementSequenceCollisionException(Throwable cause) {
        super("Houve colisão ao gerar o número do lançamento.", cause);
    }
}

package com.antlia.movimanu.interfaces.rest.exception;

public class ApiValidationException extends RuntimeException {
    private final String field;

    public ApiValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}

package com.yamiapp.exception;

public class IntegrityViolationException extends RuntimeException {
    public IntegrityViolationException(String message) {
        super(message);
    }
}

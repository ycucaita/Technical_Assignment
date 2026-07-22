package com.example.similarproducts.domain.exception;

/**
 * Raised when an upstream endpoint fails in a transient way (5xx / connection error).
 * This IS counted by the circuit breaker and IS eligible for retry.
 */
public class UpstreamServerException extends RuntimeException {

    public UpstreamServerException(String message) {
        super(message);
    }

    public UpstreamServerException(String message, Throwable cause) {
        super(message, cause);
    }
}

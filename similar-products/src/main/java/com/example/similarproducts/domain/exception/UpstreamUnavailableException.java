package com.example.similarproducts.domain.exception;

/**
 * Raised when a mandatory upstream call cannot be served at all (circuit open, timeout,
 * bulkhead full) and there is no degraded value to fall back to. Maps to HTTP 503.
 */
public class UpstreamUnavailableException extends RuntimeException {

    public UpstreamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

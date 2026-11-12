package com.bookngo.bookingservice.exception;

/** Thrown when a downstream service (e.g. catalog-service) cannot be reached. */
public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.lypzis.lead_worker.exception;

public class NonRetryableProcessingException extends RuntimeException {

    public NonRetryableProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

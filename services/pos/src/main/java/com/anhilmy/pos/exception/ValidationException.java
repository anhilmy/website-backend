package com.anhilmy.pos.exception;

/** Thrown when request input fails validation. Maps to HTTP 400. */
public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super(400, "VALIDATION_ERROR", message);
    }
}

package com.anhilmy.pos.exception;

/** Thrown when an authenticated user lacks permission. Maps to HTTP 403. */
public class AuthorizationException extends ApiException {

    public AuthorizationException(String message) {
        super(403, "FORBIDDEN", message);
    }
}

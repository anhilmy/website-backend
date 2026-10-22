package com.anhilmy.pos.exception;

/** Thrown when credentials are missing or invalid. Maps to HTTP 401. */
public class AuthenticationException extends ApiException {

    public AuthenticationException(String message) {
        super(401, "AUTHENTICATION_FAILED", message);
    }
}

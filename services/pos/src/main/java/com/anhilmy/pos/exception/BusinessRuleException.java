package com.anhilmy.pos.exception;

/** Thrown when a domain/business rule is violated. Maps to HTTP 409. */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(409, "BUSINESS_RULE_VIOLATION", message);
    }
}

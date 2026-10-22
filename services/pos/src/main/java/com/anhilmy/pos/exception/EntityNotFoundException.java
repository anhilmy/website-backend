package com.anhilmy.pos.exception;

/** Thrown when a requested entity does not exist. Maps to HTTP 404. */
public class EntityNotFoundException extends ApiException {

    public EntityNotFoundException(String entity, Object id) {
        super(404, "NOT_FOUND", entity + " with id " + id + " was not found");
    }

    public EntityNotFoundException(String message) {
        super(404, "NOT_FOUND", message);
    }
}

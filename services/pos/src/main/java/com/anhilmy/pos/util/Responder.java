package com.anhilmy.pos.util;

import com.anhilmy.pos.dto.ApiResponse;
import com.anhilmy.pos.exception.ValidationException;
import com.google.gson.JsonSyntaxException;

import spark.Request;
import spark.Response;

/** Helpers for building JSON responses and parsing request bodies. */
public final class Responder {

    private Responder() {
    }

    public static String ok(Response res, Object data) {
        res.type("application/json");
        res.status(200);
        return Json.gson().toJson(ApiResponse.ok(data));
    }

    public static String ok(Response res, Object data, String message) {
        res.type("application/json");
        res.status(200);
        return Json.gson().toJson(ApiResponse.ok(data, message));
    }

    public static String created(Response res, Object data) {
        res.type("application/json");
        res.status(201);
        return Json.gson().toJson(ApiResponse.ok(data));
    }

    public static String noContent(Response res) {
        res.status(204);
        return "";
    }

    public static String error(Response res, int status, String code, String message) {
        res.type("application/json");
        res.status(status);
        return Json.gson().toJson(ApiResponse.error(code, message));
    }

    /** Parses a JSON body into {@code type}, failing with a 400 on malformed input. */
    public static <T> T parse(Request req, Class<T> type) {
        String body = req.body();
        if (body == null || body.isBlank()) {
            throw new ValidationException("Request body is required");
        }
        try {
            T parsed = Json.gson().fromJson(body, type);
            if (parsed == null) {
                throw new ValidationException("Request body is required");
            }
            return parsed;
        } catch (JsonSyntaxException e) {
            throw new ValidationException("Malformed JSON request body");
        }
    }
}

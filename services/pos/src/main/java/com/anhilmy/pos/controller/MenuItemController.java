package com.anhilmy.pos.controller;

import java.math.BigDecimal;

import com.anhilmy.pos.dto.MenuItemRequest;
import com.anhilmy.pos.exception.ValidationException;
import com.anhilmy.pos.middleware.AuthenticationFilter;
import com.anhilmy.pos.model.Roles;
import com.anhilmy.pos.service.MenuItemService;
import com.anhilmy.pos.util.InputValidator;
import com.anhilmy.pos.util.Responder;
import com.anhilmy.pos.util.Routes;

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

/** Menu item read for all authenticated users; mutations are manager-only. */
public class MenuItemController {

    private static final MenuItemService service = new MenuItemService();

    public static void registerRoutes() {
        get("/items", Routes.wrap((req, res) -> {
            String search = req.queryParams("search");
            Long categoryId = parseLongOrNull(req.queryParams("categoryId"), "categoryId");
            Boolean available = parseBooleanOrNull(req.queryParams("available"), "available");
            BigDecimal minPrice = parseDecimalOrNull(req.queryParams("minPrice"), "minPrice");
            BigDecimal maxPrice = parseDecimalOrNull(req.queryParams("maxPrice"), "maxPrice");
            return Responder.ok(res,
                    service.list(search, categoryId, available, minPrice, maxPrice));
        }));

        get("/items/:id", Routes.wrap((req, res) -> {
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            return Responder.ok(res, service.getById(id));
        }));

        post("/items", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            return Responder.created(res,
                    service.create(Responder.parse(req, MenuItemRequest.class)));
        }));

        put("/items/:id", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            MenuItemRequest body = Responder.parse(req, MenuItemRequest.class);
            return Responder.ok(res, service.update(id, body));
        }));

        // Soft delete
        delete("/items/:id", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            service.deactivate(id);
            return Responder.noContent(res);
        }));
    }

    private static Long parseLongOrNull(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a number");
        }
    }

    private static BigDecimal parseDecimalOrNull(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a number");
        }
    }

    private static Boolean parseBooleanOrNull(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        if (!raw.equalsIgnoreCase("true") && !raw.equalsIgnoreCase("false")) {
            throw new ValidationException(field + " must be true or false");
        }
        return Boolean.valueOf(raw);
    }
}

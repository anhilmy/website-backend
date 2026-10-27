package com.anhilmy.pos.controller;

import com.anhilmy.pos.dto.PaymentMethodRequest;
import com.anhilmy.pos.exception.ValidationException;
import com.anhilmy.pos.middleware.AuthenticationFilter;
import com.anhilmy.pos.model.Roles;
import com.anhilmy.pos.service.PaymentMethodService;
import com.anhilmy.pos.util.InputValidator;
import com.anhilmy.pos.util.Responder;
import com.anhilmy.pos.util.Routes;

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

/** Payment methods: public read for the till, manager-only mutations. */
public class PaymentMethodController {

    private static final PaymentMethodService service = new PaymentMethodService();

    public static void registerRoutes() {
        get("/payment-methods", Routes.wrap((req, res) -> Responder.ok(res,
                service.list(parseBooleanOrNull(req.queryParams("active"), "active")))));

        post("/payment-methods", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            return Responder.created(res,
                    service.create(Responder.parse(req, PaymentMethodRequest.class)));
        }));

        put("/payment-methods/:id", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            return Responder.ok(res,
                    service.update(id, Responder.parse(req, PaymentMethodRequest.class)));
        }));

        // Soft delete
        delete("/payment-methods/:id", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            service.deactivate(InputValidator.parseLongId(req.params(":id"), "id"));
            return Responder.noContent(res);
        }));
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

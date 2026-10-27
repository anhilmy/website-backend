package com.anhilmy.pos.controller;

import com.anhilmy.pos.dto.ModifierGroupRequest;
import com.anhilmy.pos.dto.ModifierOptionRequest;
import com.anhilmy.pos.middleware.AuthenticationFilter;
import com.anhilmy.pos.model.Roles;
import com.anhilmy.pos.service.ModifierService;
import com.anhilmy.pos.util.InputValidator;
import com.anhilmy.pos.util.Responder;
import com.anhilmy.pos.util.Routes;

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

/** Manager-only CRUD for item modifier groups and options. */
public class ModifierController {

    private static final ModifierService service = new ModifierService();

    public static void registerRoutes() {
        get("/modifier-groups", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            Long menuItemId = parseMenuItemId(req.queryParams("menuItemId"));
            return Responder.ok(res, service.listForManager(menuItemId));
        }));

        post("/modifier-groups", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            return Responder.created(res,
                    service.createGroup(Responder.parse(req, ModifierGroupRequest.class)));
        }));

        put("/modifier-groups/:id", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            return Responder.ok(res,
                    service.updateGroup(id, Responder.parse(req, ModifierGroupRequest.class)));
        }));

        delete("/modifier-groups/:id", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            service.deactivateGroup(InputValidator.parseLongId(req.params(":id"), "id"));
            return Responder.noContent(res);
        }));

        post("/modifier-groups/:groupId/options", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            Long groupId = InputValidator.parseLongId(req.params(":groupId"), "groupId");
            return Responder.created(res, service.createOption(groupId,
                    Responder.parse(req, ModifierOptionRequest.class)));
        }));

        put("/modifier-options/:id", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            return Responder.ok(res,
                    service.updateOption(id, Responder.parse(req, ModifierOptionRequest.class)));
        }));

        delete("/modifier-options/:id", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            service.deactivateOption(InputValidator.parseLongId(req.params(":id"), "id"));
            return Responder.noContent(res);
        }));
    }

    private static Long parseMenuItemId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return InputValidator.parseLongId(raw, "menuItemId");
    }
}

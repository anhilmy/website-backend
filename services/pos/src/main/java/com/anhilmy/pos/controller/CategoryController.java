package com.anhilmy.pos.controller;

import com.anhilmy.pos.dto.CategoryRequest;
import com.anhilmy.pos.middleware.AuthenticationFilter;
import com.anhilmy.pos.model.Roles;
import com.anhilmy.pos.service.MenuCategoryService;
import com.anhilmy.pos.util.InputValidator;
import com.anhilmy.pos.util.Responder;
import com.anhilmy.pos.util.Routes;

import static spark.Spark.delete;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

/** Menu category read for all authenticated users; mutations are manager-only. */
public class CategoryController {

    private static final MenuCategoryService service = new MenuCategoryService();

    public static void registerRoutes() {
        get("/categories", Routes.wrap((req, res) -> {
            String activeParam = req.queryParams("active");
            Boolean activeOnly = activeParam == null ? null : Boolean.valueOf(activeParam);
            return Responder.ok(res, service.list(activeOnly));
        }));

        get("/categories/:id", Routes.wrap((req, res) -> {
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            return Responder.ok(res, service.getById(id));
        }));

        post("/categories", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            return Responder.created(res,
                    service.create(Responder.parse(req, CategoryRequest.class)));
        }));

        put("/categories/:id", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            CategoryRequest body = Responder.parse(req, CategoryRequest.class);
            return Responder.ok(res, service.update(id, body));
        }));

        // Soft delete
        delete("/categories/:id", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            service.deactivate(id);
            return Responder.noContent(res);
        }));
    }
}

package com.anhilmy.pos.controller;

import com.anhilmy.pos.exception.ValidationException;
import com.anhilmy.pos.service.MenuCategoryService;
import com.anhilmy.pos.service.MenuItemService;
import com.anhilmy.pos.util.InputValidator;
import com.anhilmy.pos.util.Responder;
import com.anhilmy.pos.util.Routes;

import static spark.Spark.get;

/** Public, unauthenticated menu browsing. */
public class MenuController {

    private static final MenuCategoryService categoryService = new MenuCategoryService();
    private static final MenuItemService itemService = new MenuItemService();

    public static void registerRoutes() {
        get("/menu/categories", Routes.wrap((req, res) ->
                Responder.ok(res, categoryService.list(true))));

        // Public listing only ever returns available items; supports category + search filters.
        get("/menu/items", Routes.wrap((req, res) -> {
            String search = req.queryParams("search");
            String categoryParam = req.queryParams("categoryId");
            Long categoryId = null;
            if (categoryParam != null && !categoryParam.isBlank()) {
                try {
                    categoryId = Long.valueOf(categoryParam);
                } catch (NumberFormatException e) {
                    throw new ValidationException("categoryId must be a number");
                }
            }
            return Responder.ok(res,
                    itemService.list(search, categoryId, Boolean.TRUE, null, null));
        }));

        get("/menu/items/:id", Routes.wrap((req, res) -> {
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            return Responder.ok(res, itemService.getById(id));
        }));
    }
}

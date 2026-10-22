package com.anhilmy.pos.controller;

import com.anhilmy.pos.dto.ChangePasswordRequest;
import com.anhilmy.pos.dto.UserCreateRequest;
import com.anhilmy.pos.exception.AuthorizationException;
import com.anhilmy.pos.middleware.AuthenticationFilter;
import com.anhilmy.pos.model.Roles;
import com.anhilmy.pos.model.User;
import com.anhilmy.pos.service.UserService;
import com.anhilmy.pos.util.InputValidator;
import com.anhilmy.pos.util.Responder;
import com.anhilmy.pos.util.Routes;

import spark.Request;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.put;

/** User administration: reads/creates are manager-only; password change is self-service. */
public class UserController {

    private static final UserService userService = new UserService();

    public static void registerRoutes() {
        get("/users", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            return Responder.ok(res, userService.getAllUsers());
        }));

        get("/users/:id", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            return Responder.ok(res, userService.getUserById(id));
        }));

        post("/users", Routes.wrap((req, res) -> {
            AuthenticationFilter.requireRole(req, Roles.MANAGER);
            UserCreateRequest body = Responder.parse(req, UserCreateRequest.class);
            return Responder.created(res,
                    userService.createUser(body.username, body.password, body.role));
        }));

        // Change own password (managers may change anyone's).
        put("/users/:id/password", Routes.wrap((req, res) -> {
            Long id = InputValidator.parseLongId(req.params(":id"), "id");
            requireSelfOrManager(req, id);
            User user = userService.getUserById(id);
            ChangePasswordRequest body = Responder.parse(req, ChangePasswordRequest.class);
            userService.changePassword(user, body.oldPassword, body.newPassword);
            return Responder.ok(res, null, "Password updated");
        }));
    }

    private static void requireSelfOrManager(Request req, Long targetId) {
        if (Roles.MANAGER.equals(AuthenticationFilter.role(req))) {
            return;
        }
        Long uid = AuthenticationFilter.userId(req);
        if (uid == null || !uid.equals(targetId)) {
            throw new AuthorizationException("You may only change your own password");
        }
    }
}

package com.anhilmy.pos.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import com.anhilmy.pos.dto.LoginRequest;
import com.anhilmy.pos.logging.LoggingManager;
import com.anhilmy.pos.middleware.AuthenticationFilter;
import com.anhilmy.pos.service.AuthService;
import com.anhilmy.pos.util.Responder;
import com.anhilmy.pos.util.Routes;

import static spark.Spark.get;
import static spark.Spark.post;

/** Authentication endpoints: login, logout and current-user lookup. */
public class AuthController {

    private static final AuthService authService = new AuthService();

    public static void registerRoutes() {
        post("/auth/login", Routes.wrap((req, res) -> {
            LoginRequest body = Responder.parse(req, LoginRequest.class);
            var response = authService.login(body.username, body.password);
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("username", response.getUsername());
            fields.put("role", response.getRole());
            LoggingManager.audit("auth.login", fields);
            return Responder.ok(res, response);
        }));

        post("/auth/logout", Routes.wrap((req, res) -> {
            authService.logout(AuthenticationFilter.token(req));
            Map<String, Object> fields = new LinkedHashMap<>();
            fields.put("userId", AuthenticationFilter.userId(req));
            LoggingManager.audit("auth.logout", fields);
            return Responder.ok(res, null, "Logged out");
        }));

        get("/auth/me", Routes.wrap((req, res) ->
                Responder.ok(res, authService.currentUser(AuthenticationFilter.userId(req)))));
    }
}

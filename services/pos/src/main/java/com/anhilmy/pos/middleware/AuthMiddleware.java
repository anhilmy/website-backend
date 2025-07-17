package com.anhilmy.pos.middleware;

import spark.Request;
import spark.Response;
import static spark.Spark.before;
import static spark.Spark.halt;

public class AuthMiddleware {
    public static void register() {
        before("/users/*", (Request req, Response res) -> {
            String token = req.headers("Authorization");
            if (token == null || !token.equals("Bearer secret-token")) {
                throw halt(401, "Unauthorized");
            }
        });
    }
}
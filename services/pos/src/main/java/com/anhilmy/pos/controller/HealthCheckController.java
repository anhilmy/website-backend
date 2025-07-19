package com.anhilmy.pos.controller;

import com.google.gson.Gson;

import static spark.Spark.get;

public class HealthCheckController {
    static Gson gson = new Gson();

    public static void registerRoutes() {
        get("/healthcheck", (req, res) -> {
            res.type("application/json");
            return gson.toJson("OK");
        });

        
    }
}

package com.anhilmy.pos.controller;

import com.anhilmy.pos.service.UserService;
import com.google.gson.Gson;

import static spark.Spark.get;


public class UserController {
    static UserService userService = new UserService();
    static Gson gson = new Gson();

    public static void registerRoutes() {
        get("/users", (req, res) -> {
            res.type("application/json");
            return gson.toJson(userService.getAllUsers());
        });

        get("/users/:id", (req, res) -> {
            int id = Integer.parseInt(req.params(":id"));
            res.type("application/json");
            return gson.toJson(userService.getUserById(id));
        });
    }
}

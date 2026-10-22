package com.anhilmy.pos;

import static spark.Spark.*;
import com.anhilmy.pos.config.ConfigManager;

public class Main {
    public static void main(String[] args) {
        // Use ConfigManager for environment-based configuration
        int portNum = ConfigManager.getInt("server.port", 4567);
        port(portNum);
        before((req, res) -> res.type("application/json"));
        get("/health", (req, res) -> "{\"status\":\"ok\"}");
        // TODO: Add routes, middleware, and controllers
        awaitInitialization();
        System.out.println("SparkJava POS server started on port " + port());
    }
}

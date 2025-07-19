// App.java
package com.anhilmy.pos;

import com.anhilmy.pos.controller.HealthCheckController;
import com.anhilmy.pos.controller.UserController;

import static spark.Spark.path;
import static spark.Spark.port;

public class App {
    public static void main(String[] args) {
        port(4567);

        path("api/v1", () -> {
            HealthCheckController.registerRoutes();
            UserController.registerRoutes();
        });
    }
}

// App.java
package com.anhilmy.pos;


import org.hibernate.Session;

import com.anhilmy.pos.controller.HealthCheckController;
import com.anhilmy.pos.controller.UserController;
import com.anhilmy.pos.db.DbManager;

import static spark.Spark.after;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.path;
import static spark.Spark.port;

public class App {
    public static void main(String[] args) {
        port(4567);

        before((request, response) -> {
            DbManager.openSession();
        });

        // Close session and commit after request
        after((request, response) -> {
            DbManager.closeSession();
        });

        exception(Exception.class, (e, req, res) -> {
            Session session = DbManager.getSession();
            if (session != null && session.getTransaction().isActive()) {
                session.getTransaction().rollback();
            }
            res.status(500);
            res.body("Internal Server Error: " + e.getMessage());
        });

        path("/api/v1", () -> {
            HealthCheckController.registerRoutes();
            UserController.registerRoutes();
        });
    }
}

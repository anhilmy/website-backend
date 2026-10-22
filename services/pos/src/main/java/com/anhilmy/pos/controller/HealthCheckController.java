package com.anhilmy.pos.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.SessionFactory;

import com.anhilmy.pos.db.DbManager;
import com.anhilmy.pos.util.HibernateUtil;
import com.anhilmy.pos.util.Responder;

import static spark.Spark.get;

/** Liveness/readiness endpoints. */
public class HealthCheckController {

    public static void registerRoutes() {
        get("/healthcheck", (req, res) -> Responder.ok(res, "OK"));

        get("/healthcheck/ready", (req, res) -> {
            Map<String, Object> details = new LinkedHashMap<>();
            boolean dbUp = checkDatabase();
            details.put("database", dbUp ? "UP" : "DOWN");
            res.status(dbUp ? 200 : 503);
            return Responder.ok(res, details);
        });
    }

    private static boolean checkDatabase() {
        try {
            SessionFactory factory = HibernateUtil.getSessionFactory();
            // A session bound to this request already exists (opened by the before filter).
            if (DbManager.hasSession()) {
                DbManager.getSession().createNativeQuery("select 1").uniqueResult();
            } else {
                try (var session = factory.openSession()) {
                    session.createNativeQuery("select 1").uniqueResult();
                }
            }
            return true;
        } catch (Throwable e) {
            // Includes Errors such as NoClassDefFoundError from a failed SessionFactory init.
            return false;
        }
    }
}

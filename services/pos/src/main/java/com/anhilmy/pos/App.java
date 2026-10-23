package com.anhilmy.pos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anhilmy.pos.config.ConfigManager;
import com.anhilmy.pos.controller.AuthController;
import com.anhilmy.pos.controller.CategoryController;
import com.anhilmy.pos.controller.DiningTableController;
import com.anhilmy.pos.controller.DocsController;
import com.anhilmy.pos.controller.HealthCheckController;
import com.anhilmy.pos.controller.MenuController;
import com.anhilmy.pos.controller.MenuItemController;
import com.anhilmy.pos.controller.OrderController;
import com.anhilmy.pos.controller.UserController;
import com.anhilmy.pos.db.DataSeeder;
import com.anhilmy.pos.db.DbManager;
import com.anhilmy.pos.dto.ApiResponse;
import com.anhilmy.pos.exception.ApiException;
import com.anhilmy.pos.logging.LoggingManager;
import com.anhilmy.pos.middleware.AuthenticationFilter;
import com.anhilmy.pos.middleware.CorsFilter;
import com.anhilmy.pos.middleware.RateLimitFilter;
import com.anhilmy.pos.middleware.RequestResponseLogger;
import com.anhilmy.pos.middleware.SecurityHeadersFilter;
import com.anhilmy.pos.util.HibernateUtil;
import com.anhilmy.pos.util.Json;

import static spark.Spark.after;
import static spark.Spark.awaitInitialization;
import static spark.Spark.before;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.notFound;
import static spark.Spark.options;
import static spark.Spark.path;
import static spark.Spark.port;

/**
 * Application entry point.
 *
 * <p>Wires configuration, seeding, cross-cutting filters (CORS, security headers, request
 * logging, rate limiting, authentication), transaction handling and every controller.
 */
public class App {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        ConfigManager.validate();

        // Fail fast: initialise the SessionFactory now so a bad schema/DB prevents startup
        // instead of surfacing as 500s on the first request.
        try {
            HibernateUtil.getSessionFactory();
        } catch (Throwable t) {
            log.error("Database initialisation failed; refusing to start", t);
            throw t;
        }

        int serverPort = ConfigManager.getInt("server.port", 4567);
        port(serverPort);

        DataSeeder.seed();

        registerFilters();
        registerExceptionHandlers();
        registerRoutes();

        awaitInitialization();
        log.info("POS API started on port {} (profile={}, db={})", serverPort,
                ConfigManager.activeProfile(), ConfigManager.get("db.type"));

        Runtime.getRuntime().addShutdownHook(new Thread(LoggingManager::shutdown));
    }

    private static void registerFilters() {
        // Cross-cutting concerns, applied to every request in registration order.
        before((req, res) -> {
            CorsFilter.apply(req, res);
            SecurityHeadersFilter.apply(req, res);
            RequestResponseLogger.before(req, res);
            RateLimitFilter.checkGeneral(req);
        });

        options("/*", CorsFilter::preflight);

        // Stricter limit on login attempts.
        before("/api/v1/auth/login", (req, res) -> RateLimitFilter.checkLogin(req));

        // One Hibernate session/transaction per API request (skipped for CORS preflight).
        before("/api/v1/*", (req, res) -> {
            if (!"OPTIONS".equals(req.requestMethod())) {
                DbManager.openSession();
            }
        });

        // Authentication (public endpoints are skipped inside the filter).
        before("/api/v1/*", AuthenticationFilter::requireAuth);

        // Response logging + safety net so no session is ever leaked by a non-wrapped route.
        after((req, res) -> {
            RequestResponseLogger.after(req, res);
            if (DbManager.hasSession()) {
                if (res.status() >= 400) {
                    DbManager.rollbackAndClose();
                } else {
                    DbManager.commitAndClose();
                }
            }
        });
    }

    private static void registerExceptionHandlers() {
        exception(ApiException.class, (e, req, res) -> {
            DbManager.rollbackAndClose();
            res.type("application/json");
            res.status(e.getStatus());
            res.body(Json.gson().toJson(ApiResponse.error(e.getCode(), e.getMessage())));
        });

        // Concurrent edits / unique-constraint races surface as conflicts, not 500s.
        exception(org.hibernate.StaleObjectStateException.class,
                (e, req, res) -> conflict(res, "The record was changed by someone else; retry"));
        exception(jakarta.persistence.OptimisticLockException.class,
                (e, req, res) -> conflict(res, "The record was changed by someone else; retry"));
        exception(jakarta.persistence.RollbackException.class,
                (e, req, res) -> conflict(res, "Conflicting update; retry"));
        exception(org.hibernate.exception.ConstraintViolationException.class,
                (e, req, res) -> conflict(res, "That value conflicts with an existing record"));

        exception(Exception.class, (e, req, res) -> {
            DbManager.rollbackAndClose();
            String correlationId = req.attribute(RequestResponseLogger.ATTR_CORRELATION_ID);
            log.error("Unhandled error [correlationId={}]", correlationId, e);
            res.type("application/json");
            res.status(500);
            res.body(Json.gson().toJson(
                    ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred")));
        });

        notFound((req, res) -> {
            res.type("application/json");
            res.status(404);
            return Json.gson().toJson(
                    ApiResponse.error("NOT_FOUND", "Route not found: " + req.pathInfo()));
        });
    }

    private static Object conflict(spark.Response res, String message) {
        DbManager.rollbackAndClose();
        res.type("application/json");
        res.status(409);
        return Json.gson().toJson(ApiResponse.error("CONFLICT", message));
    }

    private static void registerRoutes() {
        // Root-level health alias used by Docker healthchecks.
        get("/health", (req, res) -> {
            res.type("application/json");
            return Json.gson().toJson(ApiResponse.ok("OK"));
        });

        // API documentation (root scope, public).
        DocsController.registerRoutes();

        path("/api/v1", () -> {
            HealthCheckController.registerRoutes();
            AuthController.registerRoutes();
            MenuController.registerRoutes();

            CategoryController.registerRoutes();
            MenuItemController.registerRoutes();
            OrderController.registerRoutes();
            DiningTableController.registerRoutes();
            UserController.registerRoutes();
        });
    }
}

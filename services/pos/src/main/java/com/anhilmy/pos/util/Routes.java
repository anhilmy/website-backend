package com.anhilmy.pos.util;

import org.hibernate.JDBCException;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anhilmy.pos.db.DbManager;
import com.anhilmy.pos.dto.ApiResponse;
import com.anhilmy.pos.exception.ApiException;

import spark.Route;

/**
 * Wraps route handlers with deterministic transaction handling and a JSON error body.
 *
 * <p>The Hibernate session is opened in a {@code before} filter; each wrapped handler commits on
 * success and rolls back on any exception. Errors are rendered here rather than relying on
 * Spark's exception mapper, which can fall through to a misleading 404.
 */
public final class Routes {

    private static final Logger log = LoggerFactory.getLogger(Routes.class);

    private Routes() {
    }

    public static Route wrap(Route handler) {
        return (req, res) -> {
            try {
                Object out = handler.handle(req, res);
                DbManager.commitAndClose();
                return out;
            } catch (Throwable t) {
                DbManager.rollbackAndClose();
                return error(res, t);
            }
        };
    }

    private static Object error(spark.Response res, Throwable t) {
        res.type("application/json");

        if (t instanceof ApiException api) {
            res.status(api.getStatus());
            return Json.gson().toJson(ApiResponse.error(api.getCode(), api.getMessage()));
        }
        if (t instanceof LockAcquisitionException
                || t instanceof jakarta.persistence.PessimisticLockException
                || t instanceof StaleObjectStateException
                || t instanceof jakarta.persistence.OptimisticLockException
                || t instanceof jakarta.persistence.RollbackException
                || t instanceof ConstraintViolationException) {
            res.status(409);
            return Json.gson().toJson(ApiResponse.error("CONFLICT",
                    "The record was changed by someone else; retry the request"));
        }
        if (t instanceof JDBCException) {
            log.error("Database error", t);
            res.status(503);
            return Json.gson().toJson(ApiResponse.error("SERVICE_UNAVAILABLE",
                    "The database is temporarily unavailable; retry"));
        }

        log.error("Unhandled route error", t);
        res.status(500);
        return Json.gson().toJson(ApiResponse.error("INTERNAL_ERROR",
                "An unexpected error occurred"));
    }
}

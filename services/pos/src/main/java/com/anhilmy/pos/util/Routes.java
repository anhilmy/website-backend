package com.anhilmy.pos.util;

import com.anhilmy.pos.db.DbManager;

import spark.Route;

/**
 * Wraps route handlers with deterministic transaction handling.
 *
 * <p>The Hibernate session is opened in a {@code before} filter; each wrapped handler commits on
 * success and rolls back on any exception. This avoids relying on Spark's after-filter ordering
 * relative to exception handlers.
 */
public final class Routes {

    private Routes() {
    }

    public static Route wrap(Route handler) {
        return (req, res) -> {
            try {
                Object out = handler.handle(req, res);
                DbManager.commitAndClose();
                return out;
            } catch (Throwable t) {
                // Catch Throwable (not just Exception) so Errors such as
                // NoClassDefFoundError still release the thread-local session.
                DbManager.rollbackAndClose();
                if (t instanceof RuntimeException re) {
                    throw re;
                }
                if (t instanceof Error error) {
                    throw error;
                }
                throw new RuntimeException(t.getMessage(), t);
            }
        };
    }
}

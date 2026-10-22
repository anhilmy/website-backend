package com.anhilmy.pos.db;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anhilmy.pos.util.HibernateUtil;

/**
 * Manages one Hibernate {@link Session} per request thread.
 *
 * <p>A session (and its transaction) is opened in a Spark {@code before} filter and must be
 * finished by either {@link #commitAndClose()} or {@link #rollbackAndClose()}. Keeping the
 * session thread-local lets repositories stay free of transaction plumbing while the HTTP layer
 * decides commit-vs-rollback based on the response outcome.
 */
public final class DbManager {

    private static final Logger log = LoggerFactory.getLogger(DbManager.class);

    private static final ThreadLocal<Session> CURRENT = new ThreadLocal<>();

    private DbManager() {
    }

    public static void openSession() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        CURRENT.set(session);
    }

    public static Session getSession() {
        Session session = CURRENT.get();
        if (session == null) {
            throw new IllegalStateException(
                    "No Hibernate session bound to this thread. Call openSession() first.");
        }
        return session;
    }

    public static boolean hasSession() {
        return CURRENT.get() != null;
    }

    public static void commitAndClose() {
        Session session = CURRENT.get();
        if (session == null) {
            return;
        }
        try {
            Transaction tx = session.getTransaction();
            if (tx != null && tx.isActive()) {
                tx.commit();
            }
        } catch (RuntimeException e) {
            log.error("Failed to commit transaction, rolling back", e);
            safeRollback(session);
            throw e;
        } finally {
            closeQuietly(session);
            CURRENT.remove();
        }
    }

    public static void rollbackAndClose() {
        Session session = CURRENT.get();
        if (session == null) {
            return;
        }
        safeRollback(session);
        closeQuietly(session);
        CURRENT.remove();
    }

    private static void safeRollback(Session session) {
        try {
            Transaction tx = session.getTransaction();
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        } catch (RuntimeException e) {
            log.error("Failed to roll back transaction", e);
        }
    }

    private static void closeQuietly(Session session) {
        try {
            session.close();
        } catch (RuntimeException e) {
            log.error("Failed to close session", e);
        }
    }
}

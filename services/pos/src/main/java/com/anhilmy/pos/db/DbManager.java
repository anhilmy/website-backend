package com.anhilmy.pos.db;

import org.hibernate.Session;
import org.hibernate.Transaction;

import com.anhilmy.pos.util.HibernateUtil;

public class DbManager {
    private static final ThreadLocal<Session> sessionThreadLocal = new ThreadLocal<>();

    public static void openSession() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        session.beginTransaction();
        sessionThreadLocal.set(session);
    }

    public static Session getSession() {
        return sessionThreadLocal.get();
    }

    public static void closeSession() {
        Session session = sessionThreadLocal.get();
        if (session != null) {
            Transaction tx = session.getTransaction();
            if (tx != null && tx.isActive()) {
                tx.commit(); // or rollback if you want
            }
            session.close();
            sessionThreadLocal.remove();
        }
    }
}

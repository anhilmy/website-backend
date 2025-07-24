package com.anhilmy.pos.repository;

import java.util.List;
import java.util.Optional;

import org.hibernate.Session;

import com.anhilmy.pos.db.DbManager;
import com.anhilmy.pos.model.Transaction;

public class TransactionRepository {

    public Optional<Transaction> findById(int id) {
        Session session = DbManager.getSession();
        return Optional.ofNullable(session.get(Transaction.class, id));
    }

    public List<Transaction> findAll() {
        Session session = DbManager.getSession();
        return session.createQuery("from Transaction", Transaction.class).list();
    }

    public void save(Transaction transaction) {

        Session session = DbManager.getSession();
        session.persist(transaction);
    }

    public void update(Transaction transaction) {
        Session session = DbManager.getSession();
        session.merge(transaction);
    }

    public void delete(Transaction transaction) {
        Session session = DbManager.getSession();
        session.remove(transaction);
    }
}

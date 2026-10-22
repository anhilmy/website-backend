package com.anhilmy.pos.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.query.Query;

import com.anhilmy.pos.model.Transaction;

/** Persistence operations for {@link Transaction} with status/date/cashier filtering. */
public class TransactionRepository extends BaseRepository<Transaction, Long> {

    public TransactionRepository() {
        super(Transaction.class);
    }

    public List<Transaction> findByStatus(Transaction.Status status) {
        return session()
                .createQuery("from Transaction where status = :status order by createdAt desc",
                        Transaction.class)
                .setParameter("status", status)
                .list();
    }

    public List<Transaction> findByCashier(Long cashierId) {
        return session()
                .createQuery("from Transaction where cashier.id = :cid order by createdAt desc",
                        Transaction.class)
                .setParameter("cid", cashierId)
                .list();
    }

    /** Returns transactions with the given statuses, optionally restricted to a date range. */
    public List<Transaction> findFiltered(Transaction.Status status, LocalDateTime from,
            LocalDateTime to) {
        StringBuilder hql = new StringBuilder("from Transaction where 1=1");
        if (status != null) {
            hql.append(" and status = :status");
        }
        if (from != null) {
            hql.append(" and createdAt >= :from");
        }
        if (to != null) {
            hql.append(" and createdAt <= :to");
        }
        hql.append(" order by createdAt desc");

        Query<Transaction> query = session().createQuery(hql.toString(), Transaction.class);
        if (status != null) {
            query.setParameter("status", status);
        }
        if (from != null) {
            query.setParameter("from", from);
        }
        if (to != null) {
            query.setParameter("to", to);
        }
        return query.list();
    }
}

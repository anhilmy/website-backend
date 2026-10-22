package com.anhilmy.pos.repository;

import java.util.List;

import com.anhilmy.pos.model.TransactionItem;

/** Persistence operations for {@link TransactionItem}, grouped by transaction. */
public class TransactionItemRepository extends BaseRepository<TransactionItem, Long> {

    public TransactionItemRepository() {
        super(TransactionItem.class);
    }

    public List<TransactionItem> findByTransaction(Long transactionId) {
        return session()
                .createQuery("from TransactionItem where transaction.id = :tid",
                        TransactionItem.class)
                .setParameter("tid", transactionId)
                .list();
    }
}

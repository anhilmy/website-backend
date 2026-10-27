package com.anhilmy.pos.repository;

import java.util.List;
import java.util.Optional;

import com.anhilmy.pos.model.PaymentMethod;

/** Persistence operations for {@link PaymentMethod}. */
public class PaymentMethodRepository extends BaseRepository<PaymentMethod, Long> {

    public PaymentMethodRepository() {
        super(PaymentMethod.class);
    }

    public Optional<PaymentMethod> findByCode(String code) {
        return session()
                .createQuery("from PaymentMethod where lower(code) = :code", PaymentMethod.class)
                .setParameter("code", code.toLowerCase())
                .uniqueResultOptional();
    }

    public List<PaymentMethod> findByActive(boolean active) {
        return session()
                .createQuery("from PaymentMethod where active = :active "
                        + "order by sortOrder, id", PaymentMethod.class)
                .setParameter("active", active)
                .list();
    }

    public List<PaymentMethod> findAllOrdered() {
        return session()
                .createQuery("from PaymentMethod order by sortOrder, id", PaymentMethod.class)
                .list();
    }
}

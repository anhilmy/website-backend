package com.anhilmy.pos.repository;

import java.util.List;
import java.util.Optional;

import com.anhilmy.pos.model.ChargeTemplate;

/** Persistence operations for {@link ChargeTemplate}. */
public class ChargeTemplateRepository extends BaseRepository<ChargeTemplate, Long> {

    public ChargeTemplateRepository() {
        super(ChargeTemplate.class);
    }

    public Optional<ChargeTemplate> findByCode(String code) {
        return session()
                .createQuery("from ChargeTemplate where lower(code) = :code",
                        ChargeTemplate.class)
                .setParameter("code", code.toLowerCase())
                .uniqueResultOptional();
    }

    public List<ChargeTemplate> findByActive(boolean active) {
        return session()
                .createQuery("from ChargeTemplate where active = :active "
                        + "order by sortOrder, id", ChargeTemplate.class)
                .setParameter("active", active)
                .list();
    }

    public List<ChargeTemplate> findAllOrdered() {
        return session()
                .createQuery("from ChargeTemplate order by sortOrder, id",
                        ChargeTemplate.class)
                .list();
    }
}

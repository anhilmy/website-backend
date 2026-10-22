package com.anhilmy.pos.repository;

import java.math.BigDecimal;
import java.util.List;

import org.hibernate.query.Query;

import com.anhilmy.pos.model.MenuItem;

/** Persistence operations for {@link MenuItem} with filtering and search. */
public class MenuItemRepository extends BaseRepository<MenuItem, Long> {

    public MenuItemRepository() {
        super(MenuItem.class);
    }

    public List<MenuItem> findByCategory(Long categoryId) {
        return session()
                .createQuery("from MenuItem where menuCategory.id = :cid order by name",
                        MenuItem.class)
                .setParameter("cid", categoryId)
                .list();
    }

    public List<MenuItem> findByAvailability(boolean available) {
        return session()
                .createQuery("from MenuItem where available = :available order by name",
                        MenuItem.class)
                .setParameter("available", available)
                .list();
    }

    /**
     * Dynamic search across name, category, availability and price range. Any argument may be
     * {@code null} to skip that filter.
     */
    public List<MenuItem> search(String name, Long categoryId, Boolean available,
            BigDecimal minPrice, BigDecimal maxPrice) {
        StringBuilder hql = new StringBuilder("from MenuItem where 1=1");
        if (name != null && !name.isBlank()) {
            hql.append(" and lower(name) like :name");
        }
        if (categoryId != null) {
            hql.append(" and menuCategory.id = :cid");
        }
        if (available != null) {
            hql.append(" and available = :available");
        }
        if (minPrice != null) {
            hql.append(" and price >= :minPrice");
        }
        if (maxPrice != null) {
            hql.append(" and price <= :maxPrice");
        }
        hql.append(" order by name");

        Query<MenuItem> query = session().createQuery(hql.toString(), MenuItem.class);
        if (name != null && !name.isBlank()) {
            query.setParameter("name", "%" + name.toLowerCase() + "%");
        }
        if (categoryId != null) {
            query.setParameter("cid", categoryId);
        }
        if (available != null) {
            query.setParameter("available", available);
        }
        if (minPrice != null) {
            query.setParameter("minPrice", minPrice);
        }
        if (maxPrice != null) {
            query.setParameter("maxPrice", maxPrice);
        }
        return query.list();
    }
}

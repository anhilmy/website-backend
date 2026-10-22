package com.anhilmy.pos.repository;

import java.util.List;
import java.util.Optional;

import com.anhilmy.pos.model.MenuCategory;

/** Persistence operations for {@link MenuCategory} with active/inactive filtering. */
public class MenuCategoryRepository extends BaseRepository<MenuCategory, Long> {

    public MenuCategoryRepository() {
        super(MenuCategory.class);
    }

    public List<MenuCategory> findByActive(boolean active) {
        return session()
                .createQuery("from MenuCategory where active = :active order by name",
                        MenuCategory.class)
                .setParameter("active", active)
                .list();
    }

    public Optional<MenuCategory> findByName(String name) {
        return session()
                .createQuery("from MenuCategory where name = :name", MenuCategory.class)
                .setParameter("name", name)
                .uniqueResultOptional();
    }
}

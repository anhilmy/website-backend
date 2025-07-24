package com.anhilmy.pos.repository;

import java.util.List;
import java.util.Optional;

import org.hibernate.Session;

import com.anhilmy.pos.db.DbManager;
import com.anhilmy.pos.model.MenuCategory;

public class MenuCategoryRepository {

    public Optional<MenuCategory> findById(int id) {
        Session session = DbManager.getSession();
        return Optional.ofNullable(session.get(MenuCategory.class, id));
    }

    public List<MenuCategory> findAll() {
        Session session = DbManager.getSession();
        return session.createQuery("from MenuCategory", MenuCategory.class).list();
    }

    public void save(MenuCategory menuCategory) {
        Session session = DbManager.getSession();
        session.persist(menuCategory);
    }

    public void update(MenuCategory menuCategory) {
        Session session = DbManager.getSession();
        session.merge(menuCategory);
    }

    public void delete(MenuCategory menuCategory) {
        Session session = DbManager.getSession();
        session.remove(menuCategory);
    }
}

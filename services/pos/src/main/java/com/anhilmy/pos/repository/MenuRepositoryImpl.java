package com.anhilmy.pos.repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import org.hibernate.Session;

import com.anhilmy.pos.db.DbManager;
import com.anhilmy.pos.model.MenuItem;

public class MenuRepositoryImpl implements MenuRepository {

    @Override
    public Optional<MenuItem> findById(Long id) {
        Session session = DbManager.getSession();
        MenuItem menuItem = session.get(MenuItem.class, id);
        return Optional.ofNullable(menuItem);
    }

    @Override
    public List<MenuItem> findAll() {
        Session session = DbManager.getSession();
        return session.createQuery("from MenuItem", MenuItem.class).list();
    }

    @Override
    public void save(MenuItem menuItem) {
        menuItem.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        Session session = DbManager.getSession();
        session.persist(menuItem);
    }

    @Override
    public void update(MenuItem menuItem) {
        Session session = DbManager.getSession();
        session.merge(menuItem);
    }

    @Override
    public void delete(MenuItem menuItem) {
        Session session = DbManager.getSession();
        session.remove(menuItem);
    }

}

package com.anhilmy.pos.repository;

import java.util.List;
import java.util.Optional;

import org.hibernate.Session;

import com.anhilmy.pos.db.DbManager;

/**
 * Shared CRUD implementation for all repositories.
 *
 * <p>Every operation borrows the request-scoped {@link Session} from {@link DbManager}; no
 * repository opens or commits transactions itself.
 *
 * @param <T>  entity type
 * @param <ID> primary key type
 */
public abstract class BaseRepository<T, ID> {

    protected final Class<T> entityClass;

    protected BaseRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected Session session() {
        return DbManager.getSession();
    }

    public Optional<T> findById(ID id) {
        return Optional.ofNullable(session().get(entityClass, id));
    }

    public List<T> findAll() {
        return session()
                .createQuery("from " + entityClass.getSimpleName(), entityClass)
                .list();
    }

    public T save(T entity) {
        session().persist(entity);
        return entity;
    }

    public T update(T entity) {
        return session().merge(entity);
    }

    public void delete(T entity) {
        session().remove(session().contains(entity) ? entity : session().merge(entity));
    }

    public long count() {
        return session()
                .createQuery("select count(*) from " + entityClass.getSimpleName(), Long.class)
                .getSingleResult();
    }
}

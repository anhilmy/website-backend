package com.anhilmy.pos.repository;

import java.util.Optional;

import com.anhilmy.pos.model.User;

/** Persistence operations for {@link User}, including authentication lookups. */
public class UserRepository extends BaseRepository<User, Long> {

    public UserRepository() {
        super(User.class);
    }

    public Optional<User> findByUsername(String username) {
        return session()
                .createQuery("from User where username = :username", User.class)
                .setParameter("username", username)
                .uniqueResultOptional();
    }

    public boolean existsByUsername(String username) {
        Long count = session()
                .createQuery("select count(*) from User where username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult();
        return count != null && count > 0;
    }
}

package com.anhilmy.pos.repository;

import java.util.List;
import java.util.Optional;
import com.anhilmy.pos.model.MenuItem;

public interface MenuRepository {

    Optional<MenuItem> findById(Long id);

    List<MenuItem> findAll();

    void save(MenuItem menuItem);

    void update(MenuItem menuItem);

    void delete(MenuItem menuItem);

}

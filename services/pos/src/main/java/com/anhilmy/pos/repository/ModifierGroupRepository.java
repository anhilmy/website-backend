package com.anhilmy.pos.repository;

import java.util.List;

import com.anhilmy.pos.model.ModifierGroup;

/** Persistence operations for {@link ModifierGroup}. */
public class ModifierGroupRepository extends BaseRepository<ModifierGroup, Long> {

    public ModifierGroupRepository() {
        super(ModifierGroup.class);
    }

    public List<ModifierGroup> findByMenuItemId(Long menuItemId, boolean activeOnly) {
        String hql = "from ModifierGroup where menuItem.id = :id"
                + (activeOnly ? " and active = true" : "")
                + " order by sortOrder, id";
        return session().createQuery(hql, ModifierGroup.class)
                .setParameter("id", menuItemId)
                .list();
    }

    public List<ModifierGroup> findAllOrdered() {
        return session()
                .createQuery("from ModifierGroup order by menuItem.id, sortOrder, id",
                        ModifierGroup.class)
                .list();
    }

    /** Ids of items that currently have at least one active group (bulk hasModifiers). */
    public List<Long> menuItemIdsWithActiveGroups() {
        return session().createQuery(
                "select distinct g.menuItem.id from ModifierGroup g "
                        + "where g.active = true and g.menuItem is not null",
                Long.class).list();
    }

    public boolean hasActiveGroups(Long menuItemId) {
        Long count = session().createQuery(
                "select count(*) from ModifierGroup g "
                        + "where g.active = true and g.menuItem.id = :id",
                Long.class)
                .setParameter("id", menuItemId)
                .getSingleResult();
        return count != null && count > 0;
    }
}

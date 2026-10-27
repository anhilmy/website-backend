package com.anhilmy.pos.repository;

import java.util.List;

import com.anhilmy.pos.model.ModifierOption;

/** Persistence operations for {@link ModifierOption}. */
public class ModifierOptionRepository extends BaseRepository<ModifierOption, Long> {

    public ModifierOptionRepository() {
        super(ModifierOption.class);
    }

    public List<ModifierOption> findByGroupId(Long groupId) {
        return session()
                .createQuery("from ModifierOption where group.id = :gid order by sortOrder, id",
                        ModifierOption.class)
                .setParameter("gid", groupId)
                .list();
    }
}

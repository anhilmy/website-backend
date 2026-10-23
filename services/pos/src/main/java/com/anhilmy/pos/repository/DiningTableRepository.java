package com.anhilmy.pos.repository;

import java.util.List;

import org.hibernate.query.Query;

import com.anhilmy.pos.model.DiningTable;
import com.anhilmy.pos.model.TableStatus;

/** Persistence operations for {@link DiningTable}. */
public class DiningTableRepository extends BaseRepository<DiningTable, Long> {

    public DiningTableRepository() {
        super(DiningTable.class);
    }

    public List<DiningTable> findFiltered(String area, TableStatus status) {
        StringBuilder hql = new StringBuilder("from DiningTable where 1=1");
        if (area != null && !area.isBlank()) {
            hql.append(" and lower(area) = :area");
        }
        if (status != null) {
            hql.append(" and status = :status");
        }
        hql.append(" order by area, name");

        Query<DiningTable> query = session().createQuery(hql.toString(), DiningTable.class);
        if (area != null && !area.isBlank()) {
            query.setParameter("area", area.toLowerCase());
        }
        if (status != null) {
            query.setParameter("status", status);
        }
        return query.list();
    }

    public java.util.Optional<DiningTable> findByName(String name) {
        return session()
                .createQuery("from DiningTable where name = :name", DiningTable.class)
                .setParameter("name", name)
                .uniqueResultOptional();
    }

    /** Tables merged into the given host. */
    public List<DiningTable> findJoinedTo(Long hostId) {
        return session()
                .createQuery("from DiningTable where joinedTo.id = :hostId", DiningTable.class)
                .setParameter("hostId", hostId)
                .list();
    }

    /** The table (if any) currently holding the given bill. */
    public java.util.Optional<DiningTable> findByTransactionId(Long transactionId) {
        return session()
                .createQuery("from DiningTable where currentTransaction.id = :txId", DiningTable.class)
                .setParameter("txId", transactionId)
                .setMaxResults(1)
                .uniqueResultOptional();
    }
}

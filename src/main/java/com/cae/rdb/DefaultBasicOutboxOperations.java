package com.cae.rdb;


import com.cae.rdb.tables.OutboxItem;

import java.util.List;

public abstract class DefaultBasicOutboxOperations<T extends OutboxItem<I>, I> extends DefaultBasicCrudOperations<T, I> {

    protected DefaultBasicOutboxOperations(){
        DefaultBasicCrudOperations.mapGenericTypes(this);
        DefaultBasicCrudOperations.registerEntity(this);
    }

    public List<T> getAvailableBatch(int batchSize) {
        return this.writeOnStandaloneManagerReturning(entityManager -> {
            if (batchSize <= 0) return List.of();
            var selectSql = this.generateSelectForUpdateSkippingLockedItemsCommandInNativeSql();
            var outboxEventIds = entityManager.createNativeQuery(selectSql)
                    .setParameter(1, this.getTtlSeconds())
                    .setParameter(2, batchSize)
                    .getResultList();
            if (outboxEventIds.isEmpty()) return List.of();
            var updateSql = this.generateUpdateItemsToClaimedCommandInNativeSql();
            entityManager.createNativeQuery(updateSql)
                    .setParameter("outboxEventIds", outboxEventIds)
                    .executeUpdate();
            var fetchSql = this.generateFinalFetchCommandInNativeSql();
            @SuppressWarnings("unchecked")
            var events = (List<T>) entityManager.createNativeQuery(fetchSql, this.getEntityType())
                    .setParameter("outboxEventIds", outboxEventIds)
                    .getResultList();
            return events;
        });
    }

    protected abstract int getTtlSeconds();

    protected String generateSelectForUpdateSkippingLockedItemsCommandInNativeSql(){
        return "SELECT outboxEventId FROM "+ this.getEntityName() +" WHERE outboxEventClaimed = 0 OR (outboxEventClaimed = 1 AND outboxEventClaimedAt < (NOW(6) - INTERVAL ? SECOND)) ORDER BY outboxEventInsertedAt LIMIT ? FOR UPDATE SKIP LOCKED";
    }

    protected String generateUpdateItemsToClaimedCommandInNativeSql(){
        return "UPDATE "+ this.getEntityName() +" SET outboxEventClaimed = 1, outboxEventClaimedAt = NOW(6) WHERE outboxEventId IN (:outboxEventIds)";
    }

    protected String generateFinalFetchCommandInNativeSql(){
        return "SELECT * FROM "+ this.getEntityName() +" WHERE outboxEventId IN (:outboxEventIds)";
    }

}

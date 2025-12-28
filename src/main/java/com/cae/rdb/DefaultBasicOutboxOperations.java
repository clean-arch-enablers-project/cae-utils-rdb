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
            var primaryKeys = entityManager.createNativeQuery(selectSql)
                    .setParameter(1, this.getTtlSeconds())
                    .setParameter(2, batchSize)
                    .getResultList();
            if (primaryKeys.isEmpty()) return List.of();
            var updateSql = this.generateUpdateItemsToClaimedCommandInNativeSql();
            entityManager.createNativeQuery(updateSql)
                    .setParameter("primaryKeys", primaryKeys)
                    .executeUpdate();
            var fetchSql = this.generateFinalFetchCommandInNativeSql();
            @SuppressWarnings("unchecked")
            var events = (List<T>) entityManager.createNativeQuery(fetchSql, this.getEntityType())
                    .setParameter("primaryKeys", primaryKeys)
                    .getResultList();
            return events;
        });
    }

    protected abstract int getTtlSeconds();

    protected String generateSelectForUpdateSkippingLockedItemsCommandInNativeSql(){
        return "SELECT primaryKey FROM "+ this.getEntityName() +" WHERE claimed = 0 OR (claimed = 1 AND claimedAt < (NOW(6) - INTERVAL ? SECOND)) ORDER BY insertedAt LIMIT ? FOR UPDATE SKIP LOCKED";
    }

    protected String generateUpdateItemsToClaimedCommandInNativeSql(){
        return "UPDATE "+ this.getEntityName() +" SET claimed = 1, claimedAt = NOW(6) WHERE primaryKey IN (:primaryKeys)";
    }

    protected String generateFinalFetchCommandInNativeSql(){
        return "SELECT * FROM "+ this.getEntityName() +" WHERE primaryKey IN (:primaryKeys)";
    }

}

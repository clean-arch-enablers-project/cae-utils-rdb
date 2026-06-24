package com.cae.rdb;

import com.cae.rdb.tables.OutboxItem;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.hibernate.LockMode;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public abstract class DefaultBasicOutboxOperations<T extends OutboxItem<I>, I>
        extends DefaultBasicCrudOperations<T, I> {

    private static final String OUTBOX_EVENT_ALIAS = "outboxEvent";

    protected DefaultBasicOutboxOperations() {
        DefaultBasicCrudOperations.mapGenericTypes(this);
        DefaultBasicCrudOperations.registerEntity(this);
    }

    public List<T> getAvailableBatch(int batchSize) {
        return this.writeOnStandaloneManagerReturning(entityManager -> {
            if (batchSize <= 0) return List.of();

            String entityName = this.resolveHqlEntityName(entityManager);

            Object claimedValue = this.resolveOutboxEventClaimedValue(entityManager);
            Object notClaimedValue = this.resolveOutboxEventNotClaimedValue(entityManager);

            Instant now = Instant.now();
            Instant claimExpirationCutoff = now.minusSeconds(this.getTtlSeconds());

            TypedQuery<T> jpaSelectQuery = entityManager
                    .createQuery(this.generateSelectAvailableItemsCommandInHql(entityName), this.getEntityType());

            jpaSelectQuery.setParameter("claimedValue", claimedValue);
            jpaSelectQuery.setParameter("notClaimedValue", notClaimedValue);
            jpaSelectQuery.setParameter("claimExpirationCutoff", claimExpirationCutoff);
            jpaSelectQuery.setMaxResults(batchSize);

            @SuppressWarnings("unchecked")
            org.hibernate.query.Query<T> hibernateSelectQuery =
                    jpaSelectQuery.unwrap(org.hibernate.query.Query.class);

            hibernateSelectQuery.setLockMode(OUTBOX_EVENT_ALIAS, LockMode.UPGRADE_SKIPLOCKED);

            List<T> lockedEvents = hibernateSelectQuery.getResultList();

            if (lockedEvents.isEmpty()) return List.of();

            List<I> outboxEventIds = lockedEvents.stream()
                    .map(this::getOutboxEventId)
                    .collect(Collectors.toList());

            entityManager.createQuery(this.generateUpdateItemsToClaimedCommandInHql(entityName))
                    .setParameter("claimedValue", claimedValue)
                    .setParameter("claimedAt", now)
                    .setParameter("outboxEventIds", outboxEventIds)
                    .executeUpdate();

            /*
             * HQL bulk updates bypass already-managed entity instances.
             * We selected the rows before updating them, so those instances may now be stale.
             * Clearing guarantees the final fetch sees the DB-updated claimed flag and claimedAt timestamp.
             */
            entityManager.clear();

            return entityManager
                    .createQuery(this.generateFinalFetchCommandInHql(entityName), this.getEntityType())
                    .setParameter("outboxEventIds", outboxEventIds)
                    .getResultList();
        });
    }

    protected abstract int getTtlSeconds();

    protected I getOutboxEventId(T outboxEvent) {
        return outboxEvent.getOutboxEventId();
    }

    protected String generateSelectAvailableItemsCommandInHql(String entityName) {
        return "SELECT " + OUTBOX_EVENT_ALIAS + " " +
                "FROM " + entityName + " " + OUTBOX_EVENT_ALIAS + " " +
                "WHERE (" +
                "       " + OUTBOX_EVENT_ALIAS + "." + this.getOutboxEventClaimedAttributeName() + " = :notClaimedValue " +
                "       OR (" +
                "              " + OUTBOX_EVENT_ALIAS + "." + this.getOutboxEventClaimedAttributeName() + " = :claimedValue " +
                "              AND " + OUTBOX_EVENT_ALIAS + "." + this.getOutboxEventClaimedAtAttributeName() + " < :claimExpirationCutoff" +
                "          )" +
                "      ) " +
                "ORDER BY " + OUTBOX_EVENT_ALIAS + "." + this.getOutboxEventInsertedAtAttributeName();
    }

    protected String generateUpdateItemsToClaimedCommandInHql(String entityName) {
        return "UPDATE " + entityName + " " + OUTBOX_EVENT_ALIAS + " " +
                "SET " + OUTBOX_EVENT_ALIAS + "." + this.getOutboxEventClaimedAttributeName() + " = :claimedValue, " +
                "    " + OUTBOX_EVENT_ALIAS + "." + this.getOutboxEventClaimedAtAttributeName() + " = :claimedAt " +
                "WHERE " + OUTBOX_EVENT_ALIAS + "." + this.getOutboxEventIdAttributeName() + " IN :outboxEventIds";
    }

    protected String generateFinalFetchCommandInHql(String entityName) {
        return "SELECT " + OUTBOX_EVENT_ALIAS + " " +
                "FROM " + entityName + " " + OUTBOX_EVENT_ALIAS + " " +
                "WHERE " + OUTBOX_EVENT_ALIAS + "." + this.getOutboxEventIdAttributeName() + " IN :outboxEventIds";
    }

    protected String resolveHqlEntityName(EntityManager entityManager) {
        return entityManager
                .getMetamodel()
                .entity(this.getEntityType())
                .getName();
    }

    protected Object resolveOutboxEventClaimedValue(EntityManager entityManager) {
        return this.resolveClaimedAttributeValue(entityManager, true);
    }

    protected Object resolveOutboxEventNotClaimedValue(EntityManager entityManager) {
        return this.resolveClaimedAttributeValue(entityManager, false);
    }

    protected Object resolveClaimedAttributeValue(EntityManager entityManager, boolean claimed) {
        Class<?> claimedAttributeType = entityManager
                .getMetamodel()
                .entity(this.getEntityType())
                .getAttribute(this.getOutboxEventClaimedAttributeName())
                .getJavaType();

        if (claimedAttributeType.equals(Boolean.class) || claimedAttributeType.equals(boolean.class)) {
            return claimed;
        }

        if (claimedAttributeType.equals(Integer.class) || claimedAttributeType.equals(int.class)) {
            return claimed ? 1 : 0;
        }

        if (claimedAttributeType.equals(Long.class) || claimedAttributeType.equals(long.class)) {
            return claimed ? 1L : 0L;
        }

        if (claimedAttributeType.equals(Short.class) || claimedAttributeType.equals(short.class)) {
            return claimed ? Short.valueOf((short) 1) : Short.valueOf((short) 0);
        }

        if (claimedAttributeType.equals(Byte.class) || claimedAttributeType.equals(byte.class)) {
            return claimed ? Byte.valueOf((byte) 1) : Byte.valueOf((byte) 0);
        }

        throw new UnsupportedOperationException(
                "Unsupported outbox claimed attribute type: " + claimedAttributeType.getName()
                        + ". Supported types are Boolean, boolean, Integer, int, Long, long, Short, short, Byte and byte."
        );
    }

    protected String getOutboxEventIdAttributeName() {
        return "outboxEventId";
    }

    protected String getOutboxEventClaimedAttributeName() {
        return "outboxEventClaimed";
    }

    protected String getOutboxEventClaimedAtAttributeName() {
        return "outboxEventClaimedAt";
    }

    protected String getOutboxEventInsertedAtAttributeName() {
        return "outboxEventInsertedAt";
    }
}
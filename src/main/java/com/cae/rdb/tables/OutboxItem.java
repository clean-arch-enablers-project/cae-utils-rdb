package com.cae.rdb.tables;

import java.time.Instant;

public interface OutboxItem<I> extends TableSchema {

    I getOutboxEventId();
    void setOutboxEventId(I primaryKey);
    void setOutboxEventInsertedAt(Instant instant);
    Instant getOutboxEventInsertedAt();
    void setOutboxEventClaimed(Boolean claimed);
    Boolean getOutboxEventClaimed();
    void setOutboxEventClaimedAt(Instant instant);
    Instant getOutboxEventClaimedAt();
    String getOutboxEventPayload();
    void setOutboxEventPayload(String payload);
}
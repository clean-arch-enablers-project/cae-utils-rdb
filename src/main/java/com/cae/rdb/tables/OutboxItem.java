package com.cae.rdb.tables;

import java.time.Instant;

public interface OutboxItem<I> extends TableSchema<I> {

    void setInsertedAt(Instant instant);
    Instant getInsertedAt();
    void setClaimed(Boolean claimed);
    Boolean getClaimed();
    void setClaimedAt(Instant instant);
    Instant getClaimedAt();


}
package com.cae.rdb.tables;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

@MappedSuperclass
@Getter
@Setter
public abstract class UUIDBasedOutboxItem implements OutboxItem<String> {

    public static <T extends UUIDBasedOutboxItem> T createNew(Supplier<T> supplier, String payload){
        var item = supplier.get();
        item.setOutboxEventId(UUID.randomUUID().toString());
        item.setOutboxEventInsertedAt(Instant.now());
        item.setOutboxEventClaimed(false);
        item.setOutboxEventClaimedAt(null);
        item.setOutboxEventPayload(payload);
        return item;
    }

    @Id
    private String outboxEventId;
    private Instant outboxEventInsertedAt;
    private Boolean outboxEventClaimed;
    private Instant outboxEventClaimedAt;
    @Lob
    @Column(length = 1000000)
    private String outboxEventPayload;
}

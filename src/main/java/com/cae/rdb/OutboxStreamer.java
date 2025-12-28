package com.cae.rdb;

public interface OutboxStreamer {

    <T> void stream(T payload);

}

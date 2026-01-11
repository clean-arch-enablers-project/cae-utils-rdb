package com.cae.rdb;

public interface OutboxStreamer {

    void stream(String payload);

}

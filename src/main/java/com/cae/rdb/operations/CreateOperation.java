package com.cae.rdb.operations;

import com.cae.context.ExecutionContext;

import java.util.List;

public interface CreateOperation <E>{

    void createNew(E instance);
    void createNew(E instance, ExecutionContext executionContext);

    void batchCreate(List<E> instances);
    void batchCreate(List<E> instances, ExecutionContext executionContext);
}

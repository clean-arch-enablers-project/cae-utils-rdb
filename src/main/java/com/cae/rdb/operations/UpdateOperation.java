package com.cae.rdb.operations;

import com.cae.context.ExecutionContext;

import java.util.function.Consumer;

public interface UpdateOperation <E, I>{

    E merge(E instance);
    E merge(E instance, ExecutionContext executionContext);
    void patch(I primaryKey, Consumer<E> patchingAction);
    void patch(I primaryKey, Consumer<E> patchingAction, ExecutionContext executionContext);

}

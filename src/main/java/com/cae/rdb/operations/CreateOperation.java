package com.cae.rdb.operations;

import com.cae.context.ExecutionContext;

public interface CreateOperation <E>{

    void createNew(E instance);
    void createNew(E instance, ExecutionContext executionContext);


}

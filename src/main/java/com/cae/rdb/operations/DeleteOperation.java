package com.cae.rdb.operations;

import com.cae.context.ExecutionContext;

public interface DeleteOperation <I>{

    void deleteById(I id);
    void deleteById(I id, ExecutionContext executionContext);

}

package com.cae.rdb.operations;

import com.cae.context.ExecutionContext;

import java.util.List;

public interface DeleteOperation <I>{

    void deleteById(I id);
    void deleteById(I id, ExecutionContext executionContext);

    void batchDelete(List<I> ids);
    void batchDelete(List<I> ids, ExecutionContext executionContext);
}

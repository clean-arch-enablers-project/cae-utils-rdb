package com.cae.rdb.operations;

import com.cae.context.ExecutionContext;

import java.util.List;

public interface RetrieveAllOperation <E>{

    List<E> retrieveAll();
    List<E> retrieveAll(ExecutionContext executionContext);
    List<E> retrieveAll(ExecutionContext executionContext, boolean transactional);

}

package com.cae.rdb.operations;

import com.cae.context.ExecutionContext;

import java.util.Optional;

public interface FindByIdOperation <E, I>{

    Optional<E> findById(I id);
    Optional<E> findById(I id, ExecutionContext executionContext);
    Optional<E> findById(I id, ExecutionContext executionContext, boolean transactional);
    boolean existsById(I id);
    boolean existsById(I id, ExecutionContext executionContext);
    boolean existsById(I id, ExecutionContext executionContext, boolean transactional);

}

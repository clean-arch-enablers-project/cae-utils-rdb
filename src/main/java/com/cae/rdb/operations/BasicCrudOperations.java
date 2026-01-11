package com.cae.rdb.operations;


import com.cae.rdb.tables.TableSchema;

public interface BasicCrudOperations<T extends TableSchema, I> extends CreateOperation<T>,
        RetrieveAllOperation<T>,
        RetrievePaginatedOperation<T>,
        FindByIdOperation<T, I>,
        UpdateOperation<T, I>,
        DeleteOperation<I>{
}

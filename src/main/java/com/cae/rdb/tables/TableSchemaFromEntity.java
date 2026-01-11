package com.cae.rdb.tables;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public abstract class TableSchemaFromEntity<E> implements TableSchema {

    private final E entity;

}

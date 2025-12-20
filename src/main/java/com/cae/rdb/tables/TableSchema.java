package com.cae.rdb.tables;

public interface TableSchema<I>{

    I getPrimaryKey();
    void setPrimaryKey(I primaryKeyValue);

}

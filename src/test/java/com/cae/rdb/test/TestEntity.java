package com.cae.rdb.test;

import com.cae.rdb.tables.TableSchema;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "TestEntity")
@Getter
@Setter
public class TestEntity implements TableSchema {

    @Id
    private Long id;
    private String name;

}

package com.cae.rdb.test;

import com.cae.rdb.DefaultBasicCrudOperations;
import jakarta.persistence.EntityManagerFactory;

public class TestEntityRepository extends DefaultBasicCrudOperations<TestEntity, Long> {

    public TestEntityRepository(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

}

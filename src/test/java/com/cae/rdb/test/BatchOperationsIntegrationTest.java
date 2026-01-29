package com.cae.rdb.test;


import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BatchOperationsIntegrationTest {

    private static EntityManagerFactory entityManagerFactory;
    private TestEntityRepository testEntityRepository;

    @BeforeAll
    static void setupEntityManagerFactory() {
        Properties hibernateProperties = new Properties();
        hibernateProperties.setProperty("hibernate.connection.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        hibernateProperties.setProperty("hibernate.connection.driver_class", "org.h2.Driver");
        hibernateProperties.setProperty("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        hibernateProperties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
        hibernateProperties.setProperty("hibernate.show_sql", "true");
        hibernateProperties.setProperty("hibernate.format_sql", "true");

        Configuration configuration = new Configuration();
        configuration.setProperties(hibernateProperties);
        configuration.addAnnotatedClass(TestEntity.class);

        entityManagerFactory = configuration.buildSessionFactory();
    }

    @AfterAll
    static void closeEntityManagerFactory() {
        if (entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

    @BeforeEach
    void setup() {
        testEntityRepository = new TestEntityRepository(entityManagerFactory);
        clearDatabase();
    }

    @AfterEach
    void tearDown() {
        clearDatabase();
    }

    private void clearDatabase() {
        EntityManager manager = entityManagerFactory.createEntityManager();
        var transaction = manager.getTransaction();
        try {
            transaction.begin();
            manager.createQuery("DELETE FROM TestEntity").executeUpdate();
            transaction.commit();
        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        } finally {
            manager.close();
        }
    }

    @Test
    void shouldBatchDeleteEntitiesByIds() {
        // Given
        List<TestEntity> entitiesToCreate = LongStream.rangeClosed(1, 5)
                .mapToObj(id -> {
                    TestEntity entity = new TestEntity();
                    entity.setId(id);
                    entity.setName("Entity " + id);
                    return entity;
                })
                .collect(Collectors.toList());

        entitiesToCreate.forEach(entity -> testEntityRepository.createNew(entity));

        assertEquals(5, testEntityRepository.retrieveAll().size());

        List<Long> idsToDelete = List.of(2L, 4L);

        // When
        testEntityRepository.batchDelete(idsToDelete);

        // Then
        assertEquals(3, testEntityRepository.retrieveAll().size());
        assertFalse(testEntityRepository.existsById(2L));
        assertFalse(testEntityRepository.existsById(4L));
        assertTrue(testEntityRepository.existsById(1L));
        assertTrue(testEntityRepository.existsById(3L));
        assertTrue(testEntityRepository.existsById(5L));
    }

    @Test
    void shouldHandleBatchDeleteWithEmptyList() {
        // Given
        List<TestEntity> entitiesToCreate = LongStream.rangeClosed(1, 3)
                .mapToObj(id -> {
                    TestEntity entity = new TestEntity();
                    entity.setId(id);
                    entity.setName("Entity " + id);
                    return entity;
                })
                .collect(Collectors.toList());

        entitiesToCreate.forEach(entity -> testEntityRepository.createNew(entity));

        assertEquals(3, testEntityRepository.retrieveAll().size());

        List<Long> emptyIds = List.of();

        // When
        testEntityRepository.batchDelete(emptyIds);

        // Then
        assertEquals(3, testEntityRepository.retrieveAll().size()); // No change expected
        assertTrue(testEntityRepository.existsById(1L));
        assertTrue(testEntityRepository.existsById(2L));
        assertTrue(testEntityRepository.existsById(3L));
    }

    @Test
    void shouldHandleBatchDeleteWithNonExistentIds() {
        // Given
        List<TestEntity> entitiesToCreate = LongStream.rangeClosed(1, 3)
                .mapToObj(id -> {
                    TestEntity entity = new TestEntity();
                    entity.setId(id);
                    entity.setName("Entity " + id);
                    return entity;
                })
                .collect(Collectors.toList());

        entitiesToCreate.forEach(entity -> testEntityRepository.createNew(entity));

        assertEquals(3, testEntityRepository.retrieveAll().size());

        List<Long> idsToDelete = List.of(2L, 99L, 100L); // 99L and 100L do not exist

        // When
        testEntityRepository.batchDelete(idsToDelete);

        // Then
        assertEquals(2, testEntityRepository.retrieveAll().size()); // Only 2L should be deleted
        assertFalse(testEntityRepository.existsById(2L));
        assertTrue(testEntityRepository.existsById(1L));
        assertTrue(testEntityRepository.existsById(3L));
    }

    @Test
    void shouldBatchCreateEntities() {
        // Given
        List<TestEntity> entitiesToCreate = LongStream.rangeClosed(10, 15)
                .mapToObj(id -> {
                    TestEntity entity = new TestEntity();
                    entity.setId(id);
                    entity.setName("Batch Created Entity " + id);
                    return entity;
                })
                .collect(Collectors.toList());

        // When
        testEntityRepository.batchCreate(entitiesToCreate);

        // Then
        assertEquals(6, testEntityRepository.retrieveAll().size()); // 6 entities (10-15 inclusive)
        assertTrue(testEntityRepository.existsById(10L));
        assertTrue(testEntityRepository.existsById(11L));
        assertTrue(testEntityRepository.existsById(12L));
        assertTrue(testEntityRepository.existsById(13L));
        assertTrue(testEntityRepository.existsById(14L));
        assertTrue(testEntityRepository.existsById(15L));
    }

    @Test
    void shouldHandleBatchCreateWithEmptyList() {
        // Given
        assertEquals(0, testEntityRepository.retrieveAll().size());
        List<TestEntity> emptyList = List.of();

        // When
        testEntityRepository.batchCreate(emptyList);

        // Then
        assertEquals(0, testEntityRepository.retrieveAll().size()); // No change expected
    }
}

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

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void shouldBatchMergeUpdatingEntities() {
        // Given: Create initial entities
        List<TestEntity> initialEntities = LongStream.rangeClosed(1, 3)
                .mapToObj(id -> {
                    TestEntity entity = new TestEntity();
                    entity.setId(id);
                    entity.setName("Original Name " + id);
                    return entity;
                })
                .collect(Collectors.toList());
        testEntityRepository.batchCreate(initialEntities);

        // When: Modify entities and merge
        List<TestEntity> updatedEntities = initialEntities.stream()
                .peek(entity -> entity.setName("Updated Name " + entity.getId()))
                .collect(Collectors.toList());
        testEntityRepository.batchMerge(updatedEntities);

        // Then: Verify entities were updated
        assertEquals(3, testEntityRepository.retrieveAll().size());
        TestEntity updatedEntity1 = testEntityRepository.findById(1L).orElseThrow();
        assertEquals("Updated Name 1", updatedEntity1.getName());
        TestEntity updatedEntity3 = testEntityRepository.findById(3L).orElseThrow();
        assertEquals("Updated Name 3", updatedEntity3.getName());
    }

    @Test
    void shouldBatchMergeCreatingAndUpdatingEntities() {
        // Given: Create initial entities
        List<TestEntity> initialEntities = LongStream.rangeClosed(1, 2)
                .mapToObj(id -> {
                    TestEntity entity = new TestEntity();
                    entity.setId(id);
                    entity.setName("Original Name " + id);
                    return entity;
                })
                .collect(Collectors.toList());
        testEntityRepository.batchCreate(initialEntities);

        // When: Prepare a mixed list of new and updated entities
        TestEntity entityToUpdate = testEntityRepository.findById(1L).orElseThrow();
        entityToUpdate.setName("Updated Name 1");

        TestEntity newEntity = new TestEntity();
        newEntity.setId(3L);
        newEntity.setName("New Entity 3");

        List<TestEntity> mixedList = List.of(entityToUpdate, newEntity);
        testEntityRepository.batchMerge(mixedList);

        // Then: Verify state
        assertEquals(3, testEntityRepository.retrieveAll().size());
        TestEntity updatedEntity = testEntityRepository.findById(1L).orElseThrow();
        assertEquals("Updated Name 1", updatedEntity.getName());
        TestEntity stillOriginalEntity = testEntityRepository.findById(2L).orElseThrow();
        assertEquals("Original Name 2", stillOriginalEntity.getName());
        assertTrue(testEntityRepository.existsById(3L));
    }

    @Test
    void shouldHandleBatchMergeWithEmptyList() {
        // Given
        List<TestEntity> initialEntities = LongStream.rangeClosed(1, 3)
                .mapToObj(id -> {
                    TestEntity entity = new TestEntity();
                    entity.setId(id);
                    entity.setName("Original Name " + id);
                    return entity;
                })
                .collect(Collectors.toList());
        testEntityRepository.batchCreate(initialEntities);
        assertEquals(3, testEntityRepository.retrieveAll().size());

        // When
        testEntityRepository.batchMerge(List.of());

        // Then
        assertEquals(3, testEntityRepository.retrieveAll().size()); // No change
    }

    @Test
    void shouldBatchPatchEntities() {
        // Given
        List<TestEntity> entitiesToCreate = LongStream.rangeClosed(1, 3)
                .mapToObj(id -> {
                    TestEntity entity = new TestEntity();
                    entity.setId(id);
                    entity.setName("Original Name " + id);
                    return entity;
                })
                .collect(Collectors.toList());
        testEntityRepository.batchCreate(entitiesToCreate);

        List<Long> idsToPatch = List.of(1L, 3L);
        String newName = "Patched Name";

        // When
        testEntityRepository.batchPatch(idsToPatch, entities ->
                entities.forEach(entity -> entity.setName(newName))
        );

        // Then
        TestEntity patchedEntity1 = testEntityRepository.findById(1L).orElseThrow();
        assertEquals(newName, patchedEntity1.getName());

        TestEntity unpatchedEntity2 = testEntityRepository.findById(2L).orElseThrow();
        assertEquals("Original Name 2", unpatchedEntity2.getName());

        TestEntity patchedEntity3 = testEntityRepository.findById(3L).orElseThrow();
        assertEquals(newName, patchedEntity3.getName());
    }

    @Test
    void shouldHandleBatchPatchWithEmptyList() {
        // Given
        List<TestEntity> initialEntities = LongStream.rangeClosed(1, 3)
                .mapToObj(id -> {
                    TestEntity entity = new TestEntity();
                    entity.setId(id);
                    entity.setName("Original Name " + id);
                    return entity;
                })
                .collect(Collectors.toList());
        testEntityRepository.batchCreate(initialEntities);
        assertEquals(3, testEntityRepository.retrieveAll().size());

        // When
        testEntityRepository.batchPatch(List.of(), entities ->
                entities.forEach(e -> e.setName("This should not happen"))
        );

        // Then
        assertEquals(3, testEntityRepository.retrieveAll().size());
        assertEquals("Original Name 1", testEntityRepository.findById(1L).get().getName());
        assertEquals("Original Name 2", testEntityRepository.findById(2L).get().getName());
        assertEquals("Original Name 3", testEntityRepository.findById(3L).get().getName());
    }

    @Test
    void shouldThrowExceptionForBatchPatchWithNonExistentIds() {
        // Given
        List<TestEntity> entitiesToCreate = LongStream.rangeClosed(1, 2)
                .mapToObj(id -> {
                    TestEntity entity = new TestEntity();
                    entity.setId(id);
                    entity.setName("Original Name " + id);
                    return entity;
                })
                .collect(Collectors.toList());
        testEntityRepository.batchCreate(entitiesToCreate);

        List<Long> idsToPatch = List.of(1L, 99L); // 99L does not exist

        // When & Then
                var exception = assertThrows(com.cae.mapped_exceptions.specifics.InternalMappedException.class, () ->
                        testEntityRepository.batchPatch(idsToPatch, entities ->
                                entities.forEach(e -> e.setName("Patched Name"))
                        )
                );
                assertTrue(exception.getDetails().get().contains("Some instances corresponding to provided IDs were not found"));
    }
}

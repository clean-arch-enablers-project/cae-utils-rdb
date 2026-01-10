package com.cae.rdb;


import com.cae.context.ExecResource;
import com.cae.context.ExecutionContext;
import com.cae.mapped_exceptions.specifics.InternalMappedException;
import com.cae.rdb.operations.BasicCrudOperations;
import com.cae.rdb.queries.Param;
import com.cae.rdb.tables.TableSchema;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import lombok.AccessLevel;
import lombok.Getter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class DefaultBasicCrudOperations<T extends TableSchema<I>, I> implements BasicCrudOperations<T, I> {

    public static final String RESOURCE_KEY = "CAE_RDB_CONNECTION_RESOURCE";

    protected DefaultBasicCrudOperations(){
        DefaultBasicCrudOperations.mapGenericTypes(this);
        DefaultBasicCrudOperations.registerEntity(this);
    }

    @SuppressWarnings("unchecked")
    protected static <I, T extends TableSchema<I>> void mapGenericTypes(DefaultBasicCrudOperations<T,I> instance) {
        try{
            Type superClass = instance.getClass().getGenericSuperclass();
            if (superClass instanceof ParameterizedType) {
                var typeWithGenericParameters = (ParameterizedType) superClass;
                instance.entityType = (Class<T>) typeWithGenericParameters.getActualTypeArguments()[0];
                instance.entityName = Optional.ofNullable(instance.entityType.getAnnotation(Entity.class))
                        .orElseThrow(() -> new InternalMappedException(
                            "Couldn't register " + instance.getClass().getSimpleName(),
                            "Its table entity type had no @Entity annotation on it"
                        ))
                        .name();
                instance.entityName = instance.entityName.isBlank()? instance.entityType.getSimpleName() : instance.entityName;
            }
        } catch (Exception exception){
            throw new InternalMappedException(
                "Something went wrong while trying to get the entity and its ID types of the " + instance.getClass().getSimpleName() + " object ",
                "More details: the entity and/or its ID types were not specified. Also, if your entity class has no default constructor, please provide one.",
                exception
            );
        }
    }

    protected static <A extends TableSchema<I>, I> void registerEntity(DefaultBasicCrudOperations<A, I> instance) {
        EntityClassesProvider.addEntityClass(instance.entityType);
    }

    @Getter(AccessLevel.PROTECTED)
    protected Class<T> entityType;
    @Getter(AccessLevel.PROTECTED)
    protected String entityName;

    protected EntityManagerFactory entityManagerFactory;

    protected EntityManagerFactory getEntityManagerFactoryOrThrow(){
        return Optional.ofNullable(this.entityManagerFactory)
                .orElseThrow(() -> new InternalMappedException(
                    "Problem during attempt to open a new manager",
                    "The manager factory was null. " +
                    "This is probably because the CaeRdbConnectionFactory wasn't instantiated nor executed. " +
                    "You gotta implement the factory mentioned and execute its API, " +
                    "this way every DAO instance of your application gets the instance of the manager factory and new managers can be managed."
                ));
    }

    protected Consumer<EntityManager> getCreatingActionFor(T instance){
        return manager -> manager.persist(instance);
    }

    @Override
    public void createNew(T instance) {
        this.writeOnStandaloneManager(this.getCreatingActionFor(instance));
    }

    @Override
    public void createNew(T instance, ExecutionContext executionContext) {
        this.writeOnSharedManager(this.getCreatingActionFor(instance), executionContext);
    }

    protected Consumer<EntityManager> getDeletingActionFor(I primaryKey){
        return manager -> {
            var instance = manager.find(this.entityType, primaryKey);
            if (instance == null)
                throw new InternalMappedException(
                    "Couldn't delete item of ID " + primaryKey.toString(),
                    "Apparently the item does not exist."
                );
            manager.remove(instance);
        };
    }

    @Override
    public void deleteById(I primaryKey) {
        this.writeOnStandaloneManager(this.getDeletingActionFor(primaryKey));
    }

    @Override
    public void deleteById(I primaryKey, ExecutionContext executionContext) {
        this.writeOnSharedManager(this.getDeletingActionFor(primaryKey), executionContext);
    }

    protected Function<EntityManager, Optional<T>> getFindingByIdActionFor(I primaryKey){
        return manager -> {
            var instance = manager.find(this.entityType, primaryKey);
            return Optional.ofNullable(instance);
        };
    }

    @Override
    public Optional<T> findById(I primaryKey) {
        return this.readOnStandaloneManager(this.getFindingByIdActionFor(primaryKey));
    }

    @Override
    public Optional<T> findById(I primaryKey, ExecutionContext executionContext) {
        return this.readOnSharedManager(this.getFindingByIdActionFor(primaryKey), executionContext, false);
    }

    @Override
    public Optional<T> findById(I primaryKey, ExecutionContext executionContext, boolean transactional) {
        return this.readOnSharedManager(this.getFindingByIdActionFor(primaryKey), executionContext, transactional);
    }

    @Override
    public boolean existsById(I primaryKey){
        return this.findById(primaryKey).isPresent();
    }

    @Override
    public boolean existsById(I primaryKey, ExecutionContext executionContext) {
        return this.findById(primaryKey, executionContext).isPresent();
    }

    @Override
    public boolean existsById(I primaryKey, ExecutionContext executionContext, boolean transactional) {
        return this.findById(primaryKey, executionContext, transactional).isPresent();
    }

    protected Function<EntityManager, List<T>> getRetrievingAllAction(){
        return manager -> {
            var hql = "FROM " + this.entityName;
            return manager.createQuery(hql, this.entityType).getResultList();
        };
    }

    @Override
    public List<T> retrieveAll() {
        return this.readOnStandaloneManager(this.getRetrievingAllAction());
    }

    @Override
    public List<T> retrieveAll(ExecutionContext executionContext) {
        return this.readOnSharedManager(this.getRetrievingAllAction(), executionContext, false);
    }

    @Override
    public List<T> retrieveAll(ExecutionContext executionContext, boolean transactional) {
        return this.readOnSharedManager(this.getRetrievingAllAction(), executionContext, transactional);
    }

    protected Function<EntityManager, Page<T>> getRetrievingPaginatedActionFor(Integer pageNumber, Integer pageSize){
        return manager -> {
            if (pageNumber == null || pageNumber < 1) throw new InternalMappedException(
                "Couldn't paginate on '" + this.entityName + "'",
                "Page number must be non-null and greater than 0"
            );
            if (pageSize == null || pageSize < 1) throw new InternalMappedException(
                "Couldn't paginate on '" + this.entityName + "'",
                "Page size must be non-null and greater than 0"
            );
            var selectHql = "FROM " + this.entityName;
            var paginatedItems = manager.createQuery(selectHql, this.entityType)
                    .setFirstResult((pageNumber - 1) * pageSize)
                    .setMaxResults(pageSize)
                    .getResultList();
            var totalHql = "SELECT COUNT(e) FROM " + this.entityName + " e";
            var total = manager.createQuery(totalHql, Long.class).getSingleResult();
            return new Page<>(pageNumber, pageSize, total, paginatedItems);
        };
    }

    @Override
    public Page<T> retrievePaginated(Integer pageNumber, Integer pageSize) {
        return this.readOnStandaloneManager(this.getRetrievingPaginatedActionFor(pageNumber, pageSize));
    }

    @Override
    public Page<T> retrievePaginated(Integer pageNumber, Integer pageSize, ExecutionContext executionContext) {
        return this.readOnSharedManager(this.getRetrievingPaginatedActionFor(pageNumber, pageSize), executionContext, false);
    }

    @Override
    public Page<T> retrievePaginated(Integer pageNumber, Integer pageSize, ExecutionContext executionContext, boolean transactional) {
        return this.readOnSharedManager(this.getRetrievingPaginatedActionFor(pageNumber, pageSize), executionContext, transactional);
    }

    protected Function<EntityManager, T> getMergingActionFor(T instanceToMerge){
        return entityManager -> entityManager.merge(instanceToMerge);
    }

    @Override
    public T merge(T instanceToMerge) {
        return this.writeOnStandaloneManagerReturning(this.getMergingActionFor(instanceToMerge));
    }

    @Override
    public T merge(T instance, ExecutionContext executionContext) {
        return this.writeOnSharedManagerReturning(this.getMergingActionFor(instance), executionContext);
    }

    protected Consumer<EntityManager> getPatchingActionFor(I primaryKey, Consumer<T> patchingAction){
        return manager -> {
            var instance = Optional.ofNullable(manager.find(this.entityType, primaryKey))
                    .orElseThrow(() -> new InternalMappedException(
                        "Couldn't patch instance of ID '" + primaryKey.toString() + "'",
                        "Apparently it doesn't exist"
                    ));
            patchingAction.accept(instance);
        };
    }

    @Override
    public void patch(I primaryKey, Consumer<T> patchingAction){
        this.writeOnStandaloneManager(this.getPatchingActionFor(primaryKey, patchingAction));
    }

    @Override
    public void patch(I primaryKey, Consumer<T> patchingAction, ExecutionContext executionContext){
        this.writeOnSharedManager(this.getPatchingActionFor(primaryKey, patchingAction), executionContext);
    }

    protected EntityManager initializeEntityManager(){
        return this.getEntityManagerFactoryOrThrow().createEntityManager();
    }

    protected Optional<T> readOnStandaloneManagerReturningOptional(String jpql, List<Param> params){
        Function<EntityManager, T> action = em -> {
            var query = em.createQuery(
                    jpql,
                    this.getEntityType()
            );
            params.forEach(param -> query.setParameter(param.getField(), param.getValue()));
            return query.getResultList().stream().findFirst().orElse(null);
        };
        return Optional.ofNullable(this.readOnStandaloneManager(action));
    }

    protected List<T> readOnStandaloneManagerReturningList(String jpql, List<Param> params){
        Function<EntityManager, List<T>> action = em -> {
            var query = em.createQuery(
                    jpql,
                    this.getEntityType()
            );
            params.forEach(param -> query.setParameter(param.getField(), param.getValue()));
            return query.getResultList();
        };
        return this.readOnStandaloneManager(action);
    }

    protected List<T> readOnStandaloneManagerReturningList(String jpql, List<Param> params, Integer limit){
        Function<EntityManager, List<T>> action = em -> {
            var query = em.createQuery(
                    jpql,
                    this.getEntityType()
            );
            Optional.ofNullable(limit).ifPresent(query::setMaxResults);
            params.forEach(param -> query.setParameter(param.getField(), param.getValue()));
            return query.getResultList();
        };
        return this.readOnStandaloneManager(action);
    }

    protected <O> O readOnStandaloneManager(Function<EntityManager, O> action){
        try (var manager = this.initializeEntityManager()){
            try {
                return action.apply(manager);
            } catch (Exception exception){
                throw new InternalMappedException(
                    "Something went wrong while trying to execute a reading method in " + this.getClass().getSimpleName(),
                    "Caught: " + exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                    exception
                );
            }
        }
    }

    protected void writeOnStandaloneManager(Consumer<EntityManager> action){
        try (var manager = this.initializeEntityManager()){
            EntityTransaction transaction = null;
            try {
                transaction = manager.getTransaction();
                this.safelyAttemptToBegin(transaction);
                action.accept(manager);
                this.safelyCommit(transaction);
            } catch (Exception exception){
                this.safelyRollback(transaction);
                throw new InternalMappedException(
                    "Something went wrong while trying to execute writing method in " + this.getClass().getSimpleName(),
                    "Caught: " + exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                    exception
                );
            }
        }
    }

    protected  <O> O writeOnStandaloneManagerReturning(Function<EntityManager, O> action){
        try (var manager = this.initializeEntityManager()){
            EntityTransaction transaction = null;
            try {
                transaction = manager.getTransaction();
                this.safelyAttemptToBegin(transaction);
                var result = action.apply(manager);
                this.safelyCommit(transaction);
                return result;
            } catch (Exception exception){
                this.safelyRollback(transaction);
                throw new InternalMappedException(
                    "Something went wrong while trying to execute method in " + this.getClass().getSimpleName(),
                    "Caught: " + exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                    exception
                );
            }
        }
    }

    protected void writeOnSharedManager(Consumer<EntityManager> action, ExecutionContext executionContext){
        var entityManager = this.getOrInitializeSharedManagerFor(executionContext);
        this.writeOnSharedManager(action, entityManager);
    }

    protected void writeOnSharedManager(Consumer<EntityManager> action, EntityManager entityManager){
        try {
            this.safelyAttemptToBegin(entityManager.getTransaction());
            action.accept(entityManager);
        } catch (Exception exception){
            throw new InternalMappedException(
                "Something went wrong while trying to execute method in " + this.getClass().getSimpleName(),
                "Caught: " + exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                exception
            );
        }
    }

    protected  <O> O writeOnSharedManagerReturning(Function<EntityManager, O> action, ExecutionContext executionContext) {
        var entityManager = this.getOrInitializeSharedManagerFor(executionContext);
        return this.writeOnSharedManagerReturning(action, entityManager);
    }

    protected  <O> O writeOnSharedManagerReturning(Function<EntityManager, O> action, EntityManager entityManager) {
        try {
            this.safelyAttemptToBegin(entityManager.getTransaction());
            return action.apply(entityManager);
        } catch (Exception exception) {
            throw new InternalMappedException(
                "Something went wrong while trying to execute method in " + this.getClass().getSimpleName(),
                "Caught: " + exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                exception
            );
        }
    }

    protected Optional<T> readOnSharedManagerReturningOptional(String jpql, List<Param> params, ExecutionContext context){
        Function<EntityManager, T> action = em -> {
            var query = em.createQuery(
                jpql,
                this.getEntityType()
            );
            params.forEach(param -> query.setParameter(param.getField(), param.getValue()));
            return query.getResultList().stream().findFirst().orElse(null);
        };
        return Optional.ofNullable(this.readOnSharedManager(action, context, false));
    }

    protected List<T> readOnSharedManagerReturningList(String jpql, List<Param> params, ExecutionContext context){
        Function<EntityManager, List<T>> action = em -> {
            var query = em.createQuery(
                    jpql,
                    this.getEntityType()
            );
            params.forEach(param -> query.setParameter(param.getField(), param.getValue()));
            return query.getResultList();
        };
        return this.readOnSharedManager(action, context, false);
    }

    protected List<T> readOnSharedManagerReturningList(String jpql, List<Param> params, Integer limit, ExecutionContext context){
        Function<EntityManager, List<T>> action = em -> {
            var query = em.createQuery(
                    jpql,
                    this.getEntityType()
            );
            params.forEach(param -> query.setParameter(param.getField(), param.getValue()));
            Optional.ofNullable(limit).ifPresent(query::setMaxResults);
            return query.getResultList();
        };
        return this.readOnSharedManager(action, context, false);
    }

    protected <O> O readOnSharedManager(Function<EntityManager, O> action, ExecutionContext executionContext, boolean transactional){
        var entityManager = this.getOrInitializeSharedManagerFor(executionContext);
        return this.readOnSharedManager(action, entityManager, transactional);
    }

    protected <O> O readOnSharedManager(Function<EntityManager, O> action, EntityManager entityManager, boolean transactional){
        try {
            if (transactional)
                this.safelyAttemptToBegin(entityManager.getTransaction());
            return action.apply(entityManager);
        } catch (Exception exception) {
            throw new InternalMappedException(
                "Something went wrong while trying to execute method in " + this.getClass().getSimpleName(),
                "Caught: " + exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                exception
            );
        }
    }

    protected EntityManager getOrInitializeSharedManagerFor(ExecutionContext executionContext){
        return executionContext.getResource(RESOURCE_KEY, EntityManager.class).orElseGet(() -> {
            var newSharedManager = this.getEntityManagerFactoryOrThrow().createEntityManager();
            var newSharedManagerAsExecResource = ExecResource.of(newSharedManager, this::endSharedManager);
            executionContext.putResource(RESOURCE_KEY, newSharedManagerAsExecResource);
            return newSharedManager;
        });
    }

    private void endSharedManager(ExecutionContext executionContext) {
        try (var sharedManager = executionContext.getResource(RESOURCE_KEY, EntityManager.class)
                .orElseThrow(() -> new InternalMappedException(
                    "Couldn't end shared manager from ExecutionContext instance",
                    "The instance had no EntityManager resource under the key of '" + RESOURCE_KEY + "' available"
                ))){
            if (executionContext.wasSuccessful()) this.safelyCommit(sharedManager.getTransaction());
            else this.safelyRollback(sharedManager.getTransaction());
        }
    }

    protected void safelyAttemptToBegin(EntityTransaction transaction){
        if (transaction == null)
            throw new InternalMappedException(
                "Couldn't start transaction",
                "It was null from the EntityManager"
            );
        if (!transaction.isActive())
            transaction.begin();
    }

    protected void safelyCommit(EntityTransaction transaction) {
        Optional.ofNullable(transaction).ifPresent(trans -> {
            try {if (trans.isActive()) trans.commit();}
            catch (Exception commitFailure) {
                try{
                    this.safelyRollback(trans);
                } catch (Exception rollbackFailure){
                    commitFailure.addSuppressed(rollbackFailure);
                }
                throw new InternalMappedException(
                    "Couldn't commit transaction",
                    "Caught:" + commitFailure.getClass().getSimpleName(),
                    commitFailure
                );
            }
        });
    }

    protected void safelyRollback(EntityTransaction transaction) {
        Optional.ofNullable(transaction).ifPresent(trans -> {
            try {if (trans.isActive()) trans.rollback();}
            catch (Exception exception) {throw new InternalMappedException(
                "Couldn't rollback transaction",
                "Caught: " + exception.getClass().getSimpleName() + ": " + exception.getMessage(),
                exception
            );}
        });
    }


}

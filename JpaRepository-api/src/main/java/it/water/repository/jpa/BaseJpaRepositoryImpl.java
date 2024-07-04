
/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.water.repository.jpa;

import it.water.core.api.entity.owned.OwnedResource;
import it.water.core.api.model.BaseEntity;
import it.water.core.api.model.User;
import it.water.core.api.repository.RepositoryConstraintValidator;
import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.QueryBuilder;
import it.water.core.api.repository.query.QueryOrder;
import it.water.core.api.repository.query.QueryOrderParameter;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.repository.entity.model.PaginatedResult;
import it.water.repository.entity.model.exceptions.EntityNotFound;
import it.water.repository.entity.model.exceptions.NoResultException;
import it.water.repository.jpa.api.JpaRepository;
import it.water.repository.jpa.constraints.DuplicateConstraintValidator;
import it.water.repository.jpa.constraints.RepositoryConstraintValidatorsManager;
import it.water.repository.jpa.query.PredicateBuilder;
import it.water.repository.query.DefaultQueryBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.criteria.*;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * @param <T> parameter that indicates a generic class which must extend Base Entity
 *            Model class for BaseRepositoryImpl.
 *            This class implements all methods for basic CRUD operations defined in
 *            BaseRepository interface. These methods are reusable by all
 *            entities that interact with the platform.
 * @Author Aristide Cittadino.
 */
public abstract class BaseJpaRepositoryImpl<T extends BaseEntity> implements JpaRepository<T> {
    public static final String WATER_DEFAULT_PERSISTENCE_UNIT_NAME = "water-default-persistence-unit";
    @Getter(AccessLevel.PROTECTED)
    private Logger log = LoggerFactory.getLogger(BaseJpaRepositoryImpl.class.getName());

    /**
     * Generic class for Water platform
     */
    protected Class<T> type;

    /**
     * Local Entity Manager may be null if not passed in the constructor.
     * We leave the possibility to override this method which is used internally in this class
     * and let children classes to set how to retrieve it. For Example in OSGi contest it will be retrieved
     * in concrete repository classes using the @PersistenceUnit EntityManagerFactor.createEntityManager
     */
    @Getter(AccessLevel.PUBLIC)
    private EntityManager entityManager;

    /**
     * Global initialized entity managers if they are not provided from the constructor.
     * this map saves each entity manager based on the persistence unit name in order to not create more entity manager
     * than the actually defined persistence unit name
     */
    private static Map<String, EntityManager> globalEntityManagers = new HashMap<>();

    /**
     * Persistence Unit related to the entity manager that must be created for this repository.
     */
    private String persistenceUnitName;

    protected RepositoryConstraintValidatorsManager dbConstraintsValidatorManager;

    /**
     * Generic constructor it will try to load an entity manager finding persistence.xml inside project path using default persistence unit name
     *
     * @param type
     */
    protected BaseJpaRepositoryImpl(Class<T> type) {
        setupJpaRepository(WATER_DEFAULT_PERSISTENCE_UNIT_NAME, type);
        this.initJpaRepository(initDefaultEntityManager(), new DuplicateConstraintValidator());
    }

    /**
     * Generic constructor it will try to load an entity manager finding persistence.xml inside project path using the specified persistence unit name
     *
     * @param type
     */
    protected BaseJpaRepositoryImpl(Class<T> type, String persistenceUnitName) {
        setupJpaRepository(persistenceUnitName, type);
        this.initJpaRepository(initDefaultEntityManager(), new DuplicateConstraintValidator());
    }

    /**
     * Generic constructor to force all parameters
     *
     * @param type
     */
    protected BaseJpaRepositoryImpl(Class<T> type, String persistenceUnitName, EntityManager entityManager) {
        setupJpaRepository(persistenceUnitName, type);
        this.initJpaRepository(entityManager, new DuplicateConstraintValidator());
    }

    /**
     * Constructor for WaterBaseRepositoryImpl
     *
     * @param type parameter that indicates a generic entity
     */
    protected BaseJpaRepositoryImpl(Class<T> type, EntityManager entityManager) {
        setupJpaRepository(WATER_DEFAULT_PERSISTENCE_UNIT_NAME, type);
        this.initJpaRepository(entityManager, new DuplicateConstraintValidator());
    }

    protected BaseJpaRepositoryImpl(Class<T> type, EntityManager entityManager, RepositoryConstraintValidator... dbConstraintValidators) {
        setupJpaRepository(WATER_DEFAULT_PERSISTENCE_UNIT_NAME, type);
        this.initJpaRepository(entityManager, dbConstraintValidators);
    }

    protected void initJpaRepository(EntityManager entityManager, RepositoryConstraintValidator... dbConstraintValidators) {
        this.entityManager = entityManager;
        this.dbConstraintsValidatorManager = new RepositoryConstraintValidatorsManager(dbConstraintValidators);
    }

    private void setupJpaRepository(String persistenceUnitName, Class<T> type) {
        this.type = type;
        this.persistenceUnitName = persistenceUnitName;
    }

    private synchronized EntityManager initDefaultEntityManager() {
        if (!globalEntityManagers.containsKey(this.persistenceUnitName)) {
            try {
                EntityManagerFactory entityManagerFactory = createDefaultEntityManagerFactory();
                globalEntityManagers.put(this.persistenceUnitName, entityManagerFactory.createEntityManager());
            } catch (Exception e) {
                globalEntityManagers.remove(this.persistenceUnitName);
                getLog().warn(e.getMessage(), e);
                return null;
            }
        }
        return globalEntityManagers.get(this.persistenceUnitName);
    }

    protected EntityManagerFactory createDefaultEntityManagerFactory() {
        return Persistence.createEntityManagerFactory(this.persistenceUnitName);
    }

    /**
     * Identifies if the current context supports transaction or not.
     * For example in test environment where no application server is running transactional annotation won't work
     * Used on method with tx Type Required, this means that when we enter in this method there should be an active transaction
     *
     * @return
     */
    private boolean isTransactionalSupported(EntityManager em) {
        //Every eventual exception we have accessing the transaction context means that transaction system is working
        try {
            return em != null && (em.isJoinedToTransaction() || (em.getTransaction() != null && em.getTransaction().isActive()));
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Identifies if the current context supports transaction or not.
     * For example in test environment where no application server is running transactional annotation won't work
     */
    private void startTransactionIfNeeded(EntityManager em) {
        if (!isTransactionalSupported(em))
            em.getTransaction().begin();
    }

    /**
     * Identifies if the current context supports transaction or not.
     * For example in test environment where no application server is running transactional annotation won't work
     */
    private void commitTransactionIfNeeded(EntityManager em) {
        if (!isTransactionalSupported(em))
            em.getTransaction().commit();
    }

    /**
     * Save an entity in database
     * Can be overridden in order to change the logic how to retrieve entity manager
     */
    @Override
    public T persist(T entity) {
        return tx(Transactional.TxType.REQUIRED, em -> doPersist(entity, em));
    }

    /**
     * Persistence logic with a specific entity manager
     *
     * @param entity
     * @param em
     * @return
     */
    protected T doPersist(T entity, EntityManager em) {
        startTransactionIfNeeded(em);
        try {
            log.debug("Repository Saving entity {}: {}", this.type.getSimpleName(), entity);
            this.dbConstraintsValidatorManager.runCheck(entity, this.type, this);
            log.debug("Transaction found, invoke persist");
            em.persist(entity);
            log.debug("Entity persisted: {}", entity);
            commitTransactionIfNeeded(em);
            return entity;
        } catch (RuntimeException e) {
            //only in context where @transactional is not supported
            if (!isTransactionalSupported(em)) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }

    /**
     * Update an entity in database
     * Can be overridden in order to change the logic how to retrieve entity manager
     */
    @Override
    public T update(T entity) {
        return tx(Transactional.TxType.REQUIRED, em -> doUpdate(entity, em));
    }

    /**
     * Persistence logic with a specific entity manager
     *
     * @param entity
     * @param em
     * @return
     */
    protected T doUpdate(T entity, EntityManager em) {
        try {
            startTransactionIfNeeded(em);
            log.debug("Repository Update entity {}: {}", this.type.getSimpleName(), entity);
            this.dbConstraintsValidatorManager.runCheck(entity, this.type, this);
            //Enforcing the concept that the owner cannot be changed
            //TO DO: check if it is useful or not
            T entityFromDb = em.find(type, entity.getId());
            if (entityFromDb instanceof OwnedResource ownedFromDb) {
                User oldOwner = ownedFromDb.getUserOwner();
                OwnedResource owned = (OwnedResource) entity;
                owned.setUserOwner(oldOwner);
            }
            if (entity.getId() > 0) {
                log.debug("Updating entity");
                boolean upgradeVersionManually = !em.contains(entity);
                T updateEntity = em.merge(entity);
                if (upgradeVersionManually) {
                    //incresing manually version since entities can come basically from non managed contexts (like rest with jackson)
                    updateEntity.setEntityVersion(updateEntity.getEntityVersion().intValue() + 1);
                }
                log.debug("Entity merged: {}", entity);
                commitTransactionIfNeeded(em);
                return updateEntity;
            }
        } catch (RuntimeException e) {
            //only in context where @transactional is not supported
            if (!isTransactionalSupported(em)) {
                em.getTransaction().rollback();
            }
            throw e;
        }
        throw new EntityNotFound();
    }

    /**
     * Remove an entity by id
     * Can be overridden in order to change the logic how to retrieve entity manager
     */
    @Override
    public void remove(long id) {
        txExpr(Transactional.TxType.REQUIRED, em -> doRemove(id, em));
    }

    protected void doRemove(long id, EntityManager em) {
        try {
            startTransactionIfNeeded(em);
            log.debug("Repository Remove entity {} with id: {}", this.type.getSimpleName(), id);
            T entity = em.find(type, id);
            em.remove(entity);
            log.debug("Entity {}  with id: {}  removed", this.type.getSimpleName(), id);
            commitTransactionIfNeeded(em);
        } catch (RuntimeException e) {
            //only in context where @transactional is not supported
            if (!isTransactionalSupported(em)) {
                em.getTransaction().rollback();
            }
            throw e;
        }
    }

    /**
     * Can be overridden in order to change the logic how to retrieve entity manager
     *
     * @param entity
     */
    @Override
    public void remove(T entity) {
        log.debug("Repository Remove all entities {}: {}", this.type.getSimpleName(), entity);
        //post actions are preserved
        this.remove(entity.getId());
    }

    @Override
    public void removeAllByIds(Iterable<Long> ids) {
        log.debug("Repository Remove all entities {} by ids {}", this.type.getSimpleName(), ids);
        //post actions are preserved
        ids.forEach(this::remove);
    }

    @Override
    public void removeAll(Iterable<T> entities) {
        log.debug("Repository Remove all entities {}: {}", this.type.getSimpleName(), entities);
        //post actions are preserved
        entities.forEach(entity -> this.remove(entity.getId()));
    }

    @Override
    public void removeAll() {
        log.debug("Repository Remove all entities {}", this.type.getSimpleName());
        Collection<T> entites = this.findAll(-1, -1, null, null).getResults();
        //post actions are preserved
        entites.forEach(entity -> remove(entity.getId()));
    }

    /**
     * @param id parameter that indicates a entity id
     * @return
     */
    @Override
    public T find(long id) {
        log.debug("Repository Find entity {} with id: {}", this.type.getSimpleName(), id);
        Query filter = this.getQueryBuilderInstance().field("id").equalTo(id);
        if (filter != null) return this.find(filter);
        throw new NoResultException();
    }


    /**
     * @param filterStr filter
     * @return
     */
    @Override
    public T find(String filterStr) {
        Query filter = getQueryBuilderInstance().createQueryFilter(filterStr);
        return find(filter);
    }

    /**
     * Can be overridden in order to change the logic how to retrieve entity manager
     *
     * @param filter filter
     * @return
     */
    @Override
    public T find(Query filter) {
        T entity = tx(Transactional.TxType.SUPPORTS, em -> doFind(filter, em));
        if (entity == null)
            throw new NoResultException();
        return entity;
    }

    protected T doFind(Query filter, EntityManager em) {
        log.debug("Repository Find entity {} with filter: {}", this.type.getSimpleName(), filter);
        log.debug("Transaction found, invoke find");
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<T> query = criteriaBuilder.createQuery(this.type);
        Root<T> entityDef = query.from(this.type);
        Predicate condition = (filter != null) ? toPredicate(filter, entityDef, query, criteriaBuilder) : null;
        CriteriaQuery<T> criteriaQuery = (condition != null) ? query.select(entityDef).where(condition) : query.select(entityDef);
        jakarta.persistence.Query q = em.createQuery(criteriaQuery);
        try {
            T entity = (T) q.getSingleResult();
            log.debug("Found entity: {}", entity);
            //Detaching entity in order to prevent unwanted logic
            em.detach(entity);
            return entity;
        } catch (jakarta.persistence.NoResultException e) {
            return null;
        } catch (Exception e) {
            throw new WaterRuntimeException("Generic error, while executing find: " + e.getMessage());
        }
    }

    /**
     * Find all entity
     * Can be overridden in order to change the logic how to retrieve entity manager
     */
    @SuppressWarnings("unchecked")
    @Override
    public PaginatedResult<T> findAll(int delta, int page, Query filter, QueryOrder queryOrder) {
        return tx(Transactional.TxType.SUPPORTS, em -> doFindAll(delta, page, filter, queryOrder, em));
    }

    protected PaginatedResult<T> doFindAll(int delta, int page, Query filter, QueryOrder queryOrder, EntityManager em) {
        log.debug("Repository Find All entities {}", this.type.getSimpleName());
        jakarta.persistence.Query q = createQuery(filter, queryOrder, em);
        int lastPageNumber = 1;
        int nextPage = 1;

        if (delta > 0 && page > 0) {
            Long countResults = countAll(filter);
            lastPageNumber = (int) (Math.ceil(countResults / (double) delta));
            nextPage = (page <= lastPageNumber - 1) ? page + 1 : 1;
            //Executing paginated query
            int firstResult = (page - 1) * delta;
            q.setFirstResult(firstResult);
            q.setMaxResults(delta);
        }

        Collection<T> results = q.getResultList();
        PaginatedResult<T> paginatedResult = new PaginatedResult<>(lastPageNumber, page, nextPage, delta, results);
        log.debug("Query results: {}", results);
        return paginatedResult;
    }


    /**
     * Can be overridden in order to change the logic how to retrieve entity manager
     *
     * @param filter filter query filter, can be null
     * @return entity count based on a specified filter
     */
    @Override
    public long countAll(Query filter) {
        return tx(Transactional.TxType.SUPPORTS, em -> doCountAll(filter, em));
    }

    protected long doCountAll(Query filter, EntityManager em) {
        log.debug("Repository countAll entities {}", this.type.getSimpleName());
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        //constructing query and count query
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<T> entityDefCount = countQuery.from(this.type);
        Predicate conditionCount = (filter != null) ? toPredicate(filter, entityDefCount, countQuery, criteriaBuilder) : null;
        countQuery = (conditionCount != null) ? countQuery.select(criteriaBuilder.count(entityDefCount)).where(conditionCount) : countQuery.select(criteriaBuilder.count(entityDefCount));
        //Executing count query
        jakarta.persistence.Query countQueryFinal = em.createQuery(countQuery);
        return (Long) countQueryFinal.getSingleResult();
    }


    private List<Order> getOrders(CriteriaBuilder criteriaBuilder, Root<T> entityDef, QueryOrder queryOrder) {
        List<Order> criteriaOrderClause = new ArrayList<>();
        List<QueryOrderParameter> parameterList = queryOrder.getParametersList();
        if (parameterList != null && !parameterList.isEmpty()) {
            for (QueryOrderParameter orderParameter : parameterList) {
                criteriaOrderClause.add((orderParameter.isAsc()) ? criteriaBuilder.asc(entityDef.get(orderParameter.getName())) : criteriaBuilder.desc(entityDef.get(orderParameter.getName())));
            }
        }
        return criteriaOrderClause;
    }

    private jakarta.persistence.Query createQuery(Query filter, QueryOrder queryOrder, EntityManager em) {
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<T> criteriaQuery = criteriaBuilder.createQuery(this.type);
        Root<T> entityDef = criteriaQuery.from(this.type);
        Predicate condition = (filter != null) ? toPredicate(filter, entityDef, criteriaQuery, criteriaBuilder) : null;
        criteriaQuery = (condition != null) ? criteriaQuery.select(entityDef).where(condition) : criteriaQuery.select(entityDef);
        //adding order if necessary
        criteriaQuery = (queryOrder != null && queryOrder.getParametersList() != null && !queryOrder.getParametersList().isEmpty()) ? criteriaQuery.orderBy(getOrders(criteriaBuilder, entityDef, queryOrder)) : criteriaQuery;
        return em.createQuery(criteriaQuery);
    }

    private Predicate toPredicate(Query filter, Root<T> entityDef, CriteriaQuery<?> criteriaQuery, CriteriaBuilder criteriaBuilder) {
        PredicateBuilder<T> predicateBuilder = new PredicateBuilder<>(entityDef, criteriaQuery, criteriaBuilder);
        return predicateBuilder.buildPredicate(filter);
    }

    @Override
    public QueryBuilder getQueryBuilderInstance() {
        return new DefaultQueryBuilder();
    }

    /**
     * Define the default persistence unit name
     *
     * @return
     */
    protected String getPersistenceUnitName() {
        return persistenceUnitName;
    }
}

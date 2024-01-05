
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
import it.water.core.api.repository.BaseRepository;
import it.water.core.api.repository.RepositoryConstraintValidator;
import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.QueryBuilder;
import it.water.core.api.repository.query.QueryOrder;
import it.water.core.api.repository.query.QueryOrderParameter;
import it.water.repository.entity.model.PaginatedResult;
import it.water.repository.entity.model.exceptions.EntityNotFound;
import it.water.repository.entity.model.exceptions.NoResultException;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.repository.query.DefaultQueryBuilder;
import it.water.repository.jpa.constraints.DuplicateConstraintValidator;
import it.water.repository.jpa.constraints.RepositoryConstraintValidatorsManager;
import it.water.repository.jpa.query.PredicateBuilder;
import lombok.AccessLevel;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * @param <T> parameter that indicates a generic class which must extend Base Entity
 *            Model class for BaseRepositoryImpl.
 *            This class implements all methods for basic CRUD operations defined in
 *            BaseRepository interface. These methods are reusable by all
 *            entities that interact with the platform.
 * @Author Aristide Cittadino.
 */
@Transactional
public abstract class BaseJpaRepositoryImpl<T extends BaseEntity> implements BaseRepository<T> {
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
    @Getter(AccessLevel.PROTECTED)
    private EntityManager entityManager;

    protected RepositoryConstraintValidatorsManager dbConstraintsValidatorManager;

    protected BaseJpaRepositoryImpl(Class<T> type) {
        this.type = type;
        this.dbConstraintsValidatorManager = new RepositoryConstraintValidatorsManager(new DuplicateConstraintValidator());
        this.entityManager = null;
    }

    /**
     * Constructor for WaterBaseRepositoryImpl
     *
     * @param type parameter that indicates a generic entity
     */
    protected BaseJpaRepositoryImpl(Class<T> type, EntityManager entityManager) {
        this.type = type;
        this.dbConstraintsValidatorManager = new RepositoryConstraintValidatorsManager(new DuplicateConstraintValidator());
        this.entityManager = entityManager;
    }

    protected BaseJpaRepositoryImpl(Class<T> type, EntityManager entityManager, RepositoryConstraintValidator... dbConstraintValidators) {
        this(type, entityManager);
        this.dbConstraintsValidatorManager = new RepositoryConstraintValidatorsManager(dbConstraintValidators);
    }


    /**
     * Save an entity in database
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public T persist(T entity) {
        EntityManager em = getEntityManager();
        log.debug("Repository Saving entity {}: {}", this.type.getSimpleName(), entity);
        this.dbConstraintsValidatorManager.runCheck(entity, this.type, this);
        log.debug("Transaction found, invoke persist");
        em.persist(entity);
        log.debug("Entity persisted: {}", entity);
        return entity;
    }

    /**
     * Update an entity in database
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public T update(T entity) {
        EntityManager em = getEntityManager();
        log.debug("Repository Update entity {}: {}", this.type.getSimpleName(), entity);
        this.dbConstraintsValidatorManager.runCheck(entity, this.type, this);
        //Enforcing the concept that the owner cannot be changed
        //TO DO: check if it is useful or not
        T entityFromDb = find(entity.getId());
        if (entityFromDb instanceof OwnedResource) {
            OwnedResource ownedFromDb = (OwnedResource) entityFromDb;
            User oldOwner = ownedFromDb.getUserOwner();
            OwnedResource owned = (OwnedResource) entity;
            owned.setUserOwner(oldOwner);
        }
        if (entity.getId() > 0) {
            log.debug("Transaction found, invoke find and merge");
            T updateEntity = em.merge(entity);
            log.debug("Entity merged: {}", entity);
            return updateEntity;
        }
        throw new EntityNotFound();
    }

    /**
     * Remove an entity by id
     */
    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void remove(long id) {
        EntityManager em = getEntityManager();
        log.debug("Repository Remove entity {} with id: {}", this.type.getSimpleName(), id);
        T entity = em.find(type, id);
        em.remove(entity);
        log.debug("Entity {}  with id: {}  removed", this.type.getSimpleName(), id);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void remove(T entity) {
        log.debug("Repository Remove all entities {}: {}", this.type.getSimpleName(), entity);
        //post actions are preserved
        this.remove(entity.getId());
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void removeAllByIds(Iterable<Long> ids) {
        log.debug("Repository Remove all entities {} by ids {}", this.type.getSimpleName(), ids);
        //post actions are preserved
        ids.forEach(this::remove);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void removeAll(Iterable<T> entities) {
        log.debug("Repository Remove all entities {}: {}", this.type.getSimpleName(), entities);
        //post actions are preserved
        entities.forEach(entity -> this.remove(entity.getId()));
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
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
    @Transactional(Transactional.TxType.SUPPORTS)
    public T find(long id) {
        log.debug("Repository Find entity {} with id: {}", this.type.getSimpleName(), id);
        Query filter = this.getQueryBuilderInstance().createQueryFilter("id=" + id);
        if (filter != null) return this.find(filter);
        throw new NoResultException();
    }


    /**
     * @param filterStr filter
     * @return
     */
    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public T find(String filterStr) {
        Query filter = getQueryBuilderInstance().createQueryFilter(filterStr);
        return find(filter);
    }

    /**
     * @param filter filter
     * @return
     */
    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public T find(Query filter) {
        EntityManager em = getEntityManager();
        log.debug("Repository Find entity {} with filter: {}", this.type.getSimpleName(), filter);
        log.debug("Transaction found, invoke find");
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaQuery<T> query = criteriaBuilder.createQuery(this.type);
        Root<T> entityDef = query.from(this.type);
        Predicate condition = (filter != null) ? toPredicate(filter, entityDef, query, criteriaBuilder) : null;
        CriteriaQuery<T> criteriaQuery = (condition != null) ? query.select(entityDef).where(condition) : query.select(entityDef);
        javax.persistence.Query q = em.createQuery(criteriaQuery);
        try {
            T entity = (T) q.getSingleResult();
            log.debug("Found entity: {}", entity);
            return entity;
        } catch (javax.persistence.NoResultException e) {
            throw new it.water.repository.entity.model.exceptions.NoResultException();
        } catch (Exception e) {
            throw new WaterRuntimeException("Generic error, while executing find: " + e.getMessage());
        }
    }

    /**
     * Find all entity
     */
    @SuppressWarnings("unchecked")
    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public PaginatedResult<T> findAll(int delta, int page, Query filter, QueryOrder queryOrder) {
        log.debug("Repository Find All entities {}", this.type.getSimpleName());
        javax.persistence.Query q = createQuery(filter, queryOrder);
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
     * @param filter filter query filter, can be null
     * @return entity count based on a specified filter
     */
    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public long countAll(Query filter) {
        EntityManager em = getEntityManager();
        log.debug("Repository countAll entities {}", this.type.getSimpleName());
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        //constructing query and count query
        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<T> entityDefCount = countQuery.from(this.type);
        Predicate conditionCount = (filter != null) ? toPredicate(filter, entityDefCount, countQuery, criteriaBuilder) : null;
        countQuery = (conditionCount != null) ? countQuery.select(criteriaBuilder.count(entityDefCount)).where(conditionCount) : countQuery.select(criteriaBuilder.count(entityDefCount));
        //Executing count query
        javax.persistence.Query countQueryFinal = em.createQuery(countQuery);
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

    private javax.persistence.Query createQuery(Query filter, QueryOrder queryOrder) {
        EntityManager em = getEntityManager();
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
    @Transactional
    public QueryBuilder getQueryBuilderInstance() {
        return new DefaultQueryBuilder();
    }
}

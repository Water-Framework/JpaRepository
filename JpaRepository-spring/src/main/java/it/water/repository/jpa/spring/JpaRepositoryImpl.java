
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

package it.water.repository.jpa.spring;

import it.water.core.api.model.PaginableResult;
import it.water.core.api.repository.BaseRepository;
import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.QueryBuilder;
import it.water.core.api.repository.query.QueryOrder;
import it.water.repository.entity.model.exceptions.NoResultException;
import it.water.repository.jpa.model.AbstractJpaEntity;
import it.water.repository.query.DefaultQueryBuilder;
import jakarta.persistence.EntityManager;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;

/**
 * @Author Aristide Cittadino.
 * Water Implementation of the spring crud repository.
 * With this implementation or persistence logic (pre-post actions for example) is still preserved.
 */
public class JpaRepositoryImpl<T extends AbstractJpaEntity> extends SimpleJpaRepository<T, Long> implements BaseRepository<T> {

    //wrapping WaterBaseRepository
    private BaseRepository<T> repository;


    public JpaRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager, PlatformTransactionManager transactionManager) {
        super(entityInformation, entityManager);
        initWaterBaseRepository(entityInformation.getJavaType(), entityManager, transactionManager);
    }

    public JpaRepositoryImpl(Class<T> domainClass, EntityManager em, PlatformTransactionManager transactionManager) {
        super(domainClass, em);
        initWaterBaseRepository(domainClass, em, transactionManager);
    }


    /**
     * Constructing anonymouse implementation based on information passed to the jpa repo
     *
     * @param entityClass
     * @param entityManager
     */
    private void initWaterBaseRepository(Class<T> entityClass, EntityManager entityManager, PlatformTransactionManager transactionManager) {
        repository = new SpringBaseJpaRepositoryImpl<>(entityClass, entityManager.getEntityManagerFactory(), transactionManager) {
        };
    }

    @Override
    public Class<T> getEntityType() {
        return repository.getEntityType();
    }

    @SuppressWarnings("null")
    @Override
    public List<T> findAll() {
        List<T> results = new ArrayList<>();
        results.addAll(repository.findAll(-1, -1, null, null).getResults());
        return results;
    }

    @SuppressWarnings("null")
    @Override
    public List<T> findAllById(@SuppressWarnings("null") Iterable<Long> longs) {
        Iterator<Long> it = longs.iterator();
        if (!it.hasNext()) return Collections.emptyList();
        List<T> results = new ArrayList<>();
        Query idsOr = this.getQueryBuilderInstance().field("id").equalTo(it.next());
        while (it.hasNext()) {
            idsOr = idsOr.or(this.getQueryBuilderInstance().field("id").equalTo(it.next()));
        }
        results.addAll(repository.findAll(-1, -1, idsOr, null).getResults());
        return results;
    }

    @Override
    public long count() {
        return repository.countAll(null);
    }

    @Override
    public void deleteById(@SuppressWarnings("null") Long aLong) {
        repository.remove(aLong);
    }

    @Override
    public void delete(@SuppressWarnings("null") T entity) {
        repository.remove(entity.getId());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void deleteAllById(@SuppressWarnings("null") Iterable<? extends Long> longs) {
        repository.removeAllByIds((Iterable<Long>) longs);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void deleteAll(@SuppressWarnings("null") Iterable<? extends T> entities) {
        repository.removeAll((Iterable<T>) entities);
    }

    @Override
    public void deleteAll() {
        repository.removeAll();
    }

    @SuppressWarnings({ "null", "unchecked" })
    @Override
    public <S extends T> S save(@SuppressWarnings("null") S entity) {
        return (S) repository.persist(entity);
    }

    @SuppressWarnings("null")
    @Override
    public <S extends T> List<S> saveAll(@SuppressWarnings("null") Iterable<S> entities) {
        entities.forEach(entity -> repository.persist(entity));
        return (List<S>) entities;
    }

    @SuppressWarnings("null")
    @Override
    public Optional<T> findById(@SuppressWarnings("null") Long aLong) {
        return Optional.of(repository.find(aLong));
    }

    @Override
    public boolean existsById(@SuppressWarnings("null") Long aLong) {
        try {
            return findById(aLong).isPresent();
        } catch (NoResultException e) {
            return false;
        }
    }

    //########### WaterBase Repository Methods ##################

    @Override
    public T persist(T entity) {
        return this.persist(entity, null);
    }

    @Override
    public T persist(T entity, Runnable runnable) {
        return this.repository.persist(entity, runnable);
    }

    @Override
    public T update(T entity) {
        return this.update(entity, null);
    }

    @Override
    public T update(T entity, Runnable runnable) {
        return this.repository.update(entity, null);
    }

    @Override
    public void remove(long id) {
        this.remove(id, null);
    }

    @Override
    public void remove(long id, Runnable runnable) {
        this.repository.remove(id, runnable);
    }

    @Override
    public void remove(T entity) {
        this.repository.remove(entity);
    }

    @Override
    public void removeAllByIds(Iterable<Long> ids) {
        this.repository.removeAllByIds(ids);
    }

    @Override
    public void removeAll(Iterable<T> entities) {
        this.repository.removeAll(entities);
    }

    @Override
    public void removeAll() {
        this.repository.removeAll();
    }

    @Override
    public T find(long id) {
        return this.repository.find(id);
    }

    @Override
    public T find(Query filter) {
        return this.repository.find(filter);
    }

    @Override
    public T find(String filterStr) {
        return this.repository.find(filterStr);
    }

    @Override
    public PaginableResult<T> findAll(int delta, int page, Query filter, QueryOrder queryOrder) {
        return this.repository.findAll(delta, page, filter, queryOrder);
    }

    @Override
    public long countAll(Query filter) {
        return this.repository.countAll(filter);
    }

    @Override
    public QueryBuilder getQueryBuilderInstance() {
        return new DefaultQueryBuilder();
    }
}

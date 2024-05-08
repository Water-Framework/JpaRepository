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

import it.water.core.api.interceptors.OnActivate;
import it.water.core.api.model.BaseEntity;
import it.water.core.api.model.PaginableResult;
import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.QueryBuilder;
import it.water.core.api.repository.query.QueryOrder;
import it.water.repository.jpa.api.JpaRepository;
import it.water.repository.jpa.api.JpaRepositoryManager;
import it.water.repository.jpa.api.WaterJpaRepository;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @Author Aristide Cittadino
 * This class represents a framework repository object which must be materialized in specific technologies.
 * Example: if you create a framework (water) bundle with an entity. This module could be instantiated in spring,osgi,quarkus.
 * For every technology it must be used in they way the specific technology supports.
 * <p>
 * This wrapper allows to "attach" a concrete repository instance at runtime.
 * It must be always an BaseJpaRepositoryImpl object.
 */
public class WaterJpaRepositoryImpl<T extends BaseEntity> implements WaterJpaRepository<T> {
    private JpaRepository<T> concreteRepository;
    private Class<T> type;
    private String persistenceUnitName;

    public WaterJpaRepositoryImpl(Class<T> type, String persistenceUnitName) {
        this.type = type;
        this.persistenceUnitName = persistenceUnitName;
    }

    @OnActivate
    public void onActivate(JpaRepositoryManager jpaRepositoryManager) {
        this.concreteRepository = jpaRepositoryManager.createConcreteRepository(type, persistenceUnitName);
    }

    protected Class<T> getType() {
        return type;
    }

    protected String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    @Override
    public EntityManager getEntityManager() {
        return concreteRepository.getEntityManager();
    }

    @Override
    public T persist(T entity) {
        return concreteRepository.persist(entity);
    }

    @Override
    public T update(T entity) {
        return concreteRepository.update(entity);
    }

    @Override
    public void remove(long id) {
        concreteRepository.remove(id);
    }

    @Override
    public void remove(T entity) {
        concreteRepository.remove(entity);
    }

    @Override
    public void removeAllByIds(Iterable<Long> ids) {
        concreteRepository.removeAllByIds(ids);
    }

    @Override
    public void removeAll(Iterable<T> entities) {
        concreteRepository.removeAll(entities);
    }

    @Override
    public void removeAll() {
        concreteRepository.removeAll();
    }

    @Override
    public T find(long id) {
        return concreteRepository.find(id);
    }

    @Override
    public T find(String filterStr) {
        return concreteRepository.find(filterStr);
    }

    @Override
    public T find(Query filter) {
        return concreteRepository.find(filter);
    }

    @Override
    public PaginableResult<T> findAll(int delta, int page, Query filter, QueryOrder queryOrder) {
        return concreteRepository.findAll(delta, page, filter, queryOrder);
    }

    @Override
    public long countAll(Query filter) {
        return concreteRepository.countAll(filter);
    }

    @Override
    public QueryBuilder getQueryBuilderInstance() {
        return concreteRepository.getQueryBuilderInstance();
    }

    @Override
    public void txExpr(Transactional.TxType txType, Consumer<EntityManager> function) {
        concreteRepository.txExpr(txType, function);
    }

    @Override
    public <R> R tx(Transactional.TxType txType, Function<EntityManager, R> function) {
        return concreteRepository.tx(txType, function);
    }
}

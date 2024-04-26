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

import it.water.core.api.model.BaseEntity;
import it.water.core.api.repository.RepositoryConstraintValidator;
import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.QueryOrder;
import it.water.repository.entity.model.PaginatedResult;
import it.water.repository.jpa.BaseJpaRepositoryImpl;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

/**
 * @param <T>
 * @Author Aristide Cittadino
 * This class just expose same methods of BaseJpaRepository adding @Transactional annotations since in spring it is sufficient
 * for transaction management.
 */
public class SpringBaseJpaRepositoryImpl<T extends BaseEntity> extends BaseJpaRepositoryImpl<T> {
    public SpringBaseJpaRepositoryImpl(Class<T> type) {
        super(type);
    }

    public SpringBaseJpaRepositoryImpl(Class<T> type, String persistenceUnitName) {
        super(type, persistenceUnitName);
    }

    public SpringBaseJpaRepositoryImpl(Class<T> type, EntityManager entityManager) {
        super(type, entityManager);
    }

    public SpringBaseJpaRepositoryImpl(Class<T> type, EntityManager entityManager, RepositoryConstraintValidator... dbConstraintValidators) {
        super(type, entityManager, dbConstraintValidators);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public T persist(T entity) {
        return super.persist(entity);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public T update(T entity) {
        return super.update(entity);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void remove(long id) {
        super.remove(id);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void remove(T entity) {
        super.remove(entity);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void removeAllByIds(Iterable<Long> ids) {
        super.removeAllByIds(ids);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void removeAll(Iterable<T> entities) {
        super.removeAll(entities);
    }

    @Override
    @Transactional(Transactional.TxType.REQUIRED)
    public void removeAll() {
        super.removeAll();
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public T find(long id) {
        return super.find(id);
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public T find(String filterStr) {
        return super.find(filterStr);
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public T find(Query filter) {
        return super.find(filter);
    }

    @Override
    @Transactional(value = Transactional.TxType.REQUIRES_NEW)
    public PaginatedResult<T> findAll(int delta, int page, Query filter, QueryOrder queryOrder) {
        return super.findAll(delta, page, filter, queryOrder);
    }

    @Override
    @Transactional(Transactional.TxType.SUPPORTS)
    public long countAll(Query filter) {
        return super.countAll(filter);
    }
}

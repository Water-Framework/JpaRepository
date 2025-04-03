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
package it.water.repository.jpa.repository;

import it.water.core.api.repository.RepositoryConstraintValidator;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.repository.jpa.BaseJpaRepositoryImpl;
import it.water.repository.jpa.api.TestEntityRepository;
import it.water.repository.jpa.entity.TestEntity;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;


import java.util.function.Consumer;
import java.util.function.Function;

@FrameworkComponent
public class TestEntityRepositoryImpl extends BaseJpaRepositoryImpl<TestEntity> implements TestEntityRepository {

    public TestEntityRepositoryImpl() {
        super(TestEntity.class);
    }

    public TestEntityRepositoryImpl(Class<TestEntity> type, String persistenceUnitName) {
        super(type, persistenceUnitName);
    }

    public TestEntityRepositoryImpl(Class<TestEntity> type, String persistenceUnitName, EntityManager entityManager) {
        super(type, persistenceUnitName, entityManager);
    }

    public TestEntityRepositoryImpl(Class<TestEntity> type, EntityManager entityManager) {
        super(type, entityManager);
    }

    public TestEntityRepositoryImpl(Class<TestEntity> type, EntityManager entityManager, RepositoryConstraintValidator... dbConstraintValidators) {
        super(type, entityManager, dbConstraintValidators);
    }

    @Override
    public void txExpr(Transactional.TxType txType, Consumer<EntityManager> function) {
        function.accept(getEntityManager());
    }

    @Override
    public <R> R tx(Transactional.TxType txType, Function<EntityManager, R> function) {
        return function.apply(getEntityManager());
    }
}

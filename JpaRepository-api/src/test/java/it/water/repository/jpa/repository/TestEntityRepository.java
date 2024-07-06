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

import it.water.repository.jpa.BaseJpaRepositoryImpl;
import it.water.repository.jpa.entity.TestEntity;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;


import java.util.function.Consumer;
import java.util.function.Function;

public class TestEntityRepository extends BaseJpaRepositoryImpl<TestEntity> {

    public TestEntityRepository(Class<TestEntity> type) {
        super(type);
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

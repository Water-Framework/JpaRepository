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

package it.water.repository.jpa.test.utils;

import it.water.core.api.model.BaseEntity;
import it.water.repository.jpa.BaseJpaRepositoryImpl;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.function.Consumer;
import java.util.function.Function;

public class TestBaseJpaRepositoryImpl<T extends BaseEntity> extends BaseJpaRepositoryImpl<T> {

    public TestBaseJpaRepositoryImpl(Class<T> type, String persistenceUnitName) {
        super(type, persistenceUnitName);
    }

    @Override
    public void txExpr(Transactional.TxType txType, Consumer function) {
        function.accept(this.getEntityManager());
    }

    @Override
    public Object tx(Transactional.TxType txType, Function function) {
        return function.apply(this.getEntityManager());
    }

    @Override
    protected boolean isTransactionalSupported(EntityManager em) {
        return false;
    }
}

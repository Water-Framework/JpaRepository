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
import it.water.repository.jpa.BaseJpaRepositoryImpl;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @param <T>
 * @Author Aristide Cittadino
 * This class just expose same methods of BaseJpaRepository adding @Transactional annotations since in spring it is sufficient
 * for transaction management.
 */
public class SpringBaseJpaRepositoryImpl<T extends BaseEntity> extends BaseJpaRepositoryImpl<T> {
    private TransactionTemplate transactionTemplate;

    public SpringBaseJpaRepositoryImpl(Class<T> type) {
        super(type);
        initTransactionTemplate();
    }

    public SpringBaseJpaRepositoryImpl(Class<T> type, String persistenceUnitName) {
        super(type, persistenceUnitName);
        initTransactionTemplate();
    }

    public SpringBaseJpaRepositoryImpl(Class<T> type, EntityManager entityManager) {
        super(type, entityManager);
        initTransactionTemplate();
    }

    public SpringBaseJpaRepositoryImpl(Class<T> type, EntityManager entityManager, RepositoryConstraintValidator... dbConstraintValidators) {
        super(type, entityManager, dbConstraintValidators);
        initTransactionTemplate();
    }

    private void initTransactionTemplate() {
        JpaTransactionManager jpaTransactionManager = new JpaTransactionManager(getEntityManager().getEntityManagerFactory());
        transactionTemplate = new TransactionTemplate(jpaTransactionManager);
    }

    @Override
    public void txExpr(Transactional.TxType txType, Consumer<EntityManager> function) {
        transactionTemplate.setPropagationBehavior(mapTxType(txType));
        transactionTemplate.execute(status -> {
            function.accept(getEntityManager());
            return null;
        });
    }

    @Override
    public <R> R tx(Transactional.TxType txType, Function<EntityManager, R> function) {
        transactionTemplate.setPropagationBehavior(mapTxType(txType));
        return transactionTemplate.execute(status -> function.apply(getEntityManager()));
    }

    private int mapTxType(Transactional.TxType txType) {
        if (txType.equals(Transactional.TxType.REQUIRED))
            return TransactionDefinition.PROPAGATION_REQUIRED;
        else if (txType.equals(Transactional.TxType.REQUIRES_NEW))
            return TransactionDefinition.PROPAGATION_REQUIRES_NEW;
        else if (txType.equals(Transactional.TxType.SUPPORTS))
            return TransactionDefinition.PROPAGATION_SUPPORTS;
        else if (txType.equals(Transactional.TxType.NEVER))
            return TransactionDefinition.PROPAGATION_NEVER;
        else if (txType.equals(Transactional.TxType.MANDATORY))
            return TransactionDefinition.PROPAGATION_MANDATORY;
        throw new IllegalArgumentException("Invalid txType");
    }
}

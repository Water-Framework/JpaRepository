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

package it.water.repository.jpa.spring.manager;

import it.water.core.api.model.BaseEntity;
import it.water.repository.jpa.api.JpaRepository;
import it.water.repository.jpa.api.JpaRepositoryManager;
import it.water.repository.jpa.spring.SpringBaseJpaRepositoryImpl;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;


/**
 * @Author Aristide Cittadino
 * Spring Jpa Repository Manager simply create a spring based jpa repository impl.
 * This class is used when repository object are injected inside spring context but they are not native in spring.
 */
@Service
public class SpringJpaRepositoryManager implements JpaRepositoryManager {
    private Logger log = LoggerFactory.getLogger(SpringJpaRepositoryManager.class);
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Override
    public <T extends BaseEntity> JpaRepository<T> createConcreteRepository(Class<T> entityType, String persistenceUnit) {
        log.debug("Loading Entity Manager for {}", entityType.getName());
        return new SpringBaseJpaRepositoryImpl<>(entityType, entityManagerFactory, transactionManager);
    }
}

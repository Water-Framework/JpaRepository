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
import it.water.repository.jpa.api.JpaRepository;
import it.water.repository.jpa.api.JpaRepositoryManager;
import org.springframework.stereotype.Service;

/**
 * @Author Aristide Cittadino
 * Osgi Jpa Repository Manager simply create a aries based jpa repository impl.
 */
@Service
public class SpringJpaRepositoryManager implements JpaRepositoryManager {

    @Override
    public <T extends BaseEntity> JpaRepository<T> createConcreteRepository(Class<T> entityType, String persistenceUnit) {
        //todo get repository factory from spring context and insert one
        return new SpringBaseJpaRepositoryImpl<>(entityType, persistenceUnit);
    }
}

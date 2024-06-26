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

package it.water.repository.jpa.api;

import it.water.core.api.model.BaseEntity;
import it.water.core.api.service.Service;

/**
 * @author Aristide Cittadino
 * Object which has the responsability to create specific framework repository when it find a water (cross frameowork) repository bean.
 */
public interface JpaRepositoryManager extends Service {
    <T extends BaseEntity> JpaRepository<T> createConcreteRepository(Class<T> entityType, String persistenceUnit);
}

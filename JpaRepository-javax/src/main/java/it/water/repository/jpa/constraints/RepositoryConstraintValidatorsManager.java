
/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */

package it.water.repository.jpa.constraints;

import it.water.core.api.model.BaseEntity;
import it.water.core.api.repository.BaseRepository;
import it.water.core.api.repository.RepositoryConstraintValidator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


/**
 * @Author Aristide Cittadino.
 * This class manages how to create validators for specific service.
 */
public class RepositoryConstraintValidatorsManager {
    private Set<RepositoryConstraintValidator> checkers;

    public RepositoryConstraintValidatorsManager(RepositoryConstraintValidator... checkers) {
        this.checkers = new HashSet<>();
        this.checkers.addAll(Arrays.asList(checkers));
    }

    public void addDbConstraintChecker(RepositoryConstraintValidator checker) {
        this.checkers.add(checker);
    }

    public <T extends BaseEntity> void runCheck(T entity, Class<T> type, BaseRepository<T> repo) {
        checkers.stream().forEach(checker -> checker.checkConstraint(entity, type, repo));
    }
}

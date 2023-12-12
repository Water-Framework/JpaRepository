
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

package it.water.repository.jpa.spring;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import javax.persistence.EntityManager;


/**
 * @param <R> JpaRepository
 * @param <K> Db Key
 * @param <T> Db Entity
 * @Author Aristide Cittadino
 */
public class RepositoryFactory<R extends JpaRepository<T, K>,T,K> extends JpaRepositoryFactoryBean<R, T, K> {


    /**
     * Creates a new {@link JpaRepositoryFactoryBean} for the given repository interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     */
    public RepositoryFactory(Class<? extends R> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
        return new WaterJpaExecutorFactory(entityManager);
    }

    /**
     * Simple jpa executor factory
     *
     */
    private static class WaterJpaExecutorFactory extends JpaRepositoryFactory {

        /**
         * Simple jpa executor factory constructor
         *
         * @param entityManager entity manager
         */
        public WaterJpaExecutorFactory(EntityManager entityManager) {
            super(entityManager);
        }

        @Override
        protected Class getRepositoryBaseClass(RepositoryMetadata metadata) {
            return JpaRepositoryImpl.class;
        }


    }
}

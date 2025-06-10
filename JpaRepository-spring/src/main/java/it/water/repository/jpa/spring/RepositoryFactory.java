
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

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.jpa.repository.query.JpaQueryMethodFactory;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.lang.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.Serializable;

/**
 * @param <R> JpaRepository
 * @param <K> Db Key
 * @param <T> Db Entity
 * @Author Aristide Cittadino
 */
public class RepositoryFactory<R extends JpaRepository<T, K>, T, K> extends JpaRepositoryFactoryBean<R, T, K> {
    private EntityPathResolver entityPathResolver;
    private EscapeCharacter escapeCharacter = EscapeCharacter.DEFAULT;
    private JpaQueryMethodFactory queryMethodFactory;
    private PlatformTransactionManager platformTransactionManager;

    /**
     * Creates a new {@link JpaRepositoryFactoryBean} for the given repository interface.
     *
     * @param repositoryInterface must not be {@literal null}.
     */
    public RepositoryFactory(Class<? extends R> repositoryInterface, PlatformTransactionManager transactionManager) {
        super(repositoryInterface);
        this.platformTransactionManager = transactionManager;
    }

    @SuppressWarnings("null")
    @Override
    protected RepositoryFactorySupport createRepositoryFactory(@SuppressWarnings("null") EntityManager entityManager) {
        WaterJpaExecutorFactory factory = new WaterJpaExecutorFactory(entityManager, this.platformTransactionManager);
        factory.setEntityPathResolver(entityPathResolver);
        factory.setEscapeCharacter(escapeCharacter);
        if (queryMethodFactory != null) {
            factory.setQueryMethodFactory(queryMethodFactory);
        }
        return factory;
    }

    @Override
    @Autowired
    public void setEntityPathResolver(@SuppressWarnings("null") ObjectProvider<EntityPathResolver> resolver) {
        this.entityPathResolver = resolver.getIfAvailable(() -> SimpleEntityPathResolver.INSTANCE);
    }

    /**
     * Configures the {@link JpaQueryMethodFactory} to be used. Will expect a canonical bean to be present but will
     * fallback to {@link org.springframework.data.jpa.repository.query.DefaultJpaQueryMethodFactory} in case none is
     * available.
     *
     * @param factory may be {@literal null}.
     */
    @Override
    @Autowired
    public void setQueryMethodFactory(@Nullable JpaQueryMethodFactory factory) {

        if (factory != null) {
            this.queryMethodFactory = factory;
        }
    }

    @Override
    public void setEscapeCharacter(char escapeCharacter) {
        this.escapeCharacter = EscapeCharacter.of(escapeCharacter);
    }


    /**
     * Simple jpa executor factory
     */
    private static class WaterJpaExecutorFactory extends JpaRepositoryFactory {

        private PlatformTransactionManager platformTransactionManager;

        public WaterJpaExecutorFactory(EntityManager entityManager, PlatformTransactionManager platformTransactionManager) {
            super(entityManager);
            this.platformTransactionManager = platformTransactionManager;
        }

        /**
         * Platform transaction manager to support transaction management
         *
         * @return
         */
        @SuppressWarnings("unused")
        public PlatformTransactionManager getPlatformTransactionManager() {
            return platformTransactionManager;
        }

        @SuppressWarnings({ "null", "unchecked", "rawtypes" })
        @Override
        protected Class getRepositoryBaseClass(@SuppressWarnings("null") RepositoryMetadata metadata) {
            return JpaRepositoryImpl.class;
        }

        @SuppressWarnings({ "null", "unchecked", "rawtypes" })
        @Override
        protected JpaRepositoryImplementation<?, ?> getTargetRepository(@SuppressWarnings("null") RepositoryInformation information, @SuppressWarnings("null") EntityManager entityManager) {
            JpaEntityInformation<?, Serializable> entityInformation = getEntityInformation(information.getDomainType());
            return new JpaRepositoryImpl(entityInformation, entityManager, platformTransactionManager);
        }
    }
}

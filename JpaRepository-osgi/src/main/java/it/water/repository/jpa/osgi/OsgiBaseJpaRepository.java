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

package it.water.repository.jpa.osgi;

import it.water.core.api.model.BaseEntity;
import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.QueryOrder;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.repository.entity.model.PaginatedResult;
import it.water.repository.jpa.BaseJpaRepositoryImpl;
import org.apache.aries.jpa.template.JpaTemplate;
import org.apache.aries.jpa.template.TransactionType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.Optional;

/**
 * @param <T> Class which extends JpaRepository
 *            This class will wrap base jpa repository in osgi context in order to support Transactions
 *            using OSGi Transactional Control Service
 * @Author Aristide Cittadino
 */
public abstract class OsgiBaseJpaRepository<T extends BaseEntity> extends BaseJpaRepositoryImpl<T> {
    private static Logger log = LoggerFactory.getLogger(OsgiBaseJpaRepository.class);
    private JpaTemplate jpaTemplate;

    protected OsgiBaseJpaRepository(Class<T> type, String persistenceUnitName) {
        super(type, persistenceUnitName, null);
    }

    //TODO add service tracker
    protected JpaTemplate getJpaTemplate() {
        if (jpaTemplate == null) {
            BundleContext ctx = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
            String filter = "(osgi.unit.name=" + this.getPersistenceUnitName() + ")";
            ServiceReference<JpaTemplate> transactionControlServiceReference = null;
            try {
                Collection<ServiceReference<JpaTemplate>> transactionControlServiceReferences = ctx.getServiceReferences(JpaTemplate.class, filter);
                //should not be more than one
                if (!transactionControlServiceReferences.isEmpty()) {
                    Optional<ServiceReference<JpaTemplate>> serviceReferenceOptional = transactionControlServiceReferences.stream().findFirst();
                    if (serviceReferenceOptional.isPresent())
                        transactionControlServiceReference = serviceReferenceOptional.get();
                }
            } catch (InvalidSyntaxException e) {
                log.error(e.getMessage(), e);
            }
            if (transactionControlServiceReference == null)
                throw new WaterRuntimeException("No transaction control found!");
            jpaTemplate = ctx.getService(transactionControlServiceReference);
        }
        return jpaTemplate;
    }

    @Override
    public T persist(T entity) {
        return getJpaTemplate().txExpr(TransactionType.Required, entityManager -> super.doPersist(entity, entityManager));
    }

    @Override
    public T update(T entity) {
        return getJpaTemplate().txExpr(TransactionType.Required, entityManager -> super.doUpdate(entity, entityManager));
    }

    @Override
    public void remove(long id) {
        getJpaTemplate().tx(TransactionType.Required, entityManager -> super.doRemove(id, entityManager));
    }

    @Override
    public T find(Query filter) {
        //it's better to spawn a new transaction since find method can raise no component found exception
        return getJpaTemplate().txExpr(TransactionType.RequiresNew, entityManager -> super.doFind(filter, entityManager));
    }

    @Override
    public PaginatedResult<T> findAll(int delta, int page, Query filter, QueryOrder queryOrder) {
        //it's better to spawn a new transaction since find method can raise no component found exception
        return getJpaTemplate().txExpr(TransactionType.RequiresNew, entityManager -> super.doFindAll(delta, page, filter, queryOrder, entityManager));
    }

    @Override
    public long countAll(Query filter) {
        return getJpaTemplate().txExpr(TransactionType.Supports, entityManager -> super.doCountAll(filter, entityManager));
    }

    /**
     * Entity manager is laoded by JpaTempalte session so it must not be possibile to retrieve from this method.
     *
     * @return
     */
    @Override
    public EntityManager getEntityManager() {
        throw new UnsupportedOperationException();
    }
}

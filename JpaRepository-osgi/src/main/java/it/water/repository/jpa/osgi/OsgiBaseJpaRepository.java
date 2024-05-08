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
import it.water.core.model.exceptions.WaterRuntimeException;
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
import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @Author Aristide Cittadino
 * This class just overrides the transactions management methods
 */
public abstract class OsgiBaseJpaRepository<T extends BaseEntity> extends BaseJpaRepositoryImpl<T> {

    private static Logger log = LoggerFactory.getLogger(OsgiBaseJpaRepository.class);
    private JpaTemplate jpaTemplate;

    protected OsgiBaseJpaRepository(Class<T> type, String persistenceUnitName) {
        super(type, persistenceUnitName);
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
    public void txExpr(Transactional.TxType txType, Consumer<EntityManager> function) {
        getJpaTemplate().tx(mapTxType(txType), function::accept);
    }

    @Override
    public <R> R tx(Transactional.TxType txType, Function<EntityManager, R> function) {
        return getJpaTemplate().txExpr(mapTxType(txType), function::apply);
    }

    private TransactionType mapTxType(Transactional.TxType txType) {
        if (txType.equals(Transactional.TxType.REQUIRED))
            return TransactionType.Required;
        else if (txType.equals(Transactional.TxType.REQUIRES_NEW))
            return TransactionType.RequiresNew;
        else if (txType.equals(Transactional.TxType.SUPPORTS))
            return TransactionType.Supports;
        else if (txType.equals(Transactional.TxType.NEVER))
            return TransactionType.Never;
        else if (txType.equals(Transactional.TxType.MANDATORY))
            return TransactionType.Mandatory;
        throw new IllegalArgumentException("Invalid txType");
    }
}

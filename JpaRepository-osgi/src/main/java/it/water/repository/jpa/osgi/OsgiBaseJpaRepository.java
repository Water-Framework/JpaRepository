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

import com.arjuna.ats.jta.UserTransaction;
import it.water.core.api.model.BaseEntity;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.repository.jpa.BaseJpaRepositoryImpl;
import it.water.repository.jpa.osgi.hibernate.OsgiScanner;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.*;
import org.hibernate.cfg.EnvironmentSettings;
import org.hibernate.cfg.JdbcSettings;
import org.hibernate.cfg.PersistenceSettings;
import org.hibernate.cfg.SchemaToolingSettings;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.osgi.framework.*;
import org.osgi.framework.wiring.BundleWiring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @Author Aristide Cittadino
 * This class overrides how persistence unit is created inside osgi context.
 * Basically each bundle has its own persistence unit isolated from other bundles.
 * This "supports" the aggregate concept of domain driven design.
 */
public abstract class OsgiBaseJpaRepository<T extends BaseEntity> extends BaseJpaRepositoryImpl<T> {

    private static Logger log = LoggerFactory.getLogger(OsgiBaseJpaRepository.class);

    protected OsgiBaseJpaRepository(Class<T> type, String persistenceUnitName) {
        super(type, persistenceUnitName);
    }

    @Override
    public void txExpr(Transactional.TxType txType, Consumer<EntityManager> function) {
        try {
            manageTransaction(txType, em -> {
                function.accept(getEntityManager());
                return null;
            });
        } catch (Exception e) {
            throw new WaterRuntimeException(e.getMessage());
        }
    }

    @Override
    public <R> R tx(Transactional.TxType txType, Function<EntityManager, R> function) {
        try {
            return manageTransaction(txType, entityManager -> function.apply(getEntityManager()));
        } catch (Exception e) {
            throw new WaterRuntimeException(e.getMessage());
        }
    }

    @Override
    protected EntityManagerFactory createDefaultEntityManagerFactory() {
        Bundle persistenceBundle = FrameworkUtil.getBundle(this.type);
        DataSource ds = getDataSource();
        ClassLoader entityClassLoader = FrameworkUtil.getBundle(this.type).adapt(BundleWiring.class).getClassLoader();
        Collection<ClassLoader> classLoaders = new ArrayList<>();
        classLoaders.add(entityClassLoader);
        classLoaders.add(Thread.currentThread().getContextClassLoader());
        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.transaction.jta.platform", "org.hibernate.service.jta.platform.internal.JBossStandAloneJtaPlatform");
        properties.put(PersistenceSettings.SCANNER_DISCOVERY, "class");
        properties.put(PersistenceSettings.SCANNER, new OsgiScanner(persistenceBundle));
        properties.put(SchemaToolingSettings.HBM2DDL_AUTO, "update");
        properties.put(JdbcSettings.JAKARTA_JTA_DATASOURCE, ds);
        properties.put(EnvironmentSettings.CLASSLOADERS, classLoaders);
        return new HibernatePersistenceProvider().createEntityManagerFactory(getPersistenceUnitName(), properties);
    }

    private DataSource getDataSource() {
        BundleContext ctx = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
        String filter = "(osgi.jndi.service.name=water)";
        ServiceReference<DataSource> osgiDataSource = null;
        try {
            Collection<ServiceReference<DataSource>> transactionControlServiceReferences = ctx.getServiceReferences(DataSource.class, filter);
            //should not be more than one
            if (!transactionControlServiceReferences.isEmpty()) {
                Optional<ServiceReference<DataSource>> serviceReferenceOptional = transactionControlServiceReferences.stream().findFirst();
                if (serviceReferenceOptional.isPresent())
                    osgiDataSource = serviceReferenceOptional.get();
            }
        } catch (InvalidSyntaxException e) {
            log.error(e.getMessage(), e);
        }
        if (osgiDataSource == null)
            throw new WaterRuntimeException("No transaction control found!");

        return ctx.getService(osgiDataSource);
    }

    private <R> R manageTransaction(Transactional.TxType txType, Function<EntityManager, R> function) throws SystemException, InvalidTransactionException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        jakarta.transaction.UserTransaction userTransaction = UserTransaction.userTransaction();
        TransactionManager transactionManager = com.arjuna.ats.jta.TransactionManager.transactionManager();
        Transaction suspendedTransaction = null;
        if((txType == Transactional.TxType.REQUIRED || txType == Transactional.TxType.REQUIRES_NEW || txType == Transactional.TxType.MANDATORY) && userTransaction.getStatus() == Status.STATUS_MARKED_ROLLBACK || userTransaction.getStatus() == Status.STATUS_ROLLEDBACK || userTransaction.getStatus() == Status.STATUS_ROLLING_BACK){
            userTransaction.rollback();
            transactionManager.begin();
        }
        try {
            switch (txType) {
                case REQUIRED:
                    if (userTransaction.getStatus() != Status.STATUS_ACTIVE) {
                        userTransaction.begin();
                    }
                    break;
                case REQUIRES_NEW:
                    if (userTransaction.getStatus() == Status.STATUS_ACTIVE) {
                        suspendedTransaction = transactionManager.suspend();
                    }
                    userTransaction.begin();
                    break;
                case MANDATORY:
                    if (userTransaction.getStatus() != Status.STATUS_ACTIVE) {
                        throw new IllegalStateException("No active transaction");
                    }
                    break;
                case SUPPORTS:
                    // No action required; if a transaction is active, it will be used
                    break;
                case NOT_SUPPORTED:
                    if (userTransaction.getStatus() == Status.STATUS_ACTIVE) {
                        suspendedTransaction = transactionManager.suspend();
                    }
                    break;
                case NEVER:
                    if (userTransaction.getStatus() == Status.STATUS_ACTIVE) {
                        throw new IllegalStateException("Transaction context exists");
                    }
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported transaction type: " + txType);
            }

            if (userTransaction.getStatus() == Status.STATUS_ACTIVE && txType != Transactional.TxType.NOT_SUPPORTED && txType != Transactional.TxType.NEVER) {
                getEntityManager().joinTransaction();
            }

            R result = function.apply(getEntityManager());

            if (userTransaction.getStatus() == Status.STATUS_ACTIVE &&
                    (txType == Transactional.TxType.REQUIRED || txType == Transactional.TxType.REQUIRES_NEW)) {
                userTransaction.commit();
            }

            return result;
        } catch (Exception e) {
            if (userTransaction.getStatus() == Status.STATUS_ACTIVE) {
                userTransaction.rollback();
            }
            throw e;
        } finally {
            if (suspendedTransaction != null) {
                transactionManager.resume(suspendedTransaction);
            }
        }
    }

}

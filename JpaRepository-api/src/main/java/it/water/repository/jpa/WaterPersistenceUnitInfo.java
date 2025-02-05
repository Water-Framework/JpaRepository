package it.water.repository.jpa;

import it.water.core.api.model.BaseEntity;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import jakarta.persistence.spi.PersistenceUnitTransactionType;
import lombok.Getter;
import lombok.Setter;

import javax.sql.DataSource;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * @Author Aristide Cittadino
 * Persistence Unit Info in order to avoid the use of persistence xml.
 * This class allows also to load persistence unit based on the environment.
 */
public class WaterPersistenceUnitInfo implements PersistenceUnitInfo {

    private final String persistenceUnitName;
    private final List<String> managedClassNames = new ArrayList<>();
    private final Properties properties = new Properties();

    @Getter
    private DataSource jtaDataSource;
    @Getter
    private DataSource nonJtaDataSource;
    @Getter
    private PersistenceUnitTransactionType transactionType;
    private String persisnteceProviderClassName;
    @Setter
    @Getter
    private SharedCacheMode sharedCacheMode = SharedCacheMode.NONE;
    @Setter
    @Getter
    private ValidationMode validationMode = ValidationMode.NONE;
    @Getter
    @Setter
    private ClassLoader classLoader;
    @Getter
    private URL persistenceUnitRootUrl;

    public WaterPersistenceUnitInfo(String persistenceUnitName, Class<? extends BaseEntity> type) {
        this(persistenceUnitName, type, "org.hibernate.jpa.HibernatePersistenceProvider", PersistenceUnitTransactionType.RESOURCE_LOCAL, null, null, null);
    }

    public WaterPersistenceUnitInfo(String persistenceUnitName, Class<? extends BaseEntity> type, String persistenceProviderClassName, PersistenceUnitTransactionType transactionType, DataSource jtaDataSource, DataSource nonJtaDataSource, Properties properties) {
        if (type == null)
            throw new IllegalArgumentException("type must not be null");
        this.persistenceUnitName = persistenceUnitName;
        this.jtaDataSource = jtaDataSource;
        this.nonJtaDataSource = nonJtaDataSource;
        this.transactionType = transactionType;
        this.persisnteceProviderClassName = persistenceProviderClassName;
        this.classLoader = Thread.currentThread().getContextClassLoader();
        this.persistenceUnitRootUrl = type.getProtectionDomain().getCodeSource().getLocation();
        if (properties != null)
            this.properties.putAll(properties);
        setProperty("hibernate.hbm2ddl.auto", "update");
        setProperty("hibernate.archive.autodetection", "class");
    }

    public void addManagedClass(String className) {
        managedClassNames.add(className);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    @Override
    public String getPersistenceUnitName() {
        return persistenceUnitName;
    }

    @Override
    public String getPersistenceProviderClassName() {
        return persisnteceProviderClassName;
    }

    @Override
    public List<String> getMappingFileNames() {
        return Collections.emptyList();
    }

    @Override
    public List<URL> getJarFileUrls() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getManagedClassNames() {
        return managedClassNames;
    }

    @Override
    public boolean excludeUnlistedClasses() {
        return false;
    }

    @Override
    public Properties getProperties() {
        return properties;
    }

    @Override
    public String getPersistenceXMLSchemaVersion() {
        return "2.2";
    }

    @Override
    public void addTransformer(ClassTransformer transformer) {
        //no implementation
    }

    @Override
    public ClassLoader getNewTempClassLoader() {
        return getClassLoader();
    }

}

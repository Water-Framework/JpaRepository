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

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.model.PaginableResult;
import it.water.core.api.registry.ComponentRegistration;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.repository.query.Query;
import it.water.core.bundle.PropertiesNames;
import it.water.core.model.exceptions.ValidationException;
import it.water.core.registry.model.ComponentConfigurationFactory;
import it.water.implementation.spring.interceptors.SpringServiceInterceptor;
import it.water.implementation.spring.util.filter.SpringComponentFilterBuilder;
import it.water.repository.entity.model.exceptions.NoResultException;
import it.water.repository.jpa.api.JpaRepositoryManager;
import it.water.repository.jpa.spring.bundle.api.JpaTestEntityRepository;
import it.water.repository.jpa.spring.bundle.api.ServiceInterface;
import it.water.repository.jpa.spring.bundle.api.TestEntitySystemApi;
import it.water.repository.jpa.spring.bundle.api.TestEntityWaterRepo;
import it.water.repository.jpa.spring.bundle.persistence.entity.TestEntity;
import it.water.repository.jpa.spring.bundle.service.SampleService;
import it.water.repository.jpa.spring.bundle.service.ServiceInterfaceImpl2;
import it.water.repository.jpa.spring.bundle.service.ServiceInterfaceImpl3;
import it.water.repository.jpa.spring.bundle.service.SpringSystemServiceApi;
import jakarta.transaction.Transactional;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
@SpringBootTest()
@ActiveProfiles("test")
@ContextConfiguration(classes = TestConfiguration.class)
class SpringApplicationTest {
    private static final String FIELD1_NAME = "field1";
    private static final String FIELD2_NAME = "field2";
    private static final String FIELD_VALUE = "field1New";
    @Autowired
    ComponentRegistry waterComponentRegistry;
    @Autowired
    SpringServiceInterceptor springServiceInterceptor;
    //Entity Repository is also WaterBase Repository
    @Autowired
    JpaTestEntityRepository entityRepository;
    @Autowired
    TestEntitySystemApi entitySystemApi;
    @Autowired
    SampleService serviceTest;
    @Autowired
    ApplicationProperties waterApplicationProperties;
    @Autowired
    SpringSystemServiceApi springSystemServiceApi;
    @Autowired
    JpaRepositoryManager springJpaRepositoryManager;
    @Autowired
    TestEntityWaterRepo testEntityWaterRepo;

    @Test
    void initSpringApplication() {
        assertNotNull(waterComponentRegistry);
        assertNotNull(springServiceInterceptor);
        assertNotNull(entityRepository);
    }

    /**
     * This test checks if interceptors work correctly
     * since in serviceTest we inject a core componet using WaterInject annotation
     * which is managed by interceptors.
     * So if it is <> null then interceptors works correctly, generically.
     */
    @Test
    void testInterceptors() {
        assertNotNull(serviceTest);
        assertNotNull(serviceTest.getRegistry());
    }

    /**
     * This test checks wether the component registry orders or not all registered componente using the component registry
     */
    @Test
    void testPriorityAndUnregistering() {
        List<ServiceInterface> services = waterComponentRegistry.findComponents(ServiceInterface.class, null);
        assertEquals(3, services.size());
        ServiceInterface sr = waterComponentRegistry.findComponent(ServiceInterface.class, null);
        //find component should return the one with highest priority
        assertTrue(sr instanceof ServiceInterfaceImpl3);
        //testing injection in framework components
        assertNotNull(((ServiceInterfaceImpl3) sr).getComponentRegistry());
        waterComponentRegistry.unregisterComponent(ServiceInterface.class, services.get(0));
        services = waterComponentRegistry.findComponents(ServiceInterface.class, null);
        assertEquals(2, services.size());
        assertInstanceOf(ServiceInterfaceImpl2.class, services.get(0));
    }

    /**
     * Testing saving methods on spring crud repo.
     */
    @Test
    @Transactional
    void testSpringRepository() {
        TestEntity testEntity = new TestEntity(FIELD1_NAME, FIELD2_NAME);
        //testing persist on water repository
        entityRepository.save(testEntity);
        //repository should be a base repository
        assertTrue(testEntity.getId() > 0);
        assertEquals(FIELD1_NAME, testEntity.getField1());
        assertEquals(FIELD2_NAME, testEntity.getField2());
        Query query = entityRepository.getQueryBuilderInstance().field(FIELD1_NAME).equalTo(FIELD1_NAME);
        TestEntity foundWithWaterBaseRepo = entityRepository.find(query);
        TestEntity foundEntityWithSpringQuery = entityRepository.findByField2(FIELD2_NAME).get(0);
        assertEquals(testEntity, foundEntityWithSpringQuery);
        assertEquals(testEntity, foundWithWaterBaseRepo);
        foundEntityWithSpringQuery.setField1(FIELD_VALUE);
        testEntity = entityRepository.save(foundEntityWithSpringQuery);
        assertEquals(FIELD_VALUE, testEntity.getField1());
    }

    @Test
    @Transactional
    void advancedSpringRepositoryTest() {
        long initialSize = this.entityRepository.countAll(null);
        List<TestEntity> toSave = this.entityRepository.saveAll(createTestEntitiesList(0, 10));
        this.entityRepository.saveAll(toSave);
        List<TestEntity> all = this.entityRepository.findAll();
        assertEquals(initialSize + 10, all.size());
        TestEntity entity = this.entityRepository.find("field1 = " + FIELD1_NAME + "-1");
        assertNotNull(entity);
        this.entityRepository.delete(entity);
        assertThrows(NoResultException.class, () -> this.entityRepository.find("field1 = " + FIELD1_NAME + "-1"));
        initialSize--;
        PaginableResult<TestEntity> result = this.entityRepository.findAll(40, 1, null, null);
        Assertions.assertEquals(initialSize + 10, result.getResults().size());
        ArrayList<Long> ids = new ArrayList<>();
        ids.add(1L);
        Assertions.assertEquals(1, this.entityRepository.findAllById(ids).size());
        Assertions.assertEquals(this.entityRepository.count(), this.entityRepository.countAll(null));
        this.entityRepository.deleteById(1L);
        Assertions.assertEquals(0, this.entityRepository.findAllById(ids).size());
        Assertions.assertFalse(this.entityRepository.existsById(1L));
        ids.clear();
        Assertions.assertTrue(this.entityRepository.existsById(4L));
        ids.add(4L);
        Assertions.assertTrue(this.entityRepository.existsById(5L));
        ids.add(5L);
        Assertions.assertTrue(this.entityRepository.existsById(6L));
        ids.add(6L);
        this.entityRepository.deleteAllById(ids);
        Assertions.assertEquals(0, this.entityRepository.findAllById(ids).size());
        this.entityRepository.deleteAll();
        Assertions.assertEquals(0, this.entityRepository.findAll().size());
        toSave = this.entityRepository.saveAll(createTestEntitiesList(20, 10));
        this.entityRepository.saveAll(toSave);
        this.entityRepository.deleteAll(toSave);
        Assertions.assertEquals(0, this.entityRepository.findAll().size());
        toSave = this.entityRepository.saveAll(createTestEntitiesList(30, 10));
        this.entityRepository.saveAll(toSave);
        this.entityRepository.removeAll();
        Assertions.assertEquals(0, this.entityRepository.findAll().size());
        toSave = this.entityRepository.saveAll(createTestEntitiesList(30, 10));
        this.entityRepository.saveAll(toSave);
        this.entityRepository.removeAll(toSave);
        Assertions.assertEquals(0, this.entityRepository.findAll().size());
        toSave = this.entityRepository.saveAll(createTestEntitiesList(30, 10));
        this.entityRepository.saveAll(toSave);
        this.entityRepository.remove(toSave.get(0).getId());
        Assertions.assertFalse(this.entityRepository.existsById(toSave.get(0).getId()));
        this.entityRepository.remove(toSave.get(1));
        Assertions.assertFalse(this.entityRepository.existsById(toSave.get(1).getId()));
        ids.clear();
        ids.add(toSave.get(3).getId());
        ids.add(toSave.get(4).getId());
        this.entityRepository.removeAllByIds(ids);
        Assertions.assertFalse(this.entityRepository.existsById(toSave.get(3).getId()));
        Assertions.assertFalse(this.entityRepository.existsById(toSave.get(4).getId()));
    }

    /**
     * Testing save process using water componenents.
     */
    @Test
    @Transactional
    void testWaterBaseService() {
        TestEntity testEntity = new TestEntity(FIELD1_NAME, FIELD2_NAME);
        //using save method of wtf base repository
        entitySystemApi.save(testEntity);
        Query query = entityRepository.getQueryBuilderInstance().field(FIELD1_NAME).equalTo(FIELD1_NAME);
        TestEntity foundWithWaterBaseRepo = entitySystemApi.find(query);
        assertEquals(testEntity, foundWithWaterBaseRepo);
        testEntity.setField1(FIELD_VALUE);
        entitySystemApi.update(testEntity);
        assertEquals(FIELD_VALUE, testEntity.getField1());
    }

    @Test
    void checkLoadedProperties() {
        assertNotNull(waterApplicationProperties);
        assertEquals("true", waterApplicationProperties.getProperty(PropertiesNames.HYPERIOT_TEST_MODE));
    }

    @Test
    void testComponentFilter() {
        SpringComponentFilterBuilder componentFilterBuilder = new SpringComponentFilterBuilder();
        ComponentFilter filter = componentFilterBuilder.createFilter("filter", "value");
        ServiceInterface serviceInterface = waterComponentRegistry.findComponent(ServiceInterface.class, filter);
        assertNotNull(serviceInterface);
        Assertions.assertEquals("FILTERED BEAN!", serviceInterface.doThing());
        ComponentFilter andFilter = filter.and(componentFilterBuilder.createFilter("filter1", "value1"));
        Assertions.assertEquals("(&(filter=value)(filter1=value1))", andFilter.getFilter());
        Assertions.assertEquals("(!(&(filter=value)(filter1=value1)))", andFilter.not().getFilter());
        ComponentFilter orFilter = filter.or(componentFilterBuilder.createFilter("filter1", "value1"));
        Assertions.assertEquals("(|(filter=value)(filter1=value1))", orFilter.getFilter());
        Assertions.assertEquals("(!(|(filter=value)(filter1=value1)))", orFilter.not().getFilter());
        Assertions.assertEquals("(!(filter=value))", filter.not().getFilter());
    }

    @Test
    void testEntityValidation() {
        String maliutiousField = "<script>alert('ciao')</script>";
        TestEntity testEntity = new TestEntity(maliutiousField, FIELD2_NAME);
        //using save method of wtf base repository
        assertThrows(ValidationException.class, () -> entitySystemApi.save(testEntity));
        assertThrows(ValidationException.class, () -> springSystemServiceApi.elaborateResource(testEntity));
    }

    @Test
    void testWaterComponentRegistry() {
        ServiceInterface customComponent = new ServiceInterfaceImpl3();
        ComponentRegistration<ServiceInterface, String> registration = this.waterComponentRegistry.registerComponent(ServiceInterface.class, customComponent, ComponentConfigurationFactory.createNewComponentPropertyFactory().withPriority(4).build());
        assertNotNull(registration);
        assertNotNull(registration.getConfiguration());
        Assertions.assertEquals(ServiceInterface.class, registration.getRegistrationClass());
        assertDoesNotThrow(() -> this.waterComponentRegistry.unregisterComponent(registration));
    }

    @Test
    void testSpringApplicationProperties() {
        File customPropFile = new File("src/spring/test/resources/custom-props.properties");
        assertThrows(UnsupportedOperationException.class, () -> this.waterApplicationProperties.loadProperties(customPropFile));
        Properties customProps2 = new Properties();
        customProps2.put("customFromCode", "value");
        assertThrows(UnsupportedOperationException.class, () -> this.waterApplicationProperties.loadProperties(customProps2));
        assertThrows(UnsupportedOperationException.class, () -> this.waterApplicationProperties.unloadProperties(customPropFile));
        assertThrows(UnsupportedOperationException.class, () -> this.waterApplicationProperties.unloadProperties(customProps2));
    }

    @Test
    void testFindEntitySystemApi() {
        Assertions.assertNotNull(waterComponentRegistry.findEntitySystemApi(TestEntity.class.getName()));
    }

    @Test
    void testJpaRepositoryConfiguration() {
        Assertions.assertNotNull(new SpringJpaRepositoryConfig());
    }

    @Test
    void testJpaRepositoryManager() {
        Assertions.assertNotNull(springJpaRepositoryManager.createConcreteRepository(TestEntity.class, "default-persistence-unit"));
    }

    @Test
    void testWaterRepository() {
        Assertions.assertNotNull(testEntityWaterRepo);
        Assertions.assertNotNull(testEntityWaterRepo.getEntityManager());
        Assertions.assertNotNull(testEntityWaterRepo.getPersistenceUnit());
        Assertions.assertTrue(testEntityWaterRepo.isEntityManagerNotNull());
        Assertions.assertEquals(TestEntity.class.getName(), testEntityWaterRepo.getClassTypeName());
        TestEntity newEntity = new TestEntity("testWaterRepo1", "testWaterRepo2");
        Assertions.assertDoesNotThrow(() -> testEntityWaterRepo.persist(newEntity));
        Assertions.assertTrue(newEntity.getId() > 0);
        newEntity.setField1("testWaterRepo1Updated");
        Assertions.assertDoesNotThrow(() -> testEntityWaterRepo.update(newEntity));
        TestEntity foundEntity = testEntityWaterRepo.find(newEntity.getId());
        Assertions.assertEquals(newEntity.getField1(), foundEntity.getField1());
        Assertions.assertDoesNotThrow(() -> testEntityWaterRepo.findAll(-1, -1, null, null));
        Query findByFiled1 = testEntityWaterRepo.getQueryBuilderInstance().field("field1").equalTo("testWaterRepo1Updated");
        Assertions.assertNotNull(testEntityWaterRepo.find(findByFiled1));
        Assertions.assertEquals(1, testEntityWaterRepo.countAll(findByFiled1));
        testEntityWaterRepo.tx(Transactional.TxType.REQUIRED, (entityManager -> {
            System.out.println("testTransaction");
            return null;
        }));
        testEntityWaterRepo.txExpr(Transactional.TxType.REQUIRED, (entityManager -> System.out.println("testTransaction")));
        TestEntity toRemove = new TestEntity("toRemove", "toRemove");
        TestEntity toRemove2 = new TestEntity("toRemove2", "toRemove2");
        TestEntity toRemove3 = new TestEntity("toRemove3", "toRemove3");
        TestEntity toRemove4 = new TestEntity("toRemove4", "toRemove4");
        testEntityWaterRepo.persist(toRemove);
        testEntityWaterRepo.persist(toRemove2);
        testEntityWaterRepo.persist(toRemove3);
        testEntityWaterRepo.persist(toRemove4);
        testEntityWaterRepo.remove(toRemove.getId());
        Assertions.assertThrows(NoResultException.class, () -> testEntityWaterRepo.find("field1 = 'toRemove'"));
        Assertions.assertDoesNotThrow(() -> testEntityWaterRepo.find("field1 = 'toRemove2'"));
        List<Long> ids = Lists.list(toRemove2.getId());
        Assertions.assertDoesNotThrow(() -> testEntityWaterRepo.removeAllByIds(ids));
        Assertions.assertThrows(NoResultException.class, () -> testEntityWaterRepo.find("field1 = 'toRemove2'"));
        List<TestEntity> toRemoveList = Lists.list(toRemove3);
        Assertions.assertDoesNotThrow(() -> testEntityWaterRepo.removeAll(toRemoveList));
        Assertions.assertThrows(NoResultException.class, () -> testEntityWaterRepo.find("field1 = 'toRemove3'"));
        testEntityWaterRepo.remove(toRemove4);
        Assertions.assertThrows(NoResultException.class, () -> testEntityWaterRepo.find("field1 = 'toRemove4'"));
    }

    private List<TestEntity> createTestEntitiesList(int feed, int size) {
        ArrayList<TestEntity> toSave = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            TestEntity testEntity = new TestEntity(FIELD1_NAME + "-" + i, FIELD2_NAME + "-" + i);
            toSave.add(testEntity);
        }
        return toSave;
    }
}

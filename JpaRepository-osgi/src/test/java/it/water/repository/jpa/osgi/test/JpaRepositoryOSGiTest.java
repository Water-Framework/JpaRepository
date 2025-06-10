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

package it.water.repository.jpa.osgi.test;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.repository.query.Query;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.osgi.test.bundle.entity.TestEntity;
import it.water.osgi.test.bundle.entity.TestEntitySystemApi;
import it.water.osgi.test.bundle.entity.WaterTestEntityRepository;
import it.water.repository.entity.model.exceptions.NoResultException;
import it.water.repository.jpa.api.JpaRepositoryManager;
import jakarta.transaction.Transactional;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

import java.util.List;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class JpaRepositoryOSGiTest extends KarafTestSupport {

    //force global configuration
    @Override
    public Option[] config() {
        return null;
    }

    @Test
    public void waterFrameworkShouldBeInstalled() {
        // assert on an available service
        assertServiceAvailable(FeaturesService.class, 0);
        String features = executeCommand("feature:list -i");
        assertContains("water-core-features  ", features);
        String datasource = executeCommand("jdbc:ds-list");
        assertContains("water", datasource);
    }


    /**
     * Testing saving methods on spring crud repo.
     */
    @Test
    public void testRepository() {
        TestEntitySystemApi entitySystemApi = getOsgiService(TestEntitySystemApi.class);
        TestEntity testEntity = new TestEntity("field1", "field2");
        //testing persist on water repository
        entitySystemApi.save(testEntity);
        //repository should be a base repository
        Assert.assertTrue(testEntity.getId() > 0);
        Query query = entitySystemApi.getQueryBuilderInstance().field("field1").equalTo("field1");
        TestEntity foundWithBaseRepo = entitySystemApi.find(query);
        Assert.assertNotNull(foundWithBaseRepo);
        foundWithBaseRepo.setField1("field1New");
        entitySystemApi.update(foundWithBaseRepo);
        Assert.assertEquals("field1New", foundWithBaseRepo.getField1());
        Assert.assertEquals("field1New", testEntity.getField1());
        Assert.assertEquals("field2", testEntity.getField2());
    }

    @SuppressWarnings("unused")
    @Test
    public void testFindEntitySystemApi() {
        ComponentRegistry waterComponentRegistry = getOsgiService(ComponentRegistry.class);
        TestEntitySystemApi entitySystemApi = getOsgiService(TestEntitySystemApi.class);
        Assert.assertNotNull(waterComponentRegistry.findEntitySystemApi(TestEntity.class.getName()));
    }

    @Test
    public void testJpaRepositoryManager() {
        JpaRepositoryManager osgiJpaRepositoryManager = getOsgiService(JpaRepositoryManager.class);
        Assert.assertNotNull(osgiJpaRepositoryManager);
        Assert.assertNotNull(osgiJpaRepositoryManager.createConcreteRepository(TestEntity.class, "water-default-persistence-unit"));
    }

    @Test
    public void testWaterRepository() {
        WaterTestEntityRepository testEntityWaterRepo = getOsgiService(WaterTestEntityRepository.class);
        Assert.assertNotNull(testEntityWaterRepo);
        Assert.assertNotNull(testEntityWaterRepo.getEntityManager());
        Assert.assertNotNull(testEntityWaterRepo.getPersistenceUnit());
        Assert.assertTrue(testEntityWaterRepo.isEntityManagerNotNull());
        Assert.assertEquals(TestEntity.class.getName(), testEntityWaterRepo.getClassTypeName());
        TestEntity newEntity = new TestEntity("testWaterRepo1", "testWaterRepo2");
        testEntityWaterRepo.persist(newEntity);
        Assert.assertTrue(newEntity.getId() > 0);
        newEntity.setField1("testWaterRepo1Updated");
        testEntityWaterRepo.update(newEntity);
        TestEntity foundEntity = testEntityWaterRepo.find(newEntity.getId());
        Assert.assertEquals(newEntity.getField1(), foundEntity.getField1());
        testEntityWaterRepo.findAll(-1, -1, null, null);
        Query findByFiled1 = testEntityWaterRepo.getQueryBuilderInstance().field("field1").equalTo("testWaterRepo1Updated");
        Assert.assertNotNull(testEntityWaterRepo.find(findByFiled1));
        Assert.assertEquals(1, testEntityWaterRepo.countAll(findByFiled1));
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
        try {
            testEntityWaterRepo.find("field1 = 'toRemove'");
        } catch (NoResultException ex) {
        }

        testEntityWaterRepo.find("field1 = 'toRemove2'");
        List<Long> ids = List.of(toRemove2.getId());
        testEntityWaterRepo.removeAllByIds(ids);
        try {
            testEntityWaterRepo.find("field1 = 'toRemove2'");
        } catch (NoResultException ex) {
        }

        List<TestEntity> toRemoveList = List.of(toRemove3);
        testEntityWaterRepo.removeAll(toRemoveList);
        try {
            testEntityWaterRepo.find("field1 = 'toRemove3'");
        } catch (NoResultException ex) {
        }

        testEntityWaterRepo.remove(toRemove4);
        try {
            testEntityWaterRepo.find("field1 = 'toRemove4'");
        } catch (NoResultException ex) {
        }
    }

    @Test
    public void testTransactions(){
        Assert.assertTrue(runTransaction(Transactional.TxType.REQUIRED));
        Assert.assertTrue(runTransaction(Transactional.TxType.NEVER));
        Assert.assertTrue(runTransaction(Transactional.TxType.REQUIRES_NEW));
        Assert.assertTrue(runTransaction(Transactional.TxType.NOT_SUPPORTED));
        try {
            Assert.assertTrue(runTransaction(Transactional.TxType.MANDATORY));
        } catch (WaterRuntimeException e){

        }
    }

    private boolean runTransaction(Transactional.TxType txType){
        WaterTestEntityRepository testEntityWaterRepo = getOsgiService(WaterTestEntityRepository.class);
        return testEntityWaterRepo.tx(txType, (entityManager -> {
            System.out.println("testTransaction");
            return true;
        }));
    }
}

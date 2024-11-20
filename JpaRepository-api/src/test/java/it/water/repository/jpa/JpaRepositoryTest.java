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
package it.water.repository.jpa;

import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.operands.FieldValueOperand;
import it.water.core.api.repository.query.operations.*;
import it.water.repository.entity.model.PaginatedResult;
import it.water.repository.entity.model.exceptions.DuplicateEntityException;
import it.water.repository.entity.model.exceptions.NoResultException;
import it.water.repository.jpa.constraints.DuplicateConstraintValidator;
import it.water.repository.jpa.entity.TestEntity;
import it.water.repository.jpa.query.PredicateBuilder;
import it.water.repository.jpa.repository.TestEntityRepository;
import it.water.repository.query.order.DefaultQueryOrder;
import it.water.repository.query.order.DefaultQueryOrderParameter;
import org.junit.jupiter.api.*;

import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JpaRepositoryTest {
    private TestEntityRepository testEntityRepository;

    @Test
    @Order(0)
    void checkPreconditions() {
        Assertions.assertNotNull(getRepositoryTest());
        Assertions.assertNotNull(getRepositoryTest().getEntityManager());
    }

    @Test
    @Order(1)
    void testEntity() {
        TestEntity entity = new TestEntity();
        entity.setUniqueField("a");
        entity.setCombinedUniqueField1("b");
        entity.setCombinedUniqueField2("c");
        Assertions.assertEquals(0, entity.getId());
        Assertions.assertEquals(1, entity.getEntityVersion());
        Assertions.assertNotNull(entity.getEntityModifyDate());
        Assertions.assertNotNull(entity.getEntityCreateDate());
        getRepositoryTest().getEntityManager().getTransaction().begin();
        getRepositoryTest().persist(entity);
        getRepositoryTest().getEntityManager().getTransaction().commit();
        Assertions.assertTrue(entity.getId() > 0);
        Assertions.assertEquals(1, entity.getEntityVersion());
    }

    //Test entity has 1 unique field , and another unique composite fields
    //this method tests whether repository raises duplicate exception correctly
    @Test
    @Order(2)
    void testDuplicateFails() {
        TestEntity entity = new TestEntity();
        entity.setUniqueField("a");
        TestEntityRepository testRepo = getRepositoryTest();
        Assertions.assertThrows(DuplicateEntityException.class, () -> {
            testRepo.persist(entity);
        });
        //fails if cobined unique key constraint is matched
        entity.setUniqueField("a1");
        entity.setCombinedUniqueField1("b");
        entity.setCombinedUniqueField2("c");
        Assertions.assertThrows(DuplicateEntityException.class, () -> {
            testRepo.persist(entity);
        });
        //pass
        entity.setUniqueField("a1");
        entity.setCombinedUniqueField1("b1");
        entity.setCombinedUniqueField2("c");
        getRepositoryTest().persist(entity);
        Assertions.assertTrue(entity.getId() > 0);
    }

    @Test
    @Order(3)
    void testFind() {
        DefaultQueryOrder order = new DefaultQueryOrder();
        //test descending order
        order.addOrderField("uniqueField", false);
        PaginatedResult<TestEntity> results = getRepositoryTest().findAll(4, 1, null, order);
        Assertions.assertEquals(2, results.getResults().size());
        Assertions.assertEquals("a1", results.getResults().stream().findFirst().get().getUniqueField());
        //test query
        Query filter = getRepositoryTest().getQueryBuilderInstance().field("uniqueField").equalTo("a").or(getRepositoryTest().getQueryBuilderInstance().field("uniqueField").equalTo("a1"));
        Query filter1 = getRepositoryTest().getQueryBuilderInstance().createQueryFilter("uniqueField=a OR uniqueField=a1");
        Assertions.assertNotNull(getRepositoryTest().getQueryBuilderInstance().createQueryFilter("(uniqueField=a)"));
        Assertions.assertEquals(filter.getDefinition(), filter1.getDefinition());
        Assertions.assertNull(getRepositoryTest().getQueryBuilderInstance().createQueryFilter("-@das"));
        results = getRepositoryTest().findAll(10, 1, filter, order);
        Assertions.assertEquals(2, results.getResults().size());

        filter = getRepositoryTest().getQueryBuilderInstance().field("uniqueField").equalTo("a1");
        results = getRepositoryTest().findAll(10, 1, filter, order);
        Assertions.assertEquals(1, results.getResults().size());

        TestEntity specificEntity = getRepositoryTest().find(filter);
        Assertions.assertNotNull(specificEntity);
        Assertions.assertEquals("a1", specificEntity.getUniqueField());
    }

    @Test
    @Order(4)
    void testFindAndUpdate() {
        TestEntity foundEntity = getRepositoryTest().find(getRepositoryTest().getQueryBuilderInstance().field("uniqueField").equalTo("a"));
        //find by id
        Assertions.assertNotNull(getRepositoryTest().find(foundEntity.getId()));
        foundEntity.setUniqueField("a2");
        getRepositoryTest().update(foundEntity);
        Assertions.assertEquals("a2", getRepositoryTest().find(foundEntity.getId()).getUniqueField());
        TestEntityRepository testRepo = getRepositoryTest();
        Query q = getRepositoryTest().getQueryBuilderInstance().field("uniqueField").equalTo("a");
        Assertions.assertThrows(it.water.repository.entity.model.exceptions.NoResultException.class, () -> {
            testRepo.find(q);
        });
    }

    @Test
    @Order(5)
    void testRemoveById() {
        TestEntity newEntity = new TestEntity();
        newEntity.setUniqueField("uniqueField");
        newEntity.setCombinedUniqueField1("uniqueCombined1");
        newEntity.setCombinedUniqueField2("uniqueCombined12");
        getRepositoryTest().persist(newEntity);
        long entityId = newEntity.getId();
        getRepositoryTest().getEntityManager().flush();
        getRepositoryTest().remove(entityId);
        TestEntityRepository testRepo = getRepositoryTest();
        Assertions.assertThrows(it.water.repository.entity.model.exceptions.NoResultException.class, () -> {
            testRepo.find(entityId);
        });
    }

    @Test
    @Order(6)
    void testRemoveEntity() {
        TestEntity newEntity = new TestEntity();
        newEntity.setUniqueField("uniqueField");
        newEntity.setCombinedUniqueField1("uniqueCombined1");
        newEntity.setCombinedUniqueField2("uniqueCombined12");
        getRepositoryTest().persist(newEntity);
        long entityId = newEntity.getId();
        getRepositoryTest().getEntityManager().flush();
        getRepositoryTest().remove(newEntity);
        TestEntityRepository testRepo = getRepositoryTest();
        Assertions.assertThrows(it.water.repository.entity.model.exceptions.NoResultException.class, () -> {
            testRepo.find(entityId);
        });
    }

    @Test
    @Order(7)
    void testRemoveEntityByIds() {
        TestEntity newEntity = new TestEntity();
        newEntity.setUniqueField("uniqueField");
        newEntity.setCombinedUniqueField1("uniqueCombined1");
        newEntity.setCombinedUniqueField2("uniqueCombined12");
        TestEntity newEntity1 = new TestEntity();
        newEntity.setUniqueField("uniqueField1");
        newEntity.setCombinedUniqueField1("uniqueCombined3");
        newEntity.setCombinedUniqueField2("uniqueCombined4");
        getRepositoryTest().persist(newEntity);
        getRepositoryTest().persist(newEntity1);
        getRepositoryTest().getEntityManager().flush();
        long entityId = newEntity.getId();
        long entity1Id = newEntity1.getId();
        List<Long> ids = new ArrayList<>();
        ids.add(entityId);
        ids.add(entity1Id);
        getRepositoryTest().removeAllByIds(ids);
        TestEntityRepository testRepo = getRepositoryTest();
        Assertions.assertThrows(it.water.repository.entity.model.exceptions.NoResultException.class, () -> {
            testRepo.find(entityId);
        });
        Assertions.assertThrows(it.water.repository.entity.model.exceptions.NoResultException.class, () -> {
            testRepo.find(entity1Id);
        });
    }

    @Test
    @Order(8)
    void testRemoveEntities() {
        TestEntity newEntity = new TestEntity();
        newEntity.setUniqueField("uniqueField");
        newEntity.setCombinedUniqueField1("uniqueCombined1");
        newEntity.setCombinedUniqueField2("uniqueCombined12");
        TestEntity newEntity1 = new TestEntity();
        newEntity.setUniqueField("uniqueField1");
        newEntity.setCombinedUniqueField1("uniqueCombined3");
        newEntity.setCombinedUniqueField2("uniqueCombined4");
        getRepositoryTest().persist(newEntity);
        getRepositoryTest().persist(newEntity1);
        getRepositoryTest().getEntityManager().flush();
        long entityId = newEntity.getId();
        long entity1Id = newEntity1.getId();
        List<TestEntity> entities = new ArrayList<>();
        entities.add(newEntity);
        entities.add(newEntity1);
        getRepositoryTest().removeAll(entities);
        TestEntityRepository testRepo = getRepositoryTest();
        Assertions.assertThrows(it.water.repository.entity.model.exceptions.NoResultException.class, () -> {
            testRepo.find(entityId);
        });
        Assertions.assertThrows(NoResultException.class, () -> {
            testRepo.find(entity1Id);
        });
    }

    @Test
    @Order(8)
    void testRemoveAll() {
        getRepositoryTest().removeAll();
        Assertions.assertEquals(0, getRepositoryTest().findAll(1, 1, null, null).getResults().size());
    }

    @Test
    @Order(9)
    void testOrderParameter() {
        DefaultQueryOrderParameter param1 = new DefaultQueryOrderParameter();
        param1.setName("a");
        DefaultQueryOrderParameter param2 = new DefaultQueryOrderParameter();
        param2.setName("a");
        DefaultQueryOrderParameter param3 = new DefaultQueryOrderParameter();
        param3.setName("b");
        Assertions.assertEquals(param1, param2);
        Assertions.assertNotEquals(param1, param3);
        Assertions.assertNotEquals(param2, param3);
    }

    @Test
    @Order(10)
    void testJavaxPredicateGeneration() {
        TestEntityRepository testEntityRepository = getRepositoryTest();
        Root<TestEntity> root = testEntityRepository.getEntityManager().getCriteriaBuilder().createQuery(TestEntity.class).from(TestEntity.class);
        PredicateBuilder<TestEntity> predicateBuilder = new PredicateBuilder<>(root, testEntityRepository.getEntityManager().getCriteriaBuilder().createQuery(TestEntity.class), testEntityRepository.getEntityManager().getCriteriaBuilder());
        NotOperation notOperation = new NotOperation();
        notOperation.defineOperands(testEntityRepository.getQueryBuilderInstance().field("uniqueField").equalTo("a"));
        Assertions.assertEquals("NOT (uniqueField = a)", notOperation.getDefinition());
        Assertions.assertNotNull(predicateBuilder.buildPredicate(notOperation));

        NotEqualTo notEqualToOperation = new NotEqualTo();
        Assertions.assertEquals("NotEqualTo (!=)", notEqualToOperation.getName());
        notEqualToOperation.defineOperands(testEntityRepository.getQueryBuilderInstance().field("uniqueField"), new FieldValueOperand("a"));
        Assertions.assertEquals("uniqueField <> a", notEqualToOperation.getDefinition());
        Assertions.assertNotNull(predicateBuilder.buildPredicate(notEqualToOperation));

        EqualTo equalToOperation = new EqualTo();
        Assertions.assertEquals("EqualTo (=)", equalToOperation.getName());
        equalToOperation.defineOperands(testEntityRepository.getQueryBuilderInstance().field("uniqueField"), new FieldValueOperand("a"));
        Assertions.assertEquals("uniqueField = a", equalToOperation.getDefinition());
        Assertions.assertNotNull(predicateBuilder.buildPredicate(equalToOperation));

        LowerThan lowerThanOperation = new LowerThan();
        lowerThanOperation.defineOperands(getRepositoryTest().getQueryBuilderInstance().field("numberField"), new FieldValueOperand(10));
        Assertions.assertEquals("numberField < 10", lowerThanOperation.getDefinition());
        Assertions.assertNotNull(predicateBuilder.buildPredicate(lowerThanOperation));

        LowerOrEqualThan lowerOrEqualThanOperation = new LowerOrEqualThan();
        lowerOrEqualThanOperation.defineOperands(testEntityRepository.getQueryBuilderInstance().field("numberField"), new FieldValueOperand(10));
        Assertions.assertEquals("numberField <= 10", lowerOrEqualThanOperation.getDefinition());
        Assertions.assertNotNull(predicateBuilder.buildPredicate(lowerOrEqualThanOperation));

        GreaterThan greaterThanOperation = new GreaterThan();
        greaterThanOperation.defineOperands(testEntityRepository.getQueryBuilderInstance().field("numberField"), new FieldValueOperand(10));
        Assertions.assertEquals("numberField > 10", greaterThanOperation.getDefinition());
        Assertions.assertNotNull(predicateBuilder.buildPredicate(greaterThanOperation));

        GreaterOrEqualThan greaterOrEqualThanOperation = new GreaterOrEqualThan();
        greaterOrEqualThanOperation.defineOperands(testEntityRepository.getQueryBuilderInstance().field("numberField"), new FieldValueOperand(10));
        Assertions.assertEquals("numberField >= 10", greaterOrEqualThanOperation.getDefinition());
        Assertions.assertNotNull(predicateBuilder.buildPredicate(greaterOrEqualThanOperation));

        Like likeOperation = new Like();
        likeOperation.defineOperands(testEntityRepository.getQueryBuilderInstance().field("uniqueField"), new FieldValueOperand("a"));
        Assertions.assertEquals("uniqueField LIKE a", likeOperation.getDefinition());
        Assertions.assertNotNull(predicateBuilder.buildPredicate(likeOperation));

        In inOperation = new In();
        inOperation.defineOperands(testEntityRepository.getQueryBuilderInstance().field("uniqueField"), new FieldValueOperand("a"), new FieldValueOperand("b"));
        Assertions.assertEquals("uniqueField IN (a,b)", inOperation.getDefinition());
        Assertions.assertNotNull(predicateBuilder.buildPredicate(inOperation));

    }

    @Test
    @Order(11)
    void testBaseJpaRepositoryConstructors() {
        Assertions.assertDoesNotThrow(() -> new TestEntityRepository(TestEntity.class,"water-default-persistence-unit"));
        Assertions.assertDoesNotThrow(() -> new TestEntityRepository(TestEntity.class,"water-default-persistence-unit", getRepositoryTest().getEntityManager()));
        Assertions.assertDoesNotThrow(() -> new TestEntityRepository(TestEntity.class, getRepositoryTest().getEntityManager()));
        Assertions.assertDoesNotThrow(() -> new TestEntityRepository(TestEntity.class, getRepositoryTest().getEntityManager(),new DuplicateConstraintValidator()));
    }


    private TestEntityRepository getRepositoryTest() {
        if (this.testEntityRepository == null)
            this.testEntityRepository = new TestEntityRepository(TestEntity.class);
        return testEntityRepository;
    }

    private void createAndPersisteExampleEntity(){
        TestEntity alreadyPresentEntity = new TestEntity();
        alreadyPresentEntity.setUniqueField("a");
        alreadyPresentEntity.setCombinedUniqueField1("b");
        alreadyPresentEntity.setCombinedUniqueField2("c");
        getRepositoryTest().getEntityManager().getTransaction().begin();
        getRepositoryTest().persist(alreadyPresentEntity);
        getRepositoryTest().getEntityManager().getTransaction().commit();
    }
}

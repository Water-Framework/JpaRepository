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

package it.water.repository.jpa.test.utils;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import it.water.core.api.service.Service;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.testing.utils.interceptors.TestServiceProxy;
import it.water.core.testing.utils.junit.WaterTestExtension;
import it.water.repository.jpa.api.JpaRepository;
import it.water.repository.jpa.api.JpaRepositoryManager;
import jakarta.transaction.Transactional;
import lombok.Setter;

@Setter
@ExtendWith(WaterTestExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestUtilsTest implements Service {

    @Inject
    private JpaRepositoryManager testJpaRepositoryManager;
    private JpaRepository<TestUtilsEntity> sampleRepo;

    @Test
    @Order(1)
    void testJpaRepositoryManager() {
        Assertions.assertNotNull(testJpaRepositoryManager);
        @SuppressWarnings("rawtypes")
        TestServiceProxy<?> testServiceProxy = (TestServiceProxy)Proxy.getInvocationHandler(testJpaRepositoryManager);
        Assertions.assertTrue(testServiceProxy.getRealService() instanceof TestJpaRepositoryManager);
        sampleRepo = testJpaRepositoryManager.createConcreteRepository(TestUtilsEntity.class, "water-default-persistence-unit");
        Assertions.assertNotNull(sampleRepo);
    }

    @Test
    @Order(2)
    void testTransactions(){
        Assertions.assertDoesNotThrow(() -> sampleRepo.txExpr(Transactional.TxType.REQUIRED,entityManager -> System.out.println("sample transaction")));
        Assertions.assertDoesNotThrow(() -> sampleRepo.tx(Transactional.TxType.REQUIRED,entityManager -> System.out.printf("sample transaction")));
    }
}

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
import it.water.osgi.test.bundle.entity.TestEntity;
import it.water.osgi.test.bundle.entity.TestEntitySystemApi;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

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

    @Test
    public void testFindEntitySystemApi() {
        ComponentRegistry waterComponentRegistry = getOsgiService(ComponentRegistry.class);
        TestEntitySystemApi entitySystemApi = getOsgiService(TestEntitySystemApi.class);
        Assert.assertNotNull(waterComponentRegistry.findEntitySystemApi(TestEntity.class.getName()));
    }

}

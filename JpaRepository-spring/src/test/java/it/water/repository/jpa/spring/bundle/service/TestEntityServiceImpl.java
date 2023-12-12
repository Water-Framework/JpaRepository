
/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */

package it.water.repository.jpa.spring.bundle.service;

import it.water.core.api.permission.SecurityContext;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.BaseEntitySystemApi;
import it.water.repository.jpa.spring.bundle.api.TestEntityApi;
import it.water.repository.jpa.spring.bundle.api.TestEntitySystemApi;
import it.water.repository.jpa.spring.bundle.persistence.entity.TestEntity;
import it.water.repository.service.BaseEntityServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class TestEntityServiceImpl extends BaseEntityServiceImpl<TestEntity> implements TestEntityApi {

    @Autowired
    private TestEntitySystemApi systemService;

    @Autowired
    private ComponentRegistry registry;

    public TestEntityServiceImpl() {
        super(TestEntity.class);
    }

    @Override
    protected TestEntitySystemApi getSystemService() {
        return systemService;
    }

    @Override
    protected ComponentRegistry getComponentRegistry() {
        return registry;
    }

    @Override
    protected SecurityContext getSecurityContext() {
        return null;
    }
}

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

package it.water.repository.jpa.model;

import it.water.core.api.model.BaseEntity;
import it.water.repository.entity.model.AbstractEntity;

import javax.persistence.*;
import java.time.Instant;
import java.util.Date;


/**
 * @Author Aristide Cittadino.
 * This class is a basic implementation of BaseEntity methods.
 */
@MappedSuperclass
@Embeddable
public abstract class AbstractJpaEntity extends AbstractEntity
        implements BaseEntity {

    @Override
    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @Override
    @Version
    @Column(name = "entity_version", columnDefinition = "INTEGER default 1")
    public int getEntityVersion() {
        return entityVersion;
    }

    @Override
    public void setEntityVersion(int entityVersion) {
        this.entityVersion = entityVersion;
    }

    @Override
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "entity_create_date")
    public Date getEntityCreateDate() {
        return entityCreateDate;
    }

    @Override
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "entity_modify_date")
    public Date getEntityModifyDate() {
        return entityModifyDate;
    }

    @Override
    @Transient
    public String getSystemApiClassName() {
        String className = this.getClass().getName();
        return className.replace(".model.", ".api.") + "SystemApi";
    }

    @PreUpdate
    private void preUpdate() {
        this.setEntityModifyDate(new Date(Instant.now().toEpochMilli()));
    }

    @PrePersist
    private void prePersist() {
        long nowMillis = Instant.now().toEpochMilli();
        Date now = new Date(nowMillis);
        this.setEntityModifyDate(now);
        this.setEntityCreateDate(now);
    }

}

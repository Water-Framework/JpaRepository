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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonView;
import it.water.core.api.model.BaseEntity;
import it.water.core.api.model.EntityExtension;
import it.water.core.api.service.rest.WaterJsonView;
import it.water.repository.entity.model.AbstractEntity;
import jakarta.persistence.*;

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
    @JsonView({WaterJsonView.Extended.class, WaterJsonView.Compact.class, WaterJsonView.Internal.class, WaterJsonView.Privacy.class, WaterJsonView.Public.class})
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
    @JsonView({WaterJsonView.Extended.class, WaterJsonView.Compact.class, WaterJsonView.Internal.class, WaterJsonView.Privacy.class, WaterJsonView.Public.class})
    public Integer getEntityVersion() {
        return entityVersion;
    }

    @Override
    public void setEntityVersion(Integer entityVersion) {
        this.entityVersion = entityVersion;
    }

    @Override
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "entity_create_date")
    @JsonView({WaterJsonView.Extended.class, WaterJsonView.Compact.class, WaterJsonView.Internal.class, WaterJsonView.Privacy.class, WaterJsonView.Public.class})
    public Date getEntityCreateDate() {
        return entityCreateDate;
    }

    @Override
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "entity_modify_date")
    @JsonView({WaterJsonView.Extended.class, WaterJsonView.Compact.class, WaterJsonView.Internal.class, WaterJsonView.Privacy.class, WaterJsonView.Public.class})
    public Date getEntityModifyDate() {
        return entityModifyDate;
    }

    @Override
    @Transient
    @JsonIgnore
    public String getResourceName() {
        return super.getResourceName();
    }

    @PreUpdate
    protected void preUpdate() {
        this.setEntityModifyDate(new Date(Instant.now().toEpochMilli()));
        doPreUpdate();
    }

    @PrePersist
    protected void prePersist() {
        long nowMillis = Instant.now().toEpochMilli();
        Date now = new Date(nowMillis);
        this.setEntityModifyDate(now);
        this.setEntityCreateDate(now);
        doPrePersist();
    }

    //can be overridden
    protected void doPrePersist() {
        //do nothing
    }

    //can be overridden
    protected void doPreUpdate() {
        //do nothing
    }

    @Override
    @JsonIgnore
    @Transient
    public boolean isExpandableEntity() {
        return super.isExpandableEntity();
    }

    @JsonIgnore
    @Transient
    @Override
    public EntityExtension getEntityExtension() {
        return super.getEntityExtension();
    }
}

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
import it.water.core.api.model.BaseEntity;
import it.water.core.api.model.EntityExtension;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;


/**
 * @Author Aristide Cittadino.
 * This class is a basic implementation of Jpa Entity Expansion.
 * All fields are ignored by jackson since they are automatically managed by the framework.
 */
@MappedSuperclass
@Embeddable
public abstract class AbstractJpaEntityExpansion extends AbstractJpaEntity
        implements BaseEntity, EntityExtension {
    /**
     * This field should be filled with the primary key of the related entity
     */
    @Getter
    @Setter
    @JsonIgnore
    private long relatedEntityId;

    @Override
    public void setupExtensionFields(long id, BaseEntity baseEntity) {
        this.setId(id);
        this.relatedEntityId = baseEntity.getId();
    }

    @Override
    @Id
    @GeneratedValue
    @JsonIgnore
    public long getId() {
        return super.getId();
    }

    @Override
    @Version
    @Column(name = "entity_version", columnDefinition = "INTEGER default 1")
    @JsonIgnore
    public Integer getEntityVersion() {
        return super.getEntityVersion();
    }

    @Override
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "entity_create_date")
    @JsonIgnore
    public Date getEntityCreateDate() {
        return super.getEntityCreateDate();
    }

    @Override
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "entity_modify_date")
    @JsonIgnore
    public Date getEntityModifyDate() {
        return super.getEntityModifyDate();
    }

}

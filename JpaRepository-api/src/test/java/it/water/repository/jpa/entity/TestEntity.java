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
package it.water.repository.jpa.entity;

import it.water.repository.jpa.model.AbstractJpaExpandableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = "uniqueField"), @UniqueConstraint(columnNames = {"combinedUniqueField1", "combinedUniqueField2"})})
@Getter
@Setter
public class TestEntity extends AbstractJpaExpandableEntity {
    private String uniqueField;
    private Double numberField;
    private String combinedUniqueField1;
    private String combinedUniqueField2;
}

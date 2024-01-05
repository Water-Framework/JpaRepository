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

import it.water.repository.jpa.model.AbstractJpaEntity;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = "uniqueField"), @UniqueConstraint(columnNames = {"combinedUniqueField1", "combinedUniqueField2"})})
public class TestEntity extends AbstractJpaEntity {
    private String uniqueField;
    private String combinedUniqueField1;
    private String combinedUniqueField2;

    public String getUniqueField() {
        return uniqueField;
    }

    public void setUniqueField(String uniqueField) {
        this.uniqueField = uniqueField;
    }

    public String getCombinedUniqueField1() {
        return combinedUniqueField1;
    }

    public void setCombinedUniqueField1(String combinedUniqueField1) {
        this.combinedUniqueField1 = combinedUniqueField1;
    }

    public String getCombinedUniqueField2() {
        return combinedUniqueField2;
    }

    public void setCombinedUniqueField2(String combinedUniqueField2) {
        this.combinedUniqueField2 = combinedUniqueField2;
    }
}

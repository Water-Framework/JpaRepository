package it.water.repository.jpa.entity;

import it.water.repository.jpa.model.AbstractJpaEntityExpansion;
import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Access(AccessType.FIELD)
public class TestEntityDetails extends AbstractJpaEntityExpansion {

    private String extensionField;
    private Integer extensionField2;
}

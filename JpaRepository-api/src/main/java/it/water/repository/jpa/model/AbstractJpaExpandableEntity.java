package it.water.repository.jpa.model;

import com.fasterxml.jackson.annotation.*;
import it.water.core.api.model.EntityExtension;
import it.water.core.api.model.ExpandableEntity;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Aristide Cittadino
 * This class implements the concept of jpa expandable entity
 */
@MappedSuperclass
@Setter
public abstract class AbstractJpaExpandableEntity extends AbstractJpaEntity implements ExpandableEntity {

    private Map<String, Object> extraFields = new HashMap<>();
    private EntityExtension extension;

    @JsonAnySetter
    public void setExtraFields(String key, Object value) {
        extraFields.put(key, value);
    }

    @JsonSetter(nulls = Nulls.AS_EMPTY)
    public void setExtraFields(Map<String, Object> extraFields) {
        this.extraFields = extraFields;
    }

    @Transient
    @JsonAnyGetter
    public Map<String, Object> getExtraFields() {
        return extraFields;
    }

    @JsonIgnore
    @Transient
    public EntityExtension getExtension() {
        return extension;
    }
}

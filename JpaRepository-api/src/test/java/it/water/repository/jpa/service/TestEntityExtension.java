package it.water.repository.jpa.service;

import it.water.core.api.model.BaseEntity;
import it.water.core.api.service.EntityExtensionService;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.repository.jpa.entity.TestEntity;
import it.water.repository.jpa.entity.TestEntityDetails;
import lombok.Getter;
import lombok.Setter;

/**
 * @Author Aristide Cittdino
 * Registering TestEntityExtension as an extension of TestEntity
 */
@FrameworkComponent(properties = EntityExtensionService.RELATED_ENTITY_PROPERTY + "=it.water.repository.jpa.entity.TestEntity")
public class TestEntityExtension implements EntityExtensionService {

    @Getter
    @Setter
    private String waterEntityExtensionType;

    @Override
    public Class<? extends BaseEntity> relatedType() {
        return TestEntity.class;
    }

    @Override
    public Class<? extends BaseEntity> type() {
        return TestEntityDetails.class;
    }
}

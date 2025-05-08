package org.qubership.integration.platform.engine.service.deployment.processing.actions.context.create;

import lombok.extern.slf4j.Slf4j;
import org.apache.camel.spring.SpringCamelContext;
import org.qubership.integration.platform.engine.camel.context.storage.ContextStorageRepository;
import org.qubership.integration.platform.engine.model.ChainElementType;
import org.qubership.integration.platform.engine.model.constants.CamelConstants.ChainProperties;
import org.qubership.integration.platform.engine.model.deployment.update.DeploymentInfo;
import org.qubership.integration.platform.engine.model.deployment.update.ElementProperties;
import org.qubership.integration.platform.engine.service.deployment.processing.ElementProcessingAction;
import org.qubership.integration.platform.engine.service.deployment.processing.qualifiers.OnAfterDeploymentContextCreated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@OnAfterDeploymentContextCreated
@ConditionalOnProperty(value = "qip.context.storage.enabled", havingValue = "true", matchIfMissing = true)
public class ContextStorageDependencyBinder extends ElementProcessingAction {
    private final ContextStorageRepository contextStorageRepository;

    @Autowired
    public ContextStorageDependencyBinder(
            ContextStorageRepository contextStorageRepository
    ) {
        this.contextStorageRepository = contextStorageRepository;
    }

    public ContextStorageRepository getRepository() {
        return contextStorageRepository;
    }

    @Override
    public boolean applicableTo(ElementProperties properties) {
        String elementType = properties.getProperties().get(ChainProperties.ELEMENT_TYPE);
        ChainElementType chainElementType = ChainElementType.fromString(elementType);
        return (
                ChainElementType.CONTEXT_STORAGE.equals(chainElementType)
        );
    }

    @Override
    public void apply(SpringCamelContext context, ElementProperties properties, DeploymentInfo deploymentInfo) {
        String elementId = properties.getElementId();
        context.getRegistry().bind(elementId, contextStorageRepository);
    }


}

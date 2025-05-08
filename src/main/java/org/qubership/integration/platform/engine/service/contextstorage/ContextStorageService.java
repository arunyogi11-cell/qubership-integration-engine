package org.qubership.integration.platform.engine.service.contextstorage;

import org.qubership.integration.platform.engine.camel.context.storage.ContextStorageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ContextStorageService {
    private final ContextStorageRepository contextStorageRepository;

    @Autowired
    public ContextStorageService(
            ContextStorageRepository contextStorageRepository
    ) {
        this.contextStorageRepository = contextStorageRepository;
    }

    public void storeValue(String key, String value) {
        contextStorageRepository.add(key, value);
    }

    public String getValue(String key) {
        return contextStorageRepository.get(key);
    }

    public String deleteValue(String key) {
        return contextStorageRepository.delete(key);
    }

}


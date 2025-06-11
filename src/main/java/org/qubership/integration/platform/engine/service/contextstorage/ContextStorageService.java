/*
 * Copyright 2024-2025 NetCracker Technology Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

package org.qubership.integration.platform.engine.service.contextstorage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.integration.platform.engine.persistence.shared.entity.ContextStorage;
import org.qubership.integration.platform.engine.persistence.shared.repository.ContextStorageRespository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ContextStorageService {
    private final ContextStorageRespository contextStorageRepository;
    private static final String CONTEXT = "context";

    private static final String CREATED_AT = "createdAt";

    @Autowired
    public ContextStorageService(
            ContextStorageRespository contextStorageRepository
    ) {
        this.contextStorageRepository = contextStorageRepository;
    }

    public void storeValue(String contextKey, String contextValue, String generatedKey, long ttl) {
        ContextData existingContext = contextKeyExits(contextKey, contextValue, generatedKey);
        ContextStorage contextStorage = ContextStorage.builder()
                .key(generatedKey)
                .value(new ObjectMapper().convertValue(existingContext, JsonNode.class))
                .validUntil(LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).plusSeconds(ttl))
                .build();
        contextStorageRepository.save(contextStorage);
    }

    private ContextData contextKeyExits(String contextKey, String contextValue, String generatedKey) {
        return contextStorageRepository.findById(generatedKey)
                .map(existingStorage -> {
                    JsonNode existingContext = existingStorage.getValue();
                    Map<String, String> updatedContext = new HashMap<>();
                    if (existingContext != null) {
                        JsonNode contextNode = existingContext.get(CONTEXT);
                        contextNode.fields().forEachRemaining(entry ->
                                updatedContext.put(entry.getKey(), entry.getKey().equals(contextKey) ? contextValue : entry.getValue().asText())
                        );
                        updatedContext.putIfAbsent(contextKey, contextValue);
                    } else {
                        updatedContext.put(contextKey, contextValue);
                    }
                    return ContextData.builder()
                            .createdAt(existingContext != null && existingContext.has(CREATED_AT) ? existingContext.get(CREATED_AT).asLong() : Instant.now().getEpochSecond())
                            .updatedAt(Instant.now().getEpochSecond())
                            .context(updatedContext)
                            .build();
                })
                .orElseGet(() -> createNewContext(contextKey, contextValue));
    }

    private static ContextData createNewContext(String key, String value) {
        Map<String, String> updatedContext = new HashMap<>();
        updatedContext.put(key, value);
        return ContextData.builder()
                .createdAt(Instant.now().getEpochSecond())
                .updatedAt(Instant.now().getEpochSecond())
                .context(updatedContext)
                .build();
    }

    public List<String> getValue(String generatedKey, List<String> keys) {
        Object jsonValue = contextStorageRepository.findById(generatedKey)
                .map(ContextStorage::getValue)
                .orElse(null);

        if (jsonValue != null) {
            try {
                JsonNode jsonNode = new ObjectMapper().readTree(jsonValue.toString());
                JsonNode contextNode = jsonNode.get(CONTEXT);
                if (contextNode != null) {
                    return keys.stream()
                            .map(key -> contextNode.has(key) ? contextNode.get(key).asText() : null)
                            .toList();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public void deleteValue(String generatedKey) {
        contextStorageRepository.deleteById(generatedKey);
    }

}


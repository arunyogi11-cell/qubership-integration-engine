package org.qubership.integration.platform.engine.service.contextstorage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContextStorage {
    private String sessionId;
    private Map<String, String> context;
    private ContextInfo contextInfo;
}


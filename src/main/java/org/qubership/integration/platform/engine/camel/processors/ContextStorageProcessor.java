package org.qubership.integration.platform.engine.camel.processors;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.qubership.integration.platform.engine.model.constants.CamelConstants;
import org.qubership.integration.platform.engine.service.contextstorage.ContextInfo;
import org.qubership.integration.platform.engine.service.contextstorage.ContextStorage;
import org.qubership.integration.platform.engine.service.contextstorage.ContextStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;

import static org.qubership.integration.platform.engine.camel.CorrelationIdSetter.CORRELATION_ID;

@Slf4j
@Component
public class ContextStorageProcessor implements Processor {

    enum Operation {
        GET,
        SET,
        DELETE
    }

    enum Target {
        HEADER,
        PROPERTY,
        BODY
    }

    private static final String SESSION_CONTEXT_PROPERTY_PREFIX = CamelConstants.INTERNAL_PROPERTY_PREFIX + "sessionContext_";
    private static final String PROPERTY_USE_CORRELATION_ID = SESSION_CONTEXT_PROPERTY_PREFIX + "useCorrelationId";
    private static final String PROPERTY_SESSION_ID = SESSION_CONTEXT_PROPERTY_PREFIX + "sessionId";
    private static final String PROPERTY_OPERATION = SESSION_CONTEXT_PROPERTY_PREFIX + "operation";
    private static final String PROPERTY_KEY = SESSION_CONTEXT_PROPERTY_PREFIX + "key";
    private static final String PROPERTY_VALUE = SESSION_CONTEXT_PROPERTY_PREFIX + "value";
    private static final String PROPERTY_TTL = SESSION_CONTEXT_PROPERTY_PREFIX + "ttl";
    private static final String PROPERTY_KEYS = SESSION_CONTEXT_PROPERTY_PREFIX + "keys";
    private static final String PROPERTY_TARGET = SESSION_CONTEXT_PROPERTY_PREFIX + "target";
    private static final String PROPERTY_TARGET_NAME = SESSION_CONTEXT_PROPERTY_PREFIX + "targetName";
    private static final String PROPERTY_UNWRAP = SESSION_CONTEXT_PROPERTY_PREFIX + "unwrap";
    private static final String  PROPERTY_CONNECT_TIMEOUT = SESSION_CONTEXT_PROPERTY_PREFIX + "connectTimeout";

    private final ContextStorageService contextStorageService;

    private final ObjectMapper objectMapper;

    @Autowired
    public ContextStorageProcessor(
            ContextStorageService contextStorageService,
            ObjectMapper objectMapper
    ) {
        this.contextStorageService = contextStorageService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String sessionId = getContextSessionId(exchange);
        Operation operation = readEnumValue(exchange, PROPERTY_OPERATION, Operation.class);
        switch (operation) {
            case GET -> processGetValue(exchange, sessionId);
            case SET -> processSetValue(exchange, sessionId);
            case DELETE -> deleteSessionContext(exchange, sessionId);
        }
    }

    private void processGetValue(Exchange exchange, String sessionId) throws Exception {
        String filter = exchange.getProperty(PROPERTY_KEYS, String.class);
        Target target = readEnumValue(exchange, PROPERTY_TARGET, Target.class);
        String name = exchange.getProperty(PROPERTY_TARGET_NAME, String.class);
        boolean unwrap = isSingleKey(filter) && exchange.getProperty(PROPERTY_UNWRAP, Boolean.class);
        Duration connectTimeout = getConnectTimeout(exchange);

        ContextStorage contextStorage = contextStorageService.getSessionContextById(sessionId, filter, false, connectTimeout);

        String value = unwrap
                ? String.valueOf(contextStorage.getContext().get(filter))
                : objectMapper.writeValueAsString(contextStorage.getContext());

        switch (target) {
            case BODY -> exchange.getMessage().setBody(value);
            case HEADER -> exchange.getMessage().setHeader(name, value);
            case PROPERTY -> exchange.setProperty(name, value);
        }
    }

    private void processSetValue(Exchange exchange, String sessionId) throws Exception {
        String key = exchange.getProperty(PROPERTY_KEY, String.class);
        String value = exchange.getProperty(PROPERTY_VALUE, String.class);
        long ttl = exchange.getProperty(PROPERTY_TTL, Long.class);
        Duration connectTimeout = getConnectTimeout(exchange);

        long createTimestamp = Instant.now().getEpochSecond();
        long activeTillTimestamp = createTimestamp + ttl;

        ContextStorage sessionContext = ContextStorage.builder()
                .sessionId(sessionId)
                .context(Collections.singletonMap(key, value))
                .contextInfo(ContextInfo.builder()
                        .createTimestamp(createTimestamp)
                        .activeTillTimestamp(activeTillTimestamp)
                        .build())
                .build();
        contextStorageService.createOrUpdateSessionContext(sessionContext, connectTimeout);
    }

    private void deleteSessionContext(Exchange exchange, String sessionId) throws Exception {
        Duration connectTimeout = getConnectTimeout(exchange);
        contextStorageService.deleteSessionContextById(sessionId, connectTimeout);
    }

    private static String getContextSessionId(Exchange exchange) {
        boolean useCorrelationId = exchange.getProperty(PROPERTY_USE_CORRELATION_ID, Boolean.class);
        return useCorrelationId
                ? exchange.getProperty(CORRELATION_ID, String.class)
                : exchange.getProperty(PROPERTY_SESSION_ID, String.class);
    }

    private static boolean isSingleKey(String filter) {
        return !filter.contains(",");
    }

    private static <T extends Enum<T>> T readEnumValue(Exchange exchange, String propertyName, Class<T> cls) {
        return Optional.ofNullable(exchange.getProperty(propertyName, String.class))
                .map(value -> Enum.valueOf(cls, value.toUpperCase()))
                .orElse(null);
    }

    private static Duration getConnectTimeout(Exchange exchange) {
        return Duration.ofMillis(exchange.getProperty(PROPERTY_CONNECT_TIMEOUT, 120000, Long.class));
    }
}


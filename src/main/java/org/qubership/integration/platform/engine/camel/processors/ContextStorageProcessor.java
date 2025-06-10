package org.qubership.integration.platform.engine.camel.processors;



import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.qubership.integration.platform.engine.model.constants.CamelConstants;
import org.qubership.integration.platform.engine.service.contextstorage.ContextStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;



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
    private static final String CONTEXT = "context";
    private static final String PROPERTY_CONTEXT_ID = SESSION_CONTEXT_PROPERTY_PREFIX + "context";
    private static final String PROPERTY_OPERATION = SESSION_CONTEXT_PROPERTY_PREFIX + "operation";
    private static final String PROPERTY_KEY = SESSION_CONTEXT_PROPERTY_PREFIX + "key";
    private static final String PROPERTY_VALUE = SESSION_CONTEXT_PROPERTY_PREFIX + "value";
    private static final String PROPERTY_TTL = SESSION_CONTEXT_PROPERTY_PREFIX + "ttl";
    private static final String PROPERTY_KEYS = SESSION_CONTEXT_PROPERTY_PREFIX + "keys";
    private static final String PROPERTY_TARGET = SESSION_CONTEXT_PROPERTY_PREFIX + "target";
    private static final String PROPERTY_TARGET_NAME = SESSION_CONTEXT_PROPERTY_PREFIX + "targetName";
    private static final String PROPERTY_UNWRAP = SESSION_CONTEXT_PROPERTY_PREFIX + "unwrap";
    private static final String PROPERTY_CONNECT_TIMEOUT = SESSION_CONTEXT_PROPERTY_PREFIX + "connectTimeout";

    private final ContextStorageService contextStorageService;


    @Autowired
    public ContextStorageProcessor(
            ContextStorageService contextStorageService
    ) {
        this.contextStorageService = contextStorageService;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        Operation operation = readEnumValue(exchange, PROPERTY_OPERATION, Operation.class);
        switch (operation) {
            case GET -> processGetValue(exchange);
            case SET -> processSetValue(exchange);
            case DELETE -> deleteSessionContext(exchange);
        }
    }

    private void processGetValue(Exchange exchange) throws Exception {
        List<String> contextKey = Optional.ofNullable(exchange.getProperty(PROPERTY_KEYS, String.class))
                .map(value -> List.of(value.split(",")))
                .orElse(List.of());
        Target target = readEnumValue(exchange, PROPERTY_TARGET, Target.class);
        String name = exchange.getProperty(PROPERTY_TARGET_NAME, String.class);
        String generatedKey = createKey(exchange);
        //boolean unwrap = isSingleKey(key) && exchange.getProperty(PROPERTY_UNWRAP, Boolean.class);
        //Duration connectTimeout = getConnectTimeout(exchange);
        List<String> value = contextStorageService.getValue(generatedKey, contextKey);
        switch (target) {
            case BODY -> exchange.getMessage().setBody(value);
            case HEADER -> exchange.getMessage().setHeader(name, value);
            case PROPERTY -> exchange.setProperty(name, value);
        }
    }

    private void processSetValue(Exchange exchange) throws Exception {
        String contextKey = exchange.getProperty(PROPERTY_KEY, String.class);
        String contextValue = exchange.getProperty(PROPERTY_VALUE, String.class);
        LocalDateTime ttl = LocalDateTime.ofEpochSecond(exchange.getProperty(PROPERTY_TTL, Long.class), 0, ZoneOffset.UTC);
        String generatedKey = createKey(exchange);
        contextStorageService.storeValue(contextKey, contextValue, generatedKey, ttl);
    }

    private void deleteSessionContext(Exchange exchange) throws Exception {
        String generatedKey = createKey(exchange);
        contextStorageService.deleteValue(generatedKey);
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

    private static String createKey(Exchange exchange) {
        String contextServiceId = exchange.getProperty(PROPERTY_CONTEXT_ID, String.class);
        String contextId = exchange.getProperty(PROPERTY_SESSION_ID, String.class);
        return CONTEXT + ":" + contextServiceId + ":" + contextId;
    }


}


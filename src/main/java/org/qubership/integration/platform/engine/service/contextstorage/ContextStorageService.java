package org.qubership.integration.platform.engine.service.contextstorage;

import org.qubership.integration.platform.engine.model.constants.CamelNames;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

@Service
public class ContextStorageService {
    private static final String API_PATH = "/api/v1/sessions";

    private final RestTemplateBuilder restTemplateBuilder;
    private final String sessionContextServiceHost;
    private final int sessionContextServicePort;

    @Autowired
    public ContextStorageService(
            RestTemplateBuilder restTemplateBuilder,
            @Value("${cip.session-context.host}") String sessionContextServiceHost,
            @Value("${cip.session-context.port}") int sessionContextServicePort
    ) {
        this.restTemplateBuilder = restTemplateBuilder;

        this.sessionContextServiceHost = sessionContextServiceHost;
        this.sessionContextServicePort = sessionContextServicePort;
    }

    public ContextStorage createOrUpdateSessionContext(ContextStorage context, Duration connectTimeout) throws Exception {
        return buildRestTemplate(connectTimeout)
                .postForObject(buildUrl().toString(), context, ContextStorage.class);
    }

    public ContextStorage getSessionContextById(
            String sessionId,
            String filter,
            boolean includeSystemInfo,
            Duration connectTimeout
    ) throws Exception {
        String url = buildUrl() + "/" + sessionId;
        Map<String, String> parameters = Map.of(
                "filter", filter,
                "includeSystemInfo", Boolean.toString(includeSystemInfo));
        return buildRestTemplate(connectTimeout).getForObject(url, ContextStorage.class, parameters);
    }

    public void deleteSessionContextById(String sessionId, Duration connectTimeout) throws Exception {
        String url = buildUrl() + "/" + sessionId;
        buildRestTemplate(connectTimeout).delete(url);
    }

    private URL buildUrl() throws MalformedURLException {
        return new URL("http", sessionContextServiceHost, sessionContextServicePort, API_PATH);
    }

    private RestTemplate buildRestTemplate(Duration connectTimeout) {
        return restTemplateBuilder.connectTimeout(connectTimeout)
                .defaultHeader(HttpHeaders.AUTHORIZATION, CamelNames.BEARER_PREFIX)
                .build();
    }

}


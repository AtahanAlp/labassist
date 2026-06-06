package com.labassist.llm;

import com.labassist.config.OllamaProperties;
import java.time.Duration;
import java.util.List;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Thin client over the local Ollama runtime's {@code /api/chat} endpoint.
 *
 * <p>Read timeout is generous because inference runs on CPU here. I/O errors and
 * timeouts propagate so the service layer can record TIMEOUT/FAILURE and return
 * a clear 503.
 */
@Component
public class OllamaClient {

    private static final int CONNECT_TIMEOUT_MS = 5000;

    private final RestClient restClient;
    private final OllamaProperties properties;

    public OllamaClient(OllamaProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
        factory.setReadTimeout((int) Duration.ofSeconds(properties.timeoutSeconds()).toMillis());
        this.restClient = RestClient.builder().baseUrl(properties.baseUrl()).requestFactory(factory).build();
    }

    /** Sends a system+user chat turn and returns the assistant's text. */
    public String chat(String systemPrompt, String userPrompt) {
        OllamaChatRequest request = new OllamaChatRequest(
                properties.model(),
                List.of(new OllamaMessage("system", systemPrompt), new OllamaMessage("user", userPrompt)),
                false,
                new OllamaOptions(properties.temperature()));

        OllamaChatResponse response = restClient.post()
                .uri("/api/chat")
                .body(request)
                .retrieve()
                .body(OllamaChatResponse.class);

        if (response == null || response.message() == null || response.message().content() == null) {
            throw new IllegalStateException("Empty response from Ollama");
        }
        return response.message().content().trim();
    }
}

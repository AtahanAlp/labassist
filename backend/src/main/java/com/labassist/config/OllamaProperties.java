package com.labassist.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the local Ollama LLM runtime used to generate preliminary
 * interpretations of lab reports.
 */
@ConfigurationProperties("labassist.ollama")
public record OllamaProperties(
        String baseUrl,
        String model,
        int timeoutSeconds,
        double temperature) {
}

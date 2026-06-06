package com.labassist.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Response body from Ollama's /api/chat endpoint. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaChatResponse(OllamaMessage message, boolean done) {
}

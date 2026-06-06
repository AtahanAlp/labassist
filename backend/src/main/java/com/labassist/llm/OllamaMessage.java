package com.labassist.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** A chat message for the Ollama /api/chat endpoint. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OllamaMessage(String role, String content) {
}

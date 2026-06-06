package com.labassist.llm;

import java.util.List;

/** Request body for Ollama's /api/chat endpoint (non-streaming). */
public record OllamaChatRequest(
        String model,
        List<OllamaMessage> messages,
        boolean stream,
        OllamaOptions options) {
}

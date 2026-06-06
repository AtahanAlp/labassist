package com.labassist.llm;

/** Generation options passed to Ollama (kept minimal: temperature only). */
public record OllamaOptions(double temperature) {
}

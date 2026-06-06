package com.labassist.common.exception;

/** Thrown when the LLM runtime fails or times out; mapped to HTTP 503. */
public class LlmUnavailableException extends RuntimeException {

    public LlmUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

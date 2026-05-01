package com.smart_desk.demo.ai;

public class AiUnavailableException extends RuntimeException {
    public AiUnavailableException() {
        super("AI provider is not configured — set ANTHROPIC_API_KEY to enable.");
    }
}

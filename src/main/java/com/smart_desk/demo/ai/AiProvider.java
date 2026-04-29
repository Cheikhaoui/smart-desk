package com.smart_desk.demo.ai;

public interface AiProvider {
    CategorizationResult categorize(String title, String description);
}

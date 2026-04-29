package com.smart_desk.demo.ai.claude;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.claude")
public record ClaudeProperties(String apiKey, String model) {}

package com.smart_desk.demo.ai.claude;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_desk.demo.ai.AiProvider;
import com.smart_desk.demo.ai.CategorizationResult;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnExpression("!'${app.ai.claude.api-key:}'.trim().isEmpty()")
@RequiredArgsConstructor
@Slf4j
public class ClaudeAiProvider implements AiProvider {

    private static final String SYSTEM_PROMPT = """
            You are a helpdesk ticket classifier. Classify the ticket into exactly one category.

            Available categories:
            - HARDWARE: physical devices (printer, computer, monitor, keyboard, mouse, scanner)
            - SOFTWARE: applications, OS, installation, crashes, bugs, performance
            - NETWORK: internet, Wi-Fi, VPN, connectivity, DNS, firewall
            - ACCOUNT: login, password, access rights, permissions, SSO, MFA
            - OTHER: anything that does not fit the categories above

            Respond with valid JSON only, no markdown, no explanation:
            {"category":"<CATEGORY>","confidence":<number between 0.0 and 1.0>}
            """;

    private final ClaudeProperties props;
    private final ObjectMapper objectMapper;
    private AnthropicClient client;

    @PostConstruct
    void init() {
        client = AnthropicOkHttpClient.builder()
                .apiKey(props.apiKey())
                .build();
        log.info("Claude AI provider initialized (model={})", props.model());
    }

    @Override
    public CategorizationResult categorize(String title, String description) {
        String userMessage = "Title: " + title + "\nDescription: " +
                (description != null && !description.isBlank() ? description : "(no description)");

        var params = MessageCreateParams.builder()
                .model(Model.of(props.model()))
                .maxTokens(100L)
                .system(SYSTEM_PROMPT)
                .addUserMessage(userMessage)
                .build();

        var response = client.messages().create(params);

        if (response.content().isEmpty()) {
            throw new IllegalStateException("Empty response content from Claude");
        }

        String raw = response.content().get(0).asText().text().strip();
        // Defensive strip of markdown code fences in case the model wraps JSON anyway
        if (raw.startsWith("```")) {
            raw = raw.replaceAll("(?s)^```[a-z]*\\s*", "").replaceAll("\\s*```$", "").strip();
        }

        try {
            return objectMapper.readValue(raw, CategorizationResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Claude categorization response: " + raw, e);
        }
    }
}

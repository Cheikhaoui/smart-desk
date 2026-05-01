package com.smart_desk.demo.ai.claude;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart_desk.demo.ai.AiProvider;
import com.smart_desk.demo.ai.CategorizationResult;
import com.smart_desk.demo.entities.Comment;
import com.smart_desk.demo.entities.Ticket;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnExpression("!'${app.ai.claude.api-key:}'.trim().isEmpty()")
@RequiredArgsConstructor
@Slf4j
public class ClaudeAiProvider implements AiProvider {

    private static final String CATEGORIZE_SYSTEM_PROMPT = """
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

    private static final String REPLY_SYSTEM_PROMPT = """
            You are an experienced IT helpdesk agent composing a reply to a support ticket.
            Guidelines:
            - Be professional, empathetic, and concise
            - Address the specific problem described
            - If status is RESOLVED or CLOSED, confirm the resolution warmly
            - If more information is needed, ask targeted questions
            - Keep the reply under 150 words
            - Do not use placeholder text like [name] or [agent name]
            - Write only the reply body — no subject line, no greeting, no sign-off
            """;

    private static final String SUMMARY_SYSTEM_PROMPT = """
            You are an IT helpdesk assistant. Summarize the following support ticket and its conversation thread.
            Write exactly 2-4 sentences that:
            - Describe the core issue reported
            - Note the current status and any resolution or workaround applied
            - Highlight the most important action taken or information exchanged
            Write in third person. Respond with only the summary text — no labels, no preamble.
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
                .system(CATEGORIZE_SYSTEM_PROMPT)
                .addUserMessage(userMessage)
                .build();

        var response = client.messages().create(params);

        if (response.content().isEmpty()) {
            throw new IllegalStateException("Empty response content from Claude");
        }

        String raw = response.content().get(0).asText().text().strip();
        if (raw.startsWith("```")) {
            raw = raw.replaceAll("(?s)^```[a-z]*\\s*", "").replaceAll("\\s*```$", "").strip();
        }

        try {
            return objectMapper.readValue(raw, CategorizationResult.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse Claude categorization response: " + raw, e);
        }
    }

    @Override
    public String suggestReply(Ticket ticket, List<Comment> comments) {
        var sb = new StringBuilder();
        sb.append("Ticket title: ").append(ticket.getTitle()).append('\n');
        sb.append("Category: ").append(ticket.getCategory() != null ? ticket.getCategory() : "General").append('\n');
        sb.append("Status: ").append(ticket.getStatus()).append('\n');
        sb.append("Description: ").append(
                ticket.getDescription() != null && !ticket.getDescription().isBlank()
                        ? ticket.getDescription() : "(no description)").append('\n');

        if (!comments.isEmpty()) {
            sb.append('\n').append("Thread (").append(comments.size()).append(" messages, oldest first):\n");
            for (Comment c : comments) {
                sb.append('[').append(c.getAuthor().getFullName())
                  .append(" | ").append(c.getAuthor().getRole()).append("]: ")
                  .append(c.getContent()).append('\n');
            }
        }

        sb.append("\nCompose the next agent reply:");

        var params = MessageCreateParams.builder()
                .model(Model.of(props.model()))
                .maxTokens(300L)
                .system(REPLY_SYSTEM_PROMPT)
                .addUserMessage(sb.toString())
                .build();

        var response = client.messages().create(params);

        if (response.content().isEmpty()) {
            throw new IllegalStateException("Empty response content from Claude");
        }
        return response.content().get(0).asText().text().strip();
    }

    @Override
    public String summarizeThread(Ticket ticket, List<Comment> comments) {
        var sb = new StringBuilder();
        sb.append("Ticket title: ").append(ticket.getTitle()).append('\n');
        sb.append("Category: ").append(ticket.getCategory() != null ? ticket.getCategory() : "General").append('\n');
        sb.append("Status: ").append(ticket.getStatus()).append('\n');
        sb.append("Priority: ").append(ticket.getPriority()).append('\n');
        sb.append("Description: ").append(
                ticket.getDescription() != null && !ticket.getDescription().isBlank()
                        ? ticket.getDescription() : "(no description)").append('\n');

        if (!comments.isEmpty()) {
            sb.append('\n').append("Thread (").append(comments.size()).append(" messages, oldest first):\n");
            for (Comment c : comments) {
                sb.append('[').append(c.getAuthor().getFullName())
                  .append(" | ").append(c.getAuthor().getRole()).append("]: ")
                  .append(c.getContent()).append('\n');
            }
        }

        sb.append("\nProvide a concise summary:");

        var params = MessageCreateParams.builder()
                .model(Model.of(props.model()))
                .maxTokens(200L)
                .system(SUMMARY_SYSTEM_PROMPT)
                .addUserMessage(sb.toString())
                .build();

        var response = client.messages().create(params);

        if (response.content().isEmpty()) {
            throw new IllegalStateException("Empty response content from Claude");
        }
        return response.content().get(0).asText().text().strip();
    }
}

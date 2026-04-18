package com.smart_desk.demo.dto;


import com.smart_desk.demo.entities.Ticket;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class TicketDto {

    /** Create request — only what the client should provide */
    public record CreateRequest(
            @NotBlank(message = "Title is required")
            @Size(max = 500, message = "Title too long")
            String title,
            String description,
            Ticket.Priority priority,
            String category
    ) {}

    /** Update request — all fields optional (partial update) */
    public record UpdateRequest(
            @Size(max = 500) String title,
            String description,
            Ticket.Status status,
            Ticket.Priority priority,
            String category,
            UUID assignedToId
    ) {}

    /** Assign an agent to a ticket */
    public record AssignRequest(
            @jakarta.validation.constraints.NotNull UUID agentId
    ) {}

    /** Full response with relationships */
    public record Response(
            UUID id,
            String title,
            String description,
            Ticket.Status status,
            Ticket.Priority priority,
            String category,
            String aiSummary,
            UserDto.Summary createdBy,
            UserDto.Summary assignedTo,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(Ticket t) {
            return new Response(
                    t.getId(),
                    t.getTitle(),
                    t.getDescription(),
                    t.getStatus(),
                    t.getPriority(),
                    t.getCategory(),
                    t.getAiSummary(),
                    UserDto.Summary.from(t.getCreatedBy()),
                    UserDto.Summary.from(t.getAssignedTo()),
                    t.getCreatedAt(),
                    t.getUpdatedAt()
            );
        }
    }

    /** Lightweight summary for list views (no description/comments loaded) */
    public record Summary(
            UUID id,
            String title,
            Ticket.Status status,
            Ticket.Priority priority,
            String category,
            UserDto.Summary assignedTo,
            Instant createdAt
    ) {
        public static Summary from(Ticket t) {
            return new Summary(
                    t.getId(),
                    t.getTitle(),
                    t.getStatus(),
                    t.getPriority(),
                    t.getCategory(),
                    UserDto.Summary.from(t.getAssignedTo()),
                    t.getCreatedAt()
            );
        }
    }
}


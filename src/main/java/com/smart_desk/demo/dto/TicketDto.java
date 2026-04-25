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
    public record TicketResponse(
            UUID id,
            String title,
            String description,
            Ticket.Status status,
            Ticket.Priority priority,
            String category,
            String aiSummary,
            UserDto.UserSummary createdBy,
            UserDto.UserSummary assignedTo,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static TicketResponse from(Ticket t) {
            return new TicketResponse(
                    t.getId(),
                    t.getTitle(),
                    t.getDescription(),
                    t.getStatus(),
                    t.getPriority(),
                    t.getCategory(),
                    t.getAiSummary(),
                    UserDto.UserSummary.from(t.getCreatedBy()),
                    UserDto.UserSummary.from(t.getAssignedTo()),
                    t.getCreatedAt(),
                    t.getUpdatedAt()
            );
        }
    }

    /** Lightweight summary for list views (no description/comments loaded) */
    public record TicketSummary(
            UUID id,
            String title,
            Ticket.Status status,
            Ticket.Priority priority,
            String category,
            UserDto.UserSummary assignedTo,
            Instant createdAt
    ) {
        public static TicketSummary from(Ticket t) {
            return new TicketSummary(
                    t.getId(),
                    t.getTitle(),
                    t.getStatus(),
                    t.getPriority(),
                    t.getCategory(),
                    UserDto.UserSummary.from(t.getAssignedTo()),
                    t.getCreatedAt()
            );
        }
    }
}


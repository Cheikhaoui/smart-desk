package com.smart_desk.demo.notification;

import com.smart_desk.demo.dto.CommentDto;
import com.smart_desk.demo.dto.TicketDto;
import com.smart_desk.demo.entities.Comment;
import com.smart_desk.demo.entities.Ticket;

import java.time.Instant;
import java.util.UUID;

public class NotificationDto {

    /** /topic/tickets — broadcast events */
    public record TicketCreated(String type, TicketDto.TicketSummary ticket, Instant timestamp) {
        public static TicketCreated of(Ticket t) {
            return new TicketCreated("TICKET_CREATED", TicketDto.TicketSummary.from(t), Instant.now());
        }
    }

    public record TicketUpdated(String type, TicketDto.TicketSummary ticket, Instant timestamp) {
        public static TicketUpdated of(Ticket t) {
            return new TicketUpdated("TICKET_UPDATED", TicketDto.TicketSummary.from(t), Instant.now());
        }
    }

    public record TicketDeleted(String type, UUID ticketId, Instant timestamp) {
        public static TicketDeleted of(UUID ticketId) {
            return new TicketDeleted("TICKET_DELETED", ticketId, Instant.now());
        }
    }

    /** /topic/tickets/{id}/comments */
    public record CommentAdded(String type, CommentDto.CommentResponse comment, Instant timestamp) {
        public static CommentAdded of(Comment c) {
            return new CommentAdded("COMMENT_ADDED", CommentDto.CommentResponse.from(c), Instant.now());
        }
    }

    /**
     * /user/queue/notifications — private per-user toasts.
     * oldStatus / newStatus are populated only for STATUS_CHANGED.
     */
    public record UserNotification(
            String type,
            UUID ticketId,
            String ticketTitle,
            String oldStatus,
            String newStatus,
            String message,
            Instant timestamp
    ) {
        public static UserNotification assigned(Ticket t) {
            return new UserNotification("ASSIGNED", t.getId(), t.getTitle(), null, null,
                    "You were assigned to: " + t.getTitle(), Instant.now());
        }

        public static UserNotification unassigned(Ticket t) {
            return new UserNotification("UNASSIGNED", t.getId(), t.getTitle(), null, null,
                    "You were unassigned from: " + t.getTitle(), Instant.now());
        }

        public static UserNotification statusChanged(Ticket t, Ticket.Status oldStatus, Ticket.Status newStatus) {
            return new UserNotification("STATUS_CHANGED", t.getId(), t.getTitle(),
                    oldStatus.name(), newStatus.name(),
                    "Status changed from " + oldStatus + " to " + newStatus + " on: " + t.getTitle(),
                    Instant.now());
        }
    }
}

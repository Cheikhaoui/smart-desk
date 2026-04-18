package com.smart_desk.demo.notification;

import com.smart_desk.demo.dto.TicketDto;
import com.smart_desk.demo.dto.UserDto;
import com.smart_desk.demo.entities.Comment;
import com.smart_desk.demo.entities.Ticket;

import java.time.Instant;
import java.util.UUID;

public class NotificationDto {

    public enum Kind { TICKET_CREATED, TICKET_ASSIGNED, COMMENT_ADDED }

    public record Envelope(
            Kind kind,
            Instant at,
            Object payload
    ) {
        public static Envelope of(Kind kind, Object payload) {
            return new Envelope(kind, Instant.now(), payload);
        }
    }

    public record TicketPayload(TicketDto.Summary ticket) {
        public static TicketPayload from(Ticket t) {
            return new TicketPayload(TicketDto.Summary.from(t));
        }
    }

    public record AssignmentPayload(TicketDto.Summary ticket, UserDto.Summary assignee) {
        public static AssignmentPayload from(Ticket t) {
            return new AssignmentPayload(
                    TicketDto.Summary.from(t),
                    UserDto.Summary.from(t.getAssignedTo()));
        }
    }

    public record CommentPayload(
            UUID commentId,
            UUID ticketId,
            UserDto.Summary author,
            String contentPreview
    ) {
        public static CommentPayload from(Comment c) {
            String preview = c.getContent().length() > 140
                    ? c.getContent().substring(0, 140) + "…"
                    : c.getContent();
            return new CommentPayload(
                    c.getId(),
                    c.getTicket().getId(),
                    UserDto.Summary.from(c.getAuthor()),
                    preview);
        }
    }
}

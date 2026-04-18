package com.smart_desk.demo.notification;

import com.smart_desk.demo.entities.Ticket;
import com.smart_desk.demo.entities.User;
import com.smart_desk.demo.notification.events.CommentAddedEvent;
import com.smart_desk.demo.notification.events.TicketAssignedEvent;
import com.smart_desk.demo.notification.events.TicketCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private static final String TOPIC_TICKETS = "/topic/tickets";
    private static final String QUEUE_NOTIFICATIONS = "/queue/notifications";

    private final SimpMessagingTemplate messaging;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketCreated(TicketCreatedEvent event) {
        Ticket t = event.ticket();
        messaging.convertAndSend(TOPIC_TICKETS, NotificationDto.Envelope.of(
                NotificationDto.Kind.TICKET_CREATED,
                NotificationDto.TicketPayload.from(t)));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketAssigned(TicketAssignedEvent event) {
        Ticket t = event.ticket();
        User assignee = event.assignee();

        NotificationDto.Envelope envelope = NotificationDto.Envelope.of(
                NotificationDto.Kind.TICKET_ASSIGNED,
                NotificationDto.AssignmentPayload.from(t));

        messaging.convertAndSend(TOPIC_TICKETS, envelope);

        if (assignee != null) {
            messaging.convertAndSendToUser(assignee.getEmail(), QUEUE_NOTIFICATIONS, envelope);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentAdded(CommentAddedEvent event) {
        NotificationDto.Envelope envelope = NotificationDto.Envelope.of(
                NotificationDto.Kind.COMMENT_ADDED,
                NotificationDto.CommentPayload.from(event.comment()));

        messaging.convertAndSend(TOPIC_TICKETS, envelope);

        User assignee = event.comment().getTicket().getAssignedTo();
        User author = event.comment().getAuthor();
        if (assignee != null && !assignee.getId().equals(author.getId())) {
            messaging.convertAndSendToUser(assignee.getEmail(), QUEUE_NOTIFICATIONS, envelope);
        }
    }
}

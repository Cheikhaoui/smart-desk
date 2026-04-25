package com.smart_desk.demo.notification;

import com.smart_desk.demo.entities.Ticket;
import com.smart_desk.demo.entities.User;
import com.smart_desk.demo.notification.events.CommentAddedEvent;
import com.smart_desk.demo.notification.events.TicketAssignedEvent;
import com.smart_desk.demo.notification.events.TicketCreatedEvent;
import com.smart_desk.demo.notification.events.TicketDeletedEvent;
import com.smart_desk.demo.notification.events.TicketStatusChangedEvent;
import com.smart_desk.demo.notification.events.TicketUnassignedEvent;
import com.smart_desk.demo.notification.events.TicketUpdatedEvent;
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

    // ── Broadcast topic events ───────────────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketCreated(TicketCreatedEvent event) {
        messaging.convertAndSend(TOPIC_TICKETS, NotificationDto.TicketCreated.of(event.ticket()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketUpdated(TicketUpdatedEvent event) {
        messaging.convertAndSend(TOPIC_TICKETS, NotificationDto.TicketUpdated.of(event.ticket()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketDeleted(TicketDeletedEvent event) {
        messaging.convertAndSend(TOPIC_TICKETS, NotificationDto.TicketDeleted.of(event.ticketId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentAdded(CommentAddedEvent event) {
        String destination = "/topic/tickets/" + event.comment().getTicket().getId() + "/comments";
        messaging.convertAndSend(destination, NotificationDto.CommentAdded.of(event.comment()));
    }

    // ── Private per-user notifications ───────────────────────────────────────

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketAssigned(TicketAssignedEvent event) {
        Ticket t = event.ticket();
        User assignee = event.assignee();
        if (assignee == null) return;
        messaging.convertAndSendToUser(
                assignee.getId().toString(),
                QUEUE_NOTIFICATIONS,
                NotificationDto.UserNotification.assigned(t));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketUnassigned(TicketUnassignedEvent event) {
        User previous = event.previousAssignee();
        if (previous == null) return;
        messaging.convertAndSendToUser(
                previous.getId().toString(),
                QUEUE_NOTIFICATIONS,
                NotificationDto.UserNotification.unassigned(event.ticket()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTicketStatusChanged(TicketStatusChangedEvent event) {
        Ticket t = event.ticket();
        User creator = t.getCreatedBy();
        if (creator == null) return;
        messaging.convertAndSendToUser(
                creator.getId().toString(),
                QUEUE_NOTIFICATIONS,
                NotificationDto.UserNotification.statusChanged(t, event.oldStatus(), event.newStatus()));
    }
}

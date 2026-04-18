package com.smart_desk.demo.notification.events;

import com.smart_desk.demo.entities.Ticket;
import com.smart_desk.demo.entities.User;

public record TicketAssignedEvent(Ticket ticket, User assignee) {}

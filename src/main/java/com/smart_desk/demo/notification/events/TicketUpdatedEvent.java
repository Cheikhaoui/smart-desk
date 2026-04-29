package com.smart_desk.demo.notification.events;

import com.smart_desk.demo.entities.Ticket;

public record TicketUpdatedEvent(Ticket ticket) {}

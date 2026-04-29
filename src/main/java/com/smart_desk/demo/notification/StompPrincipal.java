package com.smart_desk.demo.notification;

import com.smart_desk.demo.entities.User;

import java.security.Principal;

/**
 * STOMP session principal whose getName() is the user UUID, so that
 * messages sent via convertAndSendToUser(uuid, ...) reach this session.
 */
public record StompPrincipal(String name, User user) implements Principal {
    @Override public String getName() { return name; }
}

package com.smart_desk.demo.service;


import com.smart_desk.demo.dto.TicketDto;
import com.smart_desk.demo.entities.Ticket;
import com.smart_desk.demo.entities.User;
import com.smart_desk.demo.notification.events.TicketAssignedEvent;
import com.smart_desk.demo.notification.events.TicketCreatedEvent;
import com.smart_desk.demo.notification.events.TicketDeletedEvent;
import com.smart_desk.demo.notification.events.TicketStatusChangedEvent;
import com.smart_desk.demo.notification.events.TicketUnassignedEvent;
import com.smart_desk.demo.notification.events.TicketUpdatedEvent;
import com.smart_desk.demo.repositories.TicketRepository;
import com.smart_desk.demo.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    // ── Create ───────────────────────────────────────────────────────────────

    @Transactional
    public TicketDto.TicketResponse create(TicketDto.CreateRequest req, User currentUser) {
        Ticket ticket = Ticket.builder()
                .title(req.title())
                .description(req.description())
                .priority(req.priority() != null ? req.priority() : Ticket.Priority.MEDIUM)
                .category(req.category())
                .createdBy(currentUser)
                .build();

        Ticket saved = ticketRepository.save(ticket);
        eventPublisher.publishEvent(new TicketCreatedEvent(saved));
        return TicketDto.TicketResponse.from(saved);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    public TicketDto.TicketResponse findById(UUID id) {
        return TicketDto.TicketResponse.from(getOrThrow(id));
    }

    public Page<TicketDto.TicketSummary> search(
            Ticket.Status status,
            Ticket.Priority priority,
            String category,
            Pageable pageable) {
        return ticketRepository
                .search(status, priority, category, pageable)
                .map(TicketDto.TicketSummary::from);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public TicketDto.TicketResponse update(UUID id, TicketDto.UpdateRequest req, User currentUser) {
        Ticket ticket = getOrThrow(id);
        assertCanModify(ticket, currentUser);

        Ticket.Status oldStatus = ticket.getStatus();

        if (req.title()       != null) ticket.setTitle(req.title());
        if (req.description() != null) ticket.setDescription(req.description());
        if (req.status()      != null) ticket.setStatus(req.status());
        if (req.priority()    != null) ticket.setPriority(req.priority());
        if (req.category()    != null) ticket.setCategory(req.category());

        boolean assignmentChanged = false;
        if (req.assignedToId() != null) {
            User agent = userRepository.findById(req.assignedToId())
                    .orElseThrow(() -> new EntityNotFoundException("User not found: " + req.assignedToId()));
            assignmentChanged = ticket.getAssignedTo() == null
                    || !ticket.getAssignedTo().getId().equals(agent.getId());
            ticket.setAssignedTo(agent);
        }

        Ticket saved = ticketRepository.save(ticket);

        eventPublisher.publishEvent(new TicketUpdatedEvent(saved));
        if (assignmentChanged) {
            eventPublisher.publishEvent(new TicketAssignedEvent(saved, saved.getAssignedTo()));
        }
        if (req.status() != null && req.status() != oldStatus) {
            eventPublisher.publishEvent(new TicketStatusChangedEvent(saved, oldStatus, req.status()));
        }
        return TicketDto.TicketResponse.from(saved);
    }

    // ── Assignment ───────────────────────────────────────────────────────────

    @Transactional
    public TicketDto.TicketResponse assign(UUID ticketId, UUID agentId) {
        Ticket ticket = getOrThrow(ticketId);
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + agentId));

        if (agent.getRole() != User.Role.AGENT && agent.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Only AGENT or ADMIN users can be assigned tickets");
        }

        ticket.setAssignedTo(agent);
        Ticket saved = ticketRepository.save(ticket);

        eventPublisher.publishEvent(new TicketUpdatedEvent(saved));
        eventPublisher.publishEvent(new TicketAssignedEvent(saved, agent));
        return TicketDto.TicketResponse.from(saved);
    }

    @Transactional
    public TicketDto.TicketResponse unassign(UUID ticketId) {
        Ticket ticket = getOrThrow(ticketId);
        User previous = ticket.getAssignedTo();
        ticket.setAssignedTo(null);
        Ticket saved = ticketRepository.save(ticket);

        eventPublisher.publishEvent(new TicketUpdatedEvent(saved));
        if (previous != null) {
            eventPublisher.publishEvent(new TicketUnassignedEvent(saved, previous));
        }
        return TicketDto.TicketResponse.from(saved);
    }

    public Page<TicketDto.TicketSummary> findAssignedToUser(User user, Pageable pageable) {
        return ticketRepository.findByAssignedToId(user.getId(), pageable)
                .map(TicketDto.TicketSummary::from);
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID id, User currentUser) {
        Ticket ticket = getOrThrow(id);
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AccessDeniedException("Only admins can delete tickets");
        }
        UUID ticketId = ticket.getId();
        ticketRepository.delete(ticket);
        eventPublisher.publishEvent(new TicketDeletedEvent(ticketId));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Ticket getOrThrow(UUID id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + id));
    }

    private void assertCanModify(Ticket ticket, User user) {
        boolean allowed = user.getRole() == User.Role.ADMIN
                || user.getRole() == User.Role.AGENT
                || ticket.getCreatedBy().getId().equals(user.getId());

        if (!allowed) {
            throw new AccessDeniedException(
                    "You don't have permission to modify this ticket");
        }
    }
}

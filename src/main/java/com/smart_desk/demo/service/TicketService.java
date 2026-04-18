package com.smart_desk.demo.service;


import com.smart_desk.demo.dto.TicketDto;
import com.smart_desk.demo.entities.Ticket;
import com.smart_desk.demo.entities.User;
import com.smart_desk.demo.notification.events.TicketAssignedEvent;
import com.smart_desk.demo.notification.events.TicketCreatedEvent;
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
    public TicketDto.Response create(TicketDto.CreateRequest req, User currentUser) {
        Ticket ticket = Ticket.builder()
                .title(req.title())
                .description(req.description())
                .priority(req.priority() != null ? req.priority() : Ticket.Priority.MEDIUM)
                .category(req.category())
                .createdBy(currentUser)
                .build();

        Ticket saved = ticketRepository.save(ticket);
        eventPublisher.publishEvent(new TicketCreatedEvent(saved));
        return TicketDto.Response.from(saved);
    }

    // ── Read ─────────────────────────────────────────────────────────────────

    public TicketDto.Response findById(UUID id) {
        return TicketDto.Response.from(getOrThrow(id));
    }

    public Page<TicketDto.Summary> search(
            Ticket.Status status,
            Ticket.Priority priority,
            String category,
            Pageable pageable) {
        return ticketRepository
                .search(status, priority, category, pageable)
                .map(TicketDto.Summary::from);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    @Transactional
    public TicketDto.Response update(UUID id, TicketDto.UpdateRequest req, User currentUser) {
        Ticket ticket = getOrThrow(id);
        assertCanModify(ticket, currentUser);

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
        if (assignmentChanged) {
            eventPublisher.publishEvent(new TicketAssignedEvent(saved, saved.getAssignedTo()));
        }
        return TicketDto.Response.from(saved);
    }

    // ── Assignment ───────────────────────────────────────────────────────────

    @Transactional
    public TicketDto.Response assign(UUID ticketId, UUID agentId) {
        Ticket ticket = getOrThrow(ticketId);
        User agent = userRepository.findById(agentId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + agentId));

        if (agent.getRole() != User.Role.AGENT && agent.getRole() != User.Role.ADMIN) {
            throw new IllegalArgumentException("Only AGENT or ADMIN users can be assigned tickets");
        }

        ticket.setAssignedTo(agent);
        Ticket saved = ticketRepository.save(ticket);
        eventPublisher.publishEvent(new TicketAssignedEvent(saved, agent));
        return TicketDto.Response.from(saved);
    }

    @Transactional
    public TicketDto.Response unassign(UUID ticketId) {
        Ticket ticket = getOrThrow(ticketId);
        ticket.setAssignedTo(null);
        return TicketDto.Response.from(ticketRepository.save(ticket));
    }

    public Page<TicketDto.Summary> findAssignedToUser(User user, Pageable pageable) {
        return ticketRepository.findByAssignedToId(user.getId(), pageable)
                .map(TicketDto.Summary::from);
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    @Transactional
    public void delete(UUID id, User currentUser) {
        Ticket ticket = getOrThrow(id);
        if (currentUser.getRole() != User.Role.ADMIN) {
            throw new AccessDeniedException("Only admins can delete tickets");
        }
        ticketRepository.delete(ticket);
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


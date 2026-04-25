package com.smart_desk.demo.service;

import com.smart_desk.demo.dto.CommentDto;
import com.smart_desk.demo.entities.Comment;
import com.smart_desk.demo.entities.Ticket;
import com.smart_desk.demo.entities.User;
import com.smart_desk.demo.notification.events.CommentAddedEvent;
import com.smart_desk.demo.repositories.CommentRepository;
import com.smart_desk.demo.repositories.TicketRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final ApplicationEventPublisher eventPublisher;

    public List<CommentDto.CommentResponse> listForTicket(UUID ticketId) {
        ensureTicketExists(ticketId);
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(CommentDto.CommentResponse::from)
                .toList();
    }

    @Transactional
    public CommentDto.CommentResponse add(UUID ticketId, CommentDto.CommentCreateRequest req, User currentUser) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + ticketId));

        Comment comment = Comment.builder()
                .ticket(ticket)
                .author(currentUser)
                .content(req.content())
                .aiGenerated(false)
                .build();

        Comment saved = commentRepository.save(comment);
        eventPublisher.publishEvent(new CommentAddedEvent(saved));
        return CommentDto.CommentResponse.from(saved);
    }

    @Transactional
    public CommentDto.CommentResponse update(UUID commentId, CommentDto.CommentUpdateRequest req, User currentUser) {
        Comment comment = getOrThrow(commentId);
        assertCanModify(comment, currentUser);

        comment.setContent(req.content());
        return CommentDto.CommentResponse.from(commentRepository.save(comment));
    }

    @Transactional
    public void delete(UUID commentId, User currentUser) {
        Comment comment = getOrThrow(commentId);
        assertCanModify(comment, currentUser);
        commentRepository.delete(comment);
    }

    private Comment getOrThrow(UUID id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found: " + id));
    }

    private void ensureTicketExists(UUID ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new EntityNotFoundException("Ticket not found: " + ticketId);
        }
    }

    private void assertCanModify(Comment comment, User user) {
        boolean allowed = user.getRole() == User.Role.ADMIN
                || comment.getAuthor().getId().equals(user.getId());
        if (!allowed) {
            throw new AccessDeniedException("You can only modify your own comments");
        }
    }
}

package com.smart_desk.demo.ai;

import com.smart_desk.demo.dto.TicketDto;
import com.smart_desk.demo.entities.Comment;
import com.smart_desk.demo.entities.Ticket;
import com.smart_desk.demo.notification.events.TicketUpdatedEvent;
import com.smart_desk.demo.repositories.CommentRepository;
import com.smart_desk.demo.repositories.TicketRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiService {

    private static final int MAX_THREAD_COMMENTS  = 10;
    private static final int MAX_SUMMARY_COMMENTS = 50;

    private final Optional<AiProvider> aiProvider;
    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SuggestedReply suggestReply(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + ticketId));

        List<Comment> comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        if (comments.size() > MAX_THREAD_COMMENTS) {
            comments = comments.subList(comments.size() - MAX_THREAD_COMMENTS, comments.size());
        }

        String suggestion = provider().suggestReply(ticket, comments);
        return new SuggestedReply(suggestion);
    }

    @Transactional
    public TicketDto.TicketResponse summarizeThread(UUID ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Ticket not found: " + ticketId));

        List<Comment> comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        if (comments.size() > MAX_SUMMARY_COMMENTS) {
            comments = comments.subList(0, MAX_SUMMARY_COMMENTS);
        }

        String summary = provider().summarizeThread(ticket, comments);
        ticket.setAiSummary(summary);
        ticketRepository.save(ticket);
        eventPublisher.publishEvent(new TicketUpdatedEvent(ticket));
        return TicketDto.TicketResponse.from(ticket);
    }

    private AiProvider provider() {
        return aiProvider.orElseThrow(AiUnavailableException::new);
    }
}

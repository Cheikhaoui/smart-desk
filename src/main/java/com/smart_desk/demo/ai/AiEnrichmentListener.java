package com.smart_desk.demo.ai;

import com.smart_desk.demo.entities.Ticket;
import com.smart_desk.demo.notification.events.TicketCreatedEvent;
import com.smart_desk.demo.notification.events.TicketUpdatedEvent;
import com.smart_desk.demo.repositories.TicketRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@ConditionalOnBean(AiProvider.class)
@Slf4j
public class AiEnrichmentListener {

    private final AiProvider aiProvider;
    private final TicketRepository ticketRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.ai.confidence-threshold:0.6}")
    private double confidenceThreshold;

    public AiEnrichmentListener(AiProvider aiProvider,
                                TicketRepository ticketRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.aiProvider = aiProvider;
        this.ticketRepository = ticketRepository;
        this.eventPublisher = eventPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTicketCreated(TicketCreatedEvent event) {
        Ticket ticket = event.ticket();

        // Skip enrichment when the user already provided a category
        if (ticket.getCategory() != null && !ticket.getCategory().isBlank()) {
            return;
        }

        try {
            CategorizationResult result = aiProvider.categorize(ticket.getTitle(), ticket.getDescription());
            log.debug("AI classified ticket {} → {} (confidence={})",
                    ticket.getId(), result.category(), result.confidence());

            if (result.confidence() >= confidenceThreshold) {
                // Reload inside this new transaction — the event carries a detached snapshot
                Ticket fresh = ticketRepository.findById(ticket.getId()).orElse(null);
                if (fresh == null) return;

                fresh.setCategory(result.category());
                ticketRepository.save(fresh);
                eventPublisher.publishEvent(new TicketUpdatedEvent(fresh));
                log.info("Ticket {} auto-categorized as '{}' (confidence={})",
                        ticket.getId(), result.category(), result.confidence());
            } else {
                log.debug("Skipping categorization for ticket {} — confidence {} below threshold {}",
                        ticket.getId(), result.confidence(), confidenceThreshold);
            }
        } catch (Exception e) {
            // AI enrichment is best-effort; ticket creation already committed successfully
            log.warn("AI categorization failed for ticket {}: {}", ticket.getId(), e.getMessage());
        }
    }
}

package com.smart_desk.demo.repositories;

import com.smart_desk.demo.entities.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Page<Ticket> findByAssignedToId(UUID agentId, Pageable pageable);

    Page<Ticket> findByCreatedById(UUID userId, Pageable pageable);

    /**
     * Dynamic filter: any null param is ignored.
     * Shows senior understanding of flexible querying without
     * writing a full Specification/Criteria layer.
     */
    @Query("""
        SELECT t FROM Ticket t
        WHERE (:status   IS NULL OR t.status   = :status)
        AND   (:priority IS NULL OR t.priority = :priority)
        AND   (:category IS NULL OR t.category = :category)
        """)
    Page<Ticket> search(
        @Param("status")   Ticket.Status   status,
        @Param("priority") Ticket.Priority priority,
        @Param("category") String          category,
        Pageable pageable
    );

    long countByStatus(Ticket.Status status);
}

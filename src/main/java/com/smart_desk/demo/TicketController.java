package com.smart_desk.demo;


import com.smart_desk.demo.dto.TicketDto;
import com.smart_desk.demo.entities.Ticket;
import com.smart_desk.demo.entities.User;
import com.smart_desk.demo.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // ── Create ───────────────────────────────────────────────────────────────
    @PostMapping
    @Operation(summary = "Create a new ticket",
            description = "Creates a ticket owned by the currently authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ticket created"),
            @ApiResponse(responseCode = "400", description = "Invalid input",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated")})
    public ResponseEntity<TicketDto.Response> create(
            @Valid @RequestBody TicketDto.CreateRequest request,
            @AuthenticationPrincipal User currentUser) {

        TicketDto.Response created = ticketService.create(request, currentUser);
        return ResponseEntity
                .created(URI.create("/v1/tickets/" + created.id()))
                .body(created);
    }

    // ── List / search ────────────────────────────────────────────────────────
    @GetMapping
    public Page<TicketDto.Summary> search(
            @RequestParam(required = false) Ticket.Status status,
            @RequestParam(required = false) Ticket.Priority priority,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        return ticketService.search(status, priority, category, pageable);
    }

    // ── Assigned to me ───────────────────────────────────────────────────────
    @GetMapping("/mine")
    @Operation(summary = "List tickets assigned to the current user")
    public Page<TicketDto.Summary> mine(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        return ticketService.findAssignedToUser(currentUser, pageable);
    }

    // ── Get by id ────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public TicketDto.Response getById(@PathVariable UUID id) {
        return ticketService.findById(id);
    }

    // ── Update ───────────────────────────────────────────────────────────────
    @PatchMapping("/{id}")
    public TicketDto.Response update(
            @PathVariable UUID id,
            @Valid @RequestBody TicketDto.UpdateRequest request,
            @AuthenticationPrincipal User currentUser) {

        return ticketService.update(id, request, currentUser);
    }

    // ── Assign / unassign ────────────────────────────────────────────────────
    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign a ticket to an agent (AGENT/ADMIN only)")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public TicketDto.Response assign(
            @PathVariable UUID id,
            @Valid @RequestBody TicketDto.AssignRequest request) {

        return ticketService.assign(id, request.agentId());
    }

    @DeleteMapping("/{id}/assign")
    @Operation(summary = "Unassign a ticket (AGENT/ADMIN only)")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public TicketDto.Response unassign(@PathVariable UUID id) {
        return ticketService.unassign(id);
    }

    // ── Delete ───────────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {

        ticketService.delete(id, currentUser);
    }
}


package com.smart_desk.demo;

import com.smart_desk.demo.dto.CommentDto;
import com.smart_desk.demo.entities.User;
import com.smart_desk.demo.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/tickets/{ticketId}/comments")
    @Operation(summary = "List comments for a ticket")
    public List<CommentDto.Response> list(@PathVariable UUID ticketId) {
        return commentService.listForTicket(ticketId);
    }

    @PostMapping("/tickets/{ticketId}/comments")
    @Operation(summary = "Add a comment to a ticket")
    public ResponseEntity<CommentDto.Response> add(
            @PathVariable UUID ticketId,
            @Valid @RequestBody CommentDto.CreateRequest request,
            @AuthenticationPrincipal User currentUser) {

        CommentDto.Response created = commentService.add(ticketId, request, currentUser);
        return ResponseEntity
                .created(URI.create("/v1/comments/" + created.id()))
                .body(created);
    }

    @PatchMapping("/comments/{commentId}")
    @Operation(summary = "Update a comment (author or ADMIN only)")
    public CommentDto.Response update(
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentDto.UpdateRequest request,
            @AuthenticationPrincipal User currentUser) {

        return commentService.update(commentId, request, currentUser);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a comment (author or ADMIN only)")
    public void delete(
            @PathVariable UUID commentId,
            @AuthenticationPrincipal User currentUser) {

        commentService.delete(commentId, currentUser);
    }
}

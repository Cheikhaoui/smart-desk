package com.smart_desk.demo;

import com.smart_desk.demo.dto.CommentDto;
import com.smart_desk.demo.entities.User;
import com.smart_desk.demo.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/tickets/{ticketId}/comments")
    @Operation(summary = "List comments for a ticket")
    @ApiResponse(responseCode = "200", description = "Comments for the ticket",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(schema = @Schema(implementation = CommentDto.CommentResponse.class))))
    public List<CommentDto.CommentResponse> list(@PathVariable UUID ticketId) {
        return commentService.listForTicket(ticketId);
    }

    @PostMapping("/tickets/{ticketId}/comments")
    @Operation(summary = "Add a comment to a ticket")
    @ApiResponse(responseCode = "201", description = "Comment created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CommentDto.CommentResponse.class)))
    public ResponseEntity<CommentDto.CommentResponse> add(
            @PathVariable UUID ticketId,
            @Valid @RequestBody CommentDto.CommentCreateRequest request,
            @AuthenticationPrincipal User currentUser) {

        CommentDto.CommentResponse created = commentService.add(ticketId, request, currentUser);
        return ResponseEntity
                .created(URI.create("/v1/comments/" + created.id()))
                .body(created);
    }

    @PatchMapping("/comments/{commentId}")
    @Operation(summary = "Update a comment (author or ADMIN only)")
    @ApiResponse(responseCode = "200", description = "Comment updated",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CommentDto.CommentResponse.class)))
    public CommentDto.CommentResponse update(
            @PathVariable UUID commentId,
            @Valid @RequestBody CommentDto.CommentUpdateRequest request,
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

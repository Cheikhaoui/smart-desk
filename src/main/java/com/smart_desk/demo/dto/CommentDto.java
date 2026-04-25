package com.smart_desk.demo.dto;

import com.smart_desk.demo.entities.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class CommentDto {

    public record CommentCreateRequest(
            @NotBlank @Size(max = 5000) String content
    ) {}

    public record CommentUpdateRequest(
            @NotBlank @Size(max = 5000) String content
    ) {}

    public record CommentResponse(
            UUID id,
            UUID ticketId,
            UserDto.UserSummary author,
            String content,
            boolean aiGenerated,
            Instant createdAt
    ) {
        public static CommentResponse from(Comment c) {
            return new CommentResponse(
                    c.getId(),
                    c.getTicket().getId(),
                    UserDto.UserSummary.from(c.getAuthor()),
                    c.getContent(),
                    c.isAiGenerated(),
                    c.getCreatedAt()
            );
        }
    }
}

package com.smart_desk.demo.dto;

import com.smart_desk.demo.entities.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class CommentDto {

    public record CreateRequest(
            @NotBlank @Size(max = 5000) String content
    ) {}

    public record UpdateRequest(
            @NotBlank @Size(max = 5000) String content
    ) {}

    public record Response(
            UUID id,
            UUID ticketId,
            UserDto.Summary author,
            String content,
            boolean aiGenerated,
            Instant createdAt
    ) {
        public static Response from(Comment c) {
            return new Response(
                    c.getId(),
                    c.getTicket().getId(),
                    UserDto.Summary.from(c.getAuthor()),
                    c.getContent(),
                    c.isAiGenerated(),
                    c.getCreatedAt()
            );
        }
    }
}

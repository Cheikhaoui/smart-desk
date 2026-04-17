package com.smart_desk.demo.dto;

import com.smart_desk.demo.entities.User;

import java.util.UUID;

public class UserDto {

    /** Response — never expose password or internal fields */
    public record Response(
        UUID id,
        String email,
        String fullName,
        User.Role role,
        boolean active
    ) {
        public static Response from(User user) {
            return new Response(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isActive()
            );
        }
    }

    /** Minimal summary for embedding in other responses */
    public record Summary(UUID id, String fullName, String email) {
        public static Summary from(User user) {
            if (user == null) return null;
            return new Summary(user.getId(), user.getFullName(), user.getEmail());
        }
    }
}

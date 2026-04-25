package com.smart_desk.demo.dto;

import com.smart_desk.demo.entities.User;

import java.util.UUID;

public class UserDto {

    /** Response — never expose password or internal fields */
    public record UserResponse(
        UUID id,
        String email,
        String fullName,
        User.Role role,
        boolean active
    ) {
        public static UserResponse from(User user) {
            return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.isActive()
            );
        }
    }

    /** Minimal summary for embedding in other responses */
    public record UserSummary(UUID id, String fullName, String email) {
        public static UserSummary from(User user) {
            if (user == null) return null;
            return new UserSummary(user.getId(), user.getFullName(), user.getEmail());
        }
    }
}

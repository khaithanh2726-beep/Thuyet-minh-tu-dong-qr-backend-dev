package com.qraudio.backend.dto;

import com.qraudio.backend.entity.User;
import java.time.LocalDateTime;

public record UserResponse(Long id, String username, String role, boolean active,
                           LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(),
                user.isActive(), user.getCreatedAt(), user.getUpdatedAt());
    }
}

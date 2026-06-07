package com.example.meetingroom.api.dto;

import com.example.meetingroom.domain.User;
import com.example.meetingroom.domain.UserRole;

public record UserResponse(
    Long id,
    String username,
    String email,
    String displayName,
    UserRole role
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(),
            user.getDisplayName(), user.getRole());
    }
}

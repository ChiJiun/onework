package com.example.meetingroom.api.dto;

import com.example.meetingroom.domain.User;
import com.example.meetingroom.domain.UserRole;

public record LoginResponse(
    String token,
    String tokenType,
    Long userId,
    String displayName,
    UserRole role
) {
    public LoginResponse(String token, User user) {
        this(token, "Bearer", user.getId(), user.getDisplayName(), user.getRole());
    }
}

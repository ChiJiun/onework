package com.example.meetingroom.security;

import com.example.meetingroom.domain.User;
import com.example.meetingroom.exception.ResourceNotFoundException;
import com.example.meetingroom.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user no longer exists"));
    }
}

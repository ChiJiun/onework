package com.example.meetingroom.api;

import com.example.meetingroom.api.dto.UserResponse;
import com.example.meetingroom.repository.UserRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('REVIEWER', 'ADMIN')")
    public List<UserResponse> users() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }
}

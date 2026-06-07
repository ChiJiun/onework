package com.example.meetingroom.api;

import com.example.meetingroom.api.dto.LoginRequest;
import com.example.meetingroom.api.dto.LoginResponse;
import com.example.meetingroom.domain.User;
import com.example.meetingroom.repository.UserRepository;
import com.example.meetingroom.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        UserRepository userRepository
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        UserDetails principal = (UserDetails) authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        ).getPrincipal();
        User user = userRepository.findByUsername(principal.getUsername()).orElseThrow();
        return ResponseEntity.ok(new LoginResponse(jwtService.generateToken(principal), user));
    }
}

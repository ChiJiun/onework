package com.example.meetingroom.api;

import com.example.meetingroom.api.dto.LoginRequest;
import com.example.meetingroom.api.dto.LoginResponse;
import com.example.meetingroom.domain.User;
import com.example.meetingroom.domain.UserRole;
import com.example.meetingroom.exception.ForbiddenOperationException;
import com.example.meetingroom.repository.UserRepository;
import com.example.meetingroom.security.IpWhitelist;
import com.example.meetingroom.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final IpWhitelist ipWhitelist;

    public AuthController(
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        UserRepository userRepository,
        IpWhitelist ipWhitelist
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.ipWhitelist = ipWhitelist;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticate(request));
    }

    @PostMapping("/admin/ip-login")
    public ResponseEntity<LoginResponse> adminIpLogin(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest
    ) {
        LoginResponse response = authenticate(request);
        if (response.role() != UserRole.ADMIN) {
            throw new ForbiddenOperationException("Only administrator accounts can use IP whitelist login");
        }
        if (!ipWhitelist.isAllowed(httpRequest.getRemoteAddr())) {
            throw new ForbiddenOperationException("Administrator login is not allowed from this IP address");
        }
        return ResponseEntity.ok(response);
    }

    private LoginResponse authenticate(LoginRequest request) {
        UserDetails principal = (UserDetails) authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        ).getPrincipal();
        User user = userRepository.findByUsername(principal.getUsername()).orElseThrow();
        return new LoginResponse(jwtService.generateToken(principal), user);
    }
}

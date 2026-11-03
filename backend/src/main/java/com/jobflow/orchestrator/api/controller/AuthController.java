package com.jobflow.orchestrator.api.controller;

import com.jobflow.orchestrator.api.dto.LoginRequest;
import com.jobflow.orchestrator.api.dto.RegisterRequest;
import com.jobflow.orchestrator.api.dto.TokenResponse;
import com.jobflow.orchestrator.domain.User;
import com.jobflow.orchestrator.domain.UserRole;
import com.jobflow.orchestrator.repository.UserRepository;
import com.jobflow.orchestrator.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        User user = new User(request.username(), passwordEncoder.encode(request.password()), UserRole.OPERATOR);
        userRepository.save(user);
        String token = jwtService.issueToken(user.getUsername(), user.getRole().name());
        return ResponseEntity.status(HttpStatus.CREATED).body(new TokenResponse(token));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User user = userRepository.findByUsername(request.username()).orElseThrow();
        String token = jwtService.issueToken(user.getUsername(), user.getRole().name());
        return ResponseEntity.ok(new TokenResponse(token));
    }
}

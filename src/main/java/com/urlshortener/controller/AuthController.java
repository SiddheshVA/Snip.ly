package com.urlshortener.controller;

import com.urlshortener.dto.AuthRequest;
import com.urlshortener.dto.AuthResponse;
import com.urlshortener.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller.
 * POST /api/auth/register → create account + return JWT
 * POST /api/auth/login    → authenticate + return JWT
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }
}

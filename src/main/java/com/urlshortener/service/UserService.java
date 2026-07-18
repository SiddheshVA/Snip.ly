package com.urlshortener.service;

import com.urlshortener.dto.AuthRequest;
import com.urlshortener.dto.AuthResponse;
import com.urlshortener.model.User;
import com.urlshortener.repository.UserRepository;
import com.urlshortener.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    /** Register a new user and return a JWT token */
    public AuthResponse register(AuthRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.USER)
                .build();

        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .email(user.getEmail())
                .name(user.getName())
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .build();
    }

    /** Authenticate existing user and return a JWT token */
    public AuthResponse login(AuthRequest request) {
        // This will throw BadCredentialsException if wrong credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String token = jwtTokenProvider.generateToken(user);
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .email(user.getEmail())
                .name(user.getName())
                .expiresIn(jwtTokenProvider.getExpirationMs())
                .build();
    }
}

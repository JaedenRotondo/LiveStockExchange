package com.crypto.screener.screeningApplication.service;

import com.crypto.screener.screeningApplication.dto.AuthDto;
import com.crypto.screener.screeningApplication.model.User;
import com.crypto.screener.screeningApplication.repository.UserRepository;
import com.crypto.screener.screeningApplication.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserRepository    userRepository;
    private final PasswordEncoder   passwordEncoder;
    private final JwtService        jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService      = jwtService;
    }

    // ── Register ─────────────────────────────────────────────────
    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
        }

        User user = new User(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName()
        );
        userRepository.save(user);

        // user.getId() = UUID String → becomes "sub" in the JWT
        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthDto.AuthResponse(token, user.getId(), user.getEmail(), user.getFullName());
    }

    // ── Login ────────────────────────────────────────────────────
    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new AuthDto.AuthResponse(token, user.getId(), user.getEmail(), user.getFullName());
    }
}
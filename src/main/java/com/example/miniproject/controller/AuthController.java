package com.example.miniproject.controller;

import com.example.miniproject.model.AuthRequest;
import com.example.miniproject.model.AuthResponse;
import com.example.miniproject.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtService jwtService;

    @PostMapping("/login")
    public Mono<ResponseEntity<AuthResponse>> login(@RequestBody AuthRequest request) {
        // In a real application, you would validate credentials here
        // For demo purposes, we're just generating a token
        String token = jwtService.generateToken(
                request.getUsername(),
                Arrays.asList("ROLE_USER")
        );

        return Mono.just(ResponseEntity.ok(new AuthResponse(token)));
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            jwtService.blacklistToken(token);
        }
        return Mono.just(ResponseEntity.ok().build());
    }
}

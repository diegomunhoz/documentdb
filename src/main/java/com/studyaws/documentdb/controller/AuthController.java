package com.studyaws.documentdb.controller;

import com.studyaws.documentdb.security.JwtUtil;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "AuthController", description = "Token para acesso")
public class AuthController {

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {
        // Exemplo simples: usuário/senha fixos. Implemente UserDetailsService para produção!
        if ("admin".equals(username) && "admin".equals(password)) {
            String token = jwtUtil.generateToken(username);
            return ResponseEntity.ok().body("{\"token\": \"" + token + "\"}");
        }
        return ResponseEntity.status(401).body("Usuário ou senha inválidos");
    }
}
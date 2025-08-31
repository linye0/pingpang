package com.pingpang.training.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/password-test")
public class PasswordTestController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/generate-hash")
    public ResponseEntity<Map<String, Object>> generateHash(@RequestParam String password) {
        Map<String, Object> response = new HashMap<>();
        String hash = passwordEncoder.encode(password);
        response.put("password", password);
        response.put("hash", hash);
        response.put("success", true);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyPassword(
            @RequestParam String password, 
            @RequestParam String hash) {
        Map<String, Object> response = new HashMap<>();
        boolean matches = passwordEncoder.matches(password, hash);
        response.put("password", password);
        response.put("hash", hash);
        response.put("matches", matches);
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
} 
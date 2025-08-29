package com.example.pingpang.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @GetMapping("/generate-password/{password}")
    public String generatePasswordHash(@PathVariable String password) {
        String hash = passwordEncoder.encode(password);
        
        // 验证生成的哈希
        boolean matches = passwordEncoder.matches(password, hash);
        
        return "原始密码: " + password + "\n" +
               "生成的哈希: " + hash + "\n" +
               "验证结果: " + matches + "\n" +
               "SQL语句: UPDATE users SET password = '" + hash + "' WHERE username = 'admin';";
    }
} 
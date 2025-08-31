package com.pingpang.training.controller;

import com.pingpang.training.dto.ApiResponse;
import com.pingpang.training.dto.LoginRequest;
import com.pingpang.training.dto.RegisterRequest;
import com.pingpang.training.service.AuthService;
import com.pingpang.training.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            return ApiResponse.success(authService.login(loginRequest));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
    
    @PostMapping("/register/student")
    public ApiResponse<?> registerStudent(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            return ApiResponse.success("学员注册成功", userService.registerStudent(registerRequest));
        } catch (Exception e) {
            return ApiResponse.error("注册失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/register/coach")
    public ApiResponse<?> registerCoach(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            return ApiResponse.success("教练注册申请已提交，等待审核", userService.registerCoach(registerRequest));
        } catch (Exception e) {
            return ApiResponse.error("注册失败: " + e.getMessage());
        }
    }
} 
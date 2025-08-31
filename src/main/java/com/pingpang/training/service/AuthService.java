package com.pingpang.training.service;

import com.pingpang.training.dto.LoginRequest;
import com.pingpang.training.entity.User;
import com.pingpang.training.repository.UserRepository;
import com.pingpang.training.security.JwtTokenUtil;
import com.pingpang.training.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Map<String, Object> login(LoginRequest loginRequest) {
        try {

            System.out.println("DEBUG: 尝试登录用户: " + loginRequest.getUsername());
            System.out.println("DEBUG: 前端传来的密码: " + loginRequest.getPassword());
            

            String hashedPassword = passwordEncoder.encode(loginRequest.getPassword());
            System.out.println("DEBUG: 前端密码的BCrypt哈希值: " + hashedPassword);
            
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            String token = jwtTokenUtil.generateToken(userDetails);
            User user = userDetails.getUser();

            System.out.println("DEBUG: JWT Token生成成功: " + (token != null ? "是" : "否"));
            System.out.println("DEBUG: Token长度: " + (token != null ? token.length() : 0));
            System.out.println("DEBUG: 用户角色: " + user.getRole());

            Map<String, Object> result = new HashMap<>();
            result.put("token", token);
            result.put("user", getUserInfo(user));
            
            System.out.println("DEBUG: 登录成功，返回结果");
            return result;
        } catch (BadCredentialsException e) {
            throw new RuntimeException("用户名或密码错误");
        }
    }

    public Map<String, Object> getUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("realName", user.getRealName());
        userInfo.put("role", user.getRole());
        

        if (user.getCampus() != null) {
            userInfo.put("campusId", user.getCampus().getId());
            userInfo.put("campusName", user.getCampus().getName());
        } else {
            userInfo.put("campusId", null);
            userInfo.put("campusName", "系统管理");
        }
        
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("phone", user.getPhone());
        userInfo.put("email", user.getEmail());
        return userInfo;
    }
} 
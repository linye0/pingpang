package com.example.pingpang.controller;

import com.example.pingpang.dto.ApiResponse;
import com.example.pingpang.dto.UserDTO;
import com.example.pingpang.entity.User;
import com.example.pingpang.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
    
    private final UserService userService;
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserInfo(@PathVariable Long id) {
        try {
            UserDTO user = userService.getUserInfo(id);
            return ResponseEntity.ok(ApiResponse.success(user));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            UserDTO updatedUser = userService.updateUser(id, user);
            return ResponseEntity.ok(ApiResponse.success("更新成功", updatedUser));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/ranking")
    public ResponseEntity<ApiResponse<Page<UserDTO>>> getPlayerRanking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<UserDTO> ranking = userService.getPlayerRanking(pageable);
            return ResponseEntity.ok(ApiResponse.success(ranking));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/ranking/top")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getTopPlayers(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<UserDTO> topPlayers = userService.getPlayerRanking(limit);
            return ResponseEntity.ok(ApiResponse.success(topPlayers));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getUsersByRole(@PathVariable User.UserRole role) {
        try {
            List<UserDTO> users = userService.getUsersByRole(role);
            return ResponseEntity.ok(ApiResponse.success(users));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/players")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getAllPlayers() {
        try {
            List<UserDTO> players = userService.getUsersByRole(User.UserRole.PLAYER);
            return ResponseEntity.ok(ApiResponse.success(players));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserDTO>>> searchUsers(@RequestParam String name) {
        try {
            List<UserDTO> users = userService.searchUsers(name);
            return ResponseEntity.ok(ApiResponse.success(users));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
} 
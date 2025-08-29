package com.example.pingpang.dto;

import com.example.pingpang.entity.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    
    private Long id;
    private String username;
    private String email;
    private String realName;
    private String phone;
    private User.UserRole role;
    private User.Gender gender;
    private Integer age;
    private Integer level;
    private Integer points;
    private Integer wins;
    private Integer losses;
    private User.UserStatus status;
    private Double winRate;
    
    // 从Entity转换为DTO
    public static UserDTO fromEntity(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRealName(user.getRealName());
        dto.setPhone(user.getPhone());
        dto.setRole(user.getRole());
        dto.setGender(user.getGender());
        dto.setAge(user.getAge());
        dto.setLevel(user.getLevel());
        dto.setPoints(user.getPoints());
        dto.setWins(user.getWins());
        dto.setLosses(user.getLosses());
        dto.setStatus(user.getStatus());
        dto.setWinRate(user.getWinRate());
        return dto;
    }
} 
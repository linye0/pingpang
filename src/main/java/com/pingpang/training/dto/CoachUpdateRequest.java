package com.pingpang.training.dto;

import com.pingpang.training.enums.CoachLevel;
import com.pingpang.training.enums.Gender;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.validation.constraints.Min;
import javax.validation.constraints.Max;

public class CoachUpdateRequest {
    
    @Size(max = 10, message = "姓名长度不能超过10个字符")
    private String realName;
    
    private Gender gender;
    
    @Min(value = 1, message = "年龄必须大于0")
    @Max(value = 100, message = "年龄不能超过100")
    private Integer age;
    
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "电话号码格式不正确")
    private String phone;
    
    @Email(message = "邮箱格式不正确")
    private String email;
    
    private CoachLevel level;
    
    @Size(max = 1000, message = "成就描述长度不能超过1000个字符")
    private String achievements;
    
    private String avatar;
    
    // Constructors
    public CoachUpdateRequest() {}
    
    // Getters and Setters
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public CoachLevel getLevel() { return level; }
    public void setLevel(CoachLevel level) { this.level = level; }
    
    public String getAchievements() { return achievements; }
    public void setAchievements(String achievements) { this.achievements = achievements; }
    
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
} 
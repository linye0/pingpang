package com.pingpang.training.enums;

public enum UserRole {
    SUPER_ADMIN("超级管理员"),
    CAMPUS_ADMIN("校区管理员"),
    STUDENT("学员"),
    COACH("教练员");
    
    private final String description;
    
    UserRole(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 
package com.pingpang.training.enums;

public enum ApprovalStatus {
    PENDING("待审核"),
    APPROVED("已通过"),
    REJECTED("已拒绝");
    
    private final String description;
    
    ApprovalStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 
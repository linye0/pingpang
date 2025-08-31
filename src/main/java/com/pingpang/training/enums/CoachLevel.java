package com.pingpang.training.enums;

import java.math.BigDecimal;

public enum CoachLevel {
    MASTER("特级教练员", new BigDecimal("300")),
    SENIOR("高级教练员", new BigDecimal("200")),
    INTERMEDIATE("中级教练员", new BigDecimal("150")),
    JUNIOR("初级教练员", new BigDecimal("80"));
    
    private final String description;
    private final BigDecimal hourlyRate;
    
    CoachLevel(String description, BigDecimal hourlyRate) {
        this.description = description;
        this.hourlyRate = hourlyRate;
    }
    
    public String getDescription() {
        return description;
    }
    
    public BigDecimal getHourlyRate() {
        return hourlyRate;
    }
} 
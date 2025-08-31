package com.pingpang.training.enums;

public enum CompetitionGroup {
    GROUP_A("甲组"),
    GROUP_B("乙组"),
    GROUP_C("丙组");
    
    private final String description;
    
    CompetitionGroup(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 
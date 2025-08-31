package com.pingpang.training.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.pingpang.training.config.ISO8601LocalDateTimeDeserializer;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class BookingRequest {
    
    @NotNull(message = "教练ID不能为空")
    private Long coachId;
    
    @NotNull(message = "开始时间不能为空")
    @JsonDeserialize(using = ISO8601LocalDateTimeDeserializer.class)
    private LocalDateTime startTime;
    
    @NotNull(message = "结束时间不能为空")
    @JsonDeserialize(using = ISO8601LocalDateTimeDeserializer.class)
    private LocalDateTime endTime;
    
    private String tableNumber; // 可选，系统自动分配
    
    private String remarks;
    
    public BookingRequest() {}
    
    public BookingRequest(Long coachId, LocalDateTime startTime, LocalDateTime endTime) {
        this.coachId = coachId;
        this.startTime = startTime;
        this.endTime = endTime;
    }
    
    // Getters and Setters
    public Long getCoachId() { return coachId; }
    public void setCoachId(Long coachId) { this.coachId = coachId; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }
    
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    
    // 兼容性方法 - notes字段的别名
    public String getNotes() { return remarks; }
    public void setNotes(String notes) { this.remarks = notes; }
} 
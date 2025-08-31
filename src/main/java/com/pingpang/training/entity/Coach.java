package com.pingpang.training.entity;

import com.pingpang.training.enums.CoachLevel;
import com.pingpang.training.enums.ApprovalStatus;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "coach")
@PrimaryKeyJoinColumn(name = "id")
public class Coach extends User {
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CoachLevel level;
    
    @Size(max = 1000, message = "获奖信息长度不能超过1000个字符")
    @Column(length = 1000)
    private String achievements;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;
    
    @Column(nullable = false)
    private Integer cancellationCount = 0;
    
    @Column(nullable = false)
    private LocalDateTime lastCancellationReset;
    
    @JsonIgnore
    @OneToMany(mappedBy = "coach", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CoachStudentRelation> studentRelations;
    
    @JsonIgnore
    @OneToMany(mappedBy = "coach", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CourseBooking> courseBookings;
    
    public Coach() {
        super();
        this.lastCancellationReset = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    }
    
    // Getters and Setters
    public CoachLevel getLevel() { return level; }
    public void setLevel(CoachLevel level) { this.level = level; }
    
    public String getAchievements() { return achievements; }
    public void setAchievements(String achievements) { this.achievements = achievements; }
    
    public ApprovalStatus getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }
    
    public Integer getCancellationCount() { return cancellationCount; }
    public void setCancellationCount(Integer cancellationCount) { this.cancellationCount = cancellationCount; }
    
    public LocalDateTime getLastCancellationReset() { return lastCancellationReset; }
    public void setLastCancellationReset(LocalDateTime lastCancellationReset) { this.lastCancellationReset = lastCancellationReset; }
    
    public List<CoachStudentRelation> getStudentRelations() { return studentRelations; }
    public void setStudentRelations(List<CoachStudentRelation> studentRelations) { this.studentRelations = studentRelations; }
    
    public List<CourseBooking> getCourseBookings() { return courseBookings; }
    public void setCourseBookings(List<CourseBooking> courseBookings) { this.courseBookings = courseBookings; }
    
    // 兼容性字段 - 专长（使用获奖信息作为专长）
    public String getSpecialty() { return achievements != null ? achievements : ""; }
    public void setSpecialty(String specialty) { this.achievements = specialty; }
} 
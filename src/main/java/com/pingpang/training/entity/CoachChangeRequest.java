package com.pingpang.training.entity;

import com.pingpang.training.enums.ApprovalStatus;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "coach_change_request")
public class CoachChangeRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    @NotNull
    private Student student;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_coach_id", nullable = false)
    @NotNull
    private Coach currentCoach;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_coach_id", nullable = false)
    @NotNull
    private Coach newCoach;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_admin_id", nullable = false)
    @NotNull
    private User campusAdmin;
    
    @Column(columnDefinition = "TEXT")
    private String reason;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus currentCoachApproval = ApprovalStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus newCoachApproval = ApprovalStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus adminApproval = ApprovalStatus.PENDING;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus finalStatus = ApprovalStatus.PENDING;
    
    private String currentCoachComment;
    private String newCoachComment;
    private String adminComment;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    private LocalDateTime processedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    

    public CoachChangeRequest() {}
    
    public CoachChangeRequest(Student student, Coach currentCoach, Coach newCoach, User campusAdmin, String reason) {
        this.student = student;
        this.currentCoach = currentCoach;
        this.newCoach = newCoach;
        this.campusAdmin = campusAdmin;
        this.reason = reason;
    }
    

    public boolean isAllApproved() {
        return currentCoachApproval == ApprovalStatus.APPROVED &&
               newCoachApproval == ApprovalStatus.APPROVED &&
               adminApproval == ApprovalStatus.APPROVED;
    }
    

    public boolean isAnyRejected() {
        return currentCoachApproval == ApprovalStatus.REJECTED ||
               newCoachApproval == ApprovalStatus.REJECTED ||
               adminApproval == ApprovalStatus.REJECTED;
    }
    

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    
    public Coach getCurrentCoach() { return currentCoach; }
    public void setCurrentCoach(Coach currentCoach) { this.currentCoach = currentCoach; }
    
    public Coach getNewCoach() { return newCoach; }
    public void setNewCoach(Coach newCoach) { this.newCoach = newCoach; }
    
    public User getCampusAdmin() { return campusAdmin; }
    public void setCampusAdmin(User campusAdmin) { this.campusAdmin = campusAdmin; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public ApprovalStatus getCurrentCoachApproval() { return currentCoachApproval; }
    public void setCurrentCoachApproval(ApprovalStatus currentCoachApproval) { this.currentCoachApproval = currentCoachApproval; }
    
    public ApprovalStatus getNewCoachApproval() { return newCoachApproval; }
    public void setNewCoachApproval(ApprovalStatus newCoachApproval) { this.newCoachApproval = newCoachApproval; }
    
    public ApprovalStatus getAdminApproval() { return adminApproval; }
    public void setAdminApproval(ApprovalStatus adminApproval) { this.adminApproval = adminApproval; }
    
    public ApprovalStatus getFinalStatus() { return finalStatus; }
    public void setFinalStatus(ApprovalStatus finalStatus) { this.finalStatus = finalStatus; }
    
    public String getCurrentCoachComment() { return currentCoachComment; }
    public void setCurrentCoachComment(String currentCoachComment) { this.currentCoachComment = currentCoachComment; }
    
    public String getNewCoachComment() { return newCoachComment; }
    public void setNewCoachComment(String newCoachComment) { this.newCoachComment = newCoachComment; }
    
    public String getAdminComment() { return adminComment; }
    public void setAdminComment(String adminComment) { this.adminComment = adminComment; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
} 
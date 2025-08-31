package com.pingpang.training.entity;

import javax.persistence.*;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_evaluation")
public class CourseEvaluation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private CourseBooking booking;
    
    @Size(max = 1000, message = "学员评价长度不能超过1000个字符")
    @Column(length = 1000)
    private String studentEvaluation;
    
    @Size(max = 1000, message = "教练评价长度不能超过1000个字符")
    @Column(length = 1000)
    private String coachEvaluation;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    

    public CourseEvaluation() {}
    
    public CourseEvaluation(CourseBooking booking) {
        this.booking = booking;
    }
    

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public CourseBooking getBooking() { return booking; }
    public void setBooking(CourseBooking booking) { this.booking = booking; }
    
    public String getStudentEvaluation() { return studentEvaluation; }
    public void setStudentEvaluation(String studentEvaluation) { this.studentEvaluation = studentEvaluation; }
    
    public String getCoachEvaluation() { return coachEvaluation; }
    public void setCoachEvaluation(String coachEvaluation) { this.coachEvaluation = coachEvaluation; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    // 兼容性字段  学员评分
    @Column(name = "student_rating")
    private Integer studentRating;
    
    public Integer getStudentRating() { return studentRating; }
    public void setStudentRating(Integer studentRating) { this.studentRating = studentRating; }
} 
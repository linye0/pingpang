package com.pingpang.training.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pingpang.training.enums.CompetitionGroup;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "competition_registration")
public class CompetitionRegistration {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    @JsonIgnoreProperties("registrations")
    private MonthlyCompetition competition;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "competition_group", nullable = false)
    private CompetitionGroup competitionGroup;
    
    @Column(name = "registration_time", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    

    public CompetitionRegistration() {}
    
    public CompetitionRegistration(MonthlyCompetition competition, Student student, CompetitionGroup competitionGroup) {
        this.competition = competition;
        this.student = student;
        this.competitionGroup = competitionGroup;
    }
    

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public MonthlyCompetition getCompetition() { return competition; }
    public void setCompetition(MonthlyCompetition competition) { this.competition = competition; }
    
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    
    public CompetitionGroup getCompetitionGroup() { return competitionGroup; }
    public void setCompetitionGroup(CompetitionGroup competitionGroup) { this.competitionGroup = competitionGroup; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
} 
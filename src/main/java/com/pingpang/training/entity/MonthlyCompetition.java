package com.pingpang.training.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "monthly_competition")
public class MonthlyCompetition {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "比赛名称不能为空")
    @Column(nullable = false)
    private String name;
    
    @NotNull(message = "比赛时间不能为空")
    @Column(name = "competition_date", nullable = false)
    private LocalDateTime competitionDate;
    
    @Column(name = "registration_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal registrationFee = new BigDecimal("30");
    
    @Column(name = "registration_open", nullable = false)
    private Boolean registrationOpen = true;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campus_id", nullable = false)
    private Campus campus;
    
    @Column(length = 1000)
    private String description;
    
    @Column(name = "registration_start_date")
    private LocalDateTime registrationStartDate;
    
    @Column(name = "registration_end_date")
    private LocalDateTime registrationEndDate;
    
    @Column(name = "max_participants")
    private Integer maxParticipants = 32;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "competition", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("competition")
    private List<CompetitionRegistration> registrations;
    
    @OneToMany(mappedBy = "competition", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("competition")
    private List<CompetitionMatch> matches;
    
    @Column(name = "schedule_generated", nullable = false)
    private Boolean scheduleGenerated = false;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public MonthlyCompetition() {}
    
    public MonthlyCompetition(String name, LocalDateTime competitionDate, Campus campus) {
        this.name = name;
        this.competitionDate = competitionDate;
        this.campus = campus;
    }
    

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public LocalDateTime getCompetitionDate() { return competitionDate; }
    public void setCompetitionDate(LocalDateTime competitionDate) { this.competitionDate = competitionDate; }
    
    public BigDecimal getRegistrationFee() { return registrationFee; }
    public void setRegistrationFee(BigDecimal registrationFee) { this.registrationFee = registrationFee; }
    
    public Boolean getRegistrationOpen() { return registrationOpen; }
    public void setRegistrationOpen(Boolean registrationOpen) { this.registrationOpen = registrationOpen; }
    
    public Campus getCampus() { return campus; }
    public void setCampus(Campus campus) { this.campus = campus; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<CompetitionRegistration> getRegistrations() { return registrations; }
    public void setRegistrations(List<CompetitionRegistration> registrations) { this.registrations = registrations; }
    
    public LocalDateTime getRegistrationStartDate() { return registrationStartDate; }
    public void setRegistrationStartDate(LocalDateTime registrationStartDate) { this.registrationStartDate = registrationStartDate; }
    
    public LocalDateTime getRegistrationEndDate() { return registrationEndDate; }
    public void setRegistrationEndDate(LocalDateTime registrationEndDate) { this.registrationEndDate = registrationEndDate; }
    
    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }
    
    public List<CompetitionMatch> getMatches() { return matches; }
    public void setMatches(List<CompetitionMatch> matches) { this.matches = matches; }
    
    public Boolean getScheduleGenerated() { return scheduleGenerated; }
    public void setScheduleGenerated(Boolean scheduleGenerated) { this.scheduleGenerated = scheduleGenerated; }
} 
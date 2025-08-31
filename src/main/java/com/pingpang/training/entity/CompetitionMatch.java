package com.pingpang.training.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pingpang.training.enums.CompetitionGroup;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "competition_match")
public class CompetitionMatch {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competition_id", nullable = false)
    @JsonIgnoreProperties("matches")
    private MonthlyCompetition competition;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "competition_group", nullable = false)
    private CompetitionGroup competitionGroup;
    
    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;
    
    @Column(name = "match_number", nullable = false)
    private Integer matchNumber;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player1_id")
    private Student player1;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player2_id")
    private Student player2;
    
    @Column(name = "table_number")
    private String tableNumber;
    
    @Column(name = "is_bye", nullable = false)
    private Boolean isBye = false;
    
    @Column(name = "match_type")
    private String matchType; // "全循环", "小组赛", "淘汰赛"
    
    @Column(name = "group_name")
    private String groupName; // 对于分组比赛，记录小组名称
    
    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;
    
    @Column(name = "player1_score")
    private Integer player1Score;
    
    @Column(name = "player2_score")
    private Integer player2Score;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Student winner;
    
    @Column(name = "match_status")
    private String matchStatus = "SCHEDULED"; // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
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
    
    // Constructors
    public CompetitionMatch() {}
    
    public CompetitionMatch(MonthlyCompetition competition, CompetitionGroup competitionGroup, 
                           Integer roundNumber, Integer matchNumber, Student player1, Student player2) {
        this.competition = competition;
        this.competitionGroup = competitionGroup;
        this.roundNumber = roundNumber;
        this.matchNumber = matchNumber;
        this.player1 = player1;
        this.player2 = player2;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public MonthlyCompetition getCompetition() { return competition; }
    public void setCompetition(MonthlyCompetition competition) { this.competition = competition; }
    
    public CompetitionGroup getCompetitionGroup() { return competitionGroup; }
    public void setCompetitionGroup(CompetitionGroup competitionGroup) { this.competitionGroup = competitionGroup; }
    
    public Integer getRoundNumber() { return roundNumber; }
    public void setRoundNumber(Integer roundNumber) { this.roundNumber = roundNumber; }
    
    public Integer getMatchNumber() { return matchNumber; }
    public void setMatchNumber(Integer matchNumber) { this.matchNumber = matchNumber; }
    
    public Student getPlayer1() { return player1; }
    public void setPlayer1(Student player1) { this.player1 = player1; }
    
    public Student getPlayer2() { return player2; }
    public void setPlayer2(Student player2) { this.player2 = player2; }
    
    public String getTableNumber() { return tableNumber; }
    public void setTableNumber(String tableNumber) { this.tableNumber = tableNumber; }
    
    public Boolean getIsBye() { return isBye; }
    public void setIsBye(Boolean isBye) { this.isBye = isBye; }
    
    public String getMatchType() { return matchType; }
    public void setMatchType(String matchType) { this.matchType = matchType; }
    
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    
    public LocalDateTime getScheduledTime() { return scheduledTime; }
    public void setScheduledTime(LocalDateTime scheduledTime) { this.scheduledTime = scheduledTime; }
    
    public Integer getPlayer1Score() { return player1Score; }
    public void setPlayer1Score(Integer player1Score) { this.player1Score = player1Score; }
    
    public Integer getPlayer2Score() { return player2Score; }
    public void setPlayer2Score(Integer player2Score) { this.player2Score = player2Score; }
    
    public Student getWinner() { return winner; }
    public void setWinner(Student winner) { this.winner = winner; }
    
    public String getMatchStatus() { return matchStatus; }
    public void setMatchStatus(String matchStatus) { this.matchStatus = matchStatus; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
} 
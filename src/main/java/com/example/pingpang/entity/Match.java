package com.example.pingpang.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Match {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // 选手1
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player1_id", nullable = false)
    @NotNull(message = "选手1不能为空")
    private User player1;
    
    // 选手2
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player2_id", nullable = false)
    @NotNull(message = "选手2不能为空")
    private User player2;
    
    // 比赛类型
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchType matchType = MatchType.FRIENDLY;
    
    // 比赛状态
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus status = MatchStatus.SCHEDULED;
    
    // 比赛时间
    @Column(name = "match_time")
    private LocalDateTime matchTime;
    
    // 比赛地点
    private String venue;
    
    // 选手1得分
    @Column(name = "player1_score")
    private Integer player1Score = 0;
    
    // 选手2得分
    @Column(name = "player2_score")
    private Integer player2Score = 0;
    
    // 获胜者
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private User winner;
    
    // 比赛备注
    private String remarks;
    
    // 裁判
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referee_id")
    private User referee;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
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
    
    public enum MatchType {
        FRIENDLY("友谊赛"),
        RANKING("排位赛"),
        TOURNAMENT("锦标赛"),
        TRAINING("训练赛");
        
        private final String description;
        
        MatchType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public enum MatchStatus {
        SCHEDULED("已安排"),
        IN_PROGRESS("进行中"),
        COMPLETED("已完成"),
        CANCELLED("已取消");
        
        private final String description;
        
        MatchStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
} 
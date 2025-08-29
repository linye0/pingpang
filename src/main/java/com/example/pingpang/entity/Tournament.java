package com.example.pingpang.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "tournaments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tournament {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "锦标赛名称不能为空")
    @Column(nullable = false)
    private String name;
    
    private String description;
    
    // 开始时间
    @NotNull(message = "开始时间不能为空")
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    // 结束时间
    @NotNull(message = "结束时间不能为空")
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    
    // 报名截止时间
    @Column(name = "registration_deadline")
    private LocalDateTime registrationDeadline;
    
    // 最大参赛人数
    @Column(name = "max_participants")
    private Integer maxParticipants = 32;
    
    // 比赛地点
    private String venue;
    
    // 锦标赛状态
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentStatus status = TournamentStatus.REGISTRATION;
    
    // 锦标赛类型
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentType type = TournamentType.SINGLE_ELIMINATION;
    
    // 组织者
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id")
    private User organizer;
    
    // 冠军
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "champion_id")
    private User champion;
    
    // 亚军
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "runner_up_id")
    private User runnerUp;
    
    // 参赛选手
    @ManyToMany
    @JoinTable(
        name = "tournament_participants",
        joinColumns = @JoinColumn(name = "tournament_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private List<User> participants;
    
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
    
    public enum TournamentStatus {
        REGISTRATION("报名中"),
        IN_PROGRESS("进行中"),
        COMPLETED("已完成"),
        CANCELLED("已取消");
        
        private final String description;
        
        TournamentStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public enum TournamentType {
        SINGLE_ELIMINATION("单败淘汰"),
        DOUBLE_ELIMINATION("双败淘汰"),
        ROUND_ROBIN("循环赛");
        
        private final String description;
        
        TournamentType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
} 
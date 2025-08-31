package com.pingpang.training.controller;

import com.pingpang.training.dto.ApiResponse;
import com.pingpang.training.entity.CompetitionMatch;
import com.pingpang.training.entity.CompetitionRegistration;
import com.pingpang.training.entity.MonthlyCompetition;
import com.pingpang.training.enums.CompetitionGroup;
import com.pingpang.training.repository.CompetitionMatchRepository;
import com.pingpang.training.repository.CompetitionRegistrationRepository;
import com.pingpang.training.repository.MonthlyCompetitionRepository;
import com.pingpang.training.security.UserDetailsImpl;
import com.pingpang.training.service.CompetitionService;
import com.pingpang.training.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/competition")
@CrossOrigin(origins = "*")
public class CompetitionController {
    
    @Autowired
    private CompetitionService competitionService;
    
    @Autowired
    private MonthlyCompetitionRepository monthlyCompetitionRepository;
    
    @Autowired
    private CompetitionRegistrationRepository competitionRegistrationRepository;
    
    @Autowired
    private CompetitionMatchRepository competitionMatchRepository;
    
    @Autowired
    private SystemLogService systemLogService;
    
    /**
     * 获取可报名的比赛列表
     */
    @GetMapping("/available")
    public ApiResponse<?> getAvailableCompetitions(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long campusId = userDetails.getUser().getCampus().getId();
            String campusName = userDetails.getUser().getCampus().getName();
            LocalDateTime now = LocalDateTime.now();
            
            System.out.println("=== 调试比赛查询 ===");
            System.out.println("用户: " + userDetails.getUser().getRealName());
            System.out.println("用户校区ID: " + campusId);
            System.out.println("用户校区名称: " + campusName);
            System.out.println("查询时间: " + now);
            
            List<MonthlyCompetition> competitions = monthlyCompetitionRepository
                .findUpcomingCompetitionsByCampus(campusId, now);
            
            System.out.println("查询到的比赛数量: " + competitions.size());
            for (MonthlyCompetition comp : competitions) {
                System.out.println("比赛: " + comp.getName() + 
                    ", 校区: " + comp.getCampus().getName() + 
                    ", 日期: " + comp.getCompetitionDate() +
                    ", 开放报名: " + comp.getRegistrationOpen());
            }
            
            return ApiResponse.success(competitions);
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResponse.error("获取可报名比赛失败: " + e.getMessage());
        }
    }
    
    /**
     * 学员报名比赛
     */
    @PostMapping("/{competitionId}/register")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<?> registerForCompetition(@PathVariable Long competitionId,
                                               @RequestParam CompetitionGroup group,
                                               @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            CompetitionRegistration registration = competitionService.registerForCompetition(
                competitionId, userDetails.getUser().getId(), group);
            
            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "COMPETITION_REGISTER", 
                "报名比赛ID: " + competitionId + " 组别: " + group.getDescription());
            
            return ApiResponse.success("比赛报名成功", registration);
        } catch (Exception e) {
            return ApiResponse.error("报名失败: " + e.getMessage());
        }
    }
    
    /**
     * 取消比赛报名
     */
    @DeleteMapping("/registration/{registrationId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<?> cancelRegistration(@PathVariable Long registrationId,
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            competitionService.cancelCompetitionRegistration(registrationId, userDetails.getUser().getId());
            
            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "COMPETITION_CANCEL", 
                "取消报名ID: " + registrationId);
            
            return ApiResponse.success("取消报名成功");
        } catch (Exception e) {
            return ApiResponse.error("取消报名失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取学员的比赛报名记录
     */
    @GetMapping("/my-registrations")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<?> getMyRegistrations(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<CompetitionRegistration> registrations = competitionRegistrationRepository
                .findByStudentId(userDetails.getUser().getId());
            return ApiResponse.success(registrations);
        } catch (Exception e) {
            return ApiResponse.error("获取参赛记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取比赛详细信息
     */
    @GetMapping("/{competitionId}")
    public ApiResponse<?> getCompetitionDetail(@PathVariable Long competitionId,
                                             @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            MonthlyCompetition competition = monthlyCompetitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("比赛不存在"));
                
            // 检查权限：只能查看本校区的比赛
            if (!competition.getCampus().getId().equals(userDetails.getUser().getCampus().getId())) {
                return ApiResponse.error("无权限查看其他校区的比赛");
            }
            
            return ApiResponse.success(competition);
        } catch (Exception e) {
            return ApiResponse.error("获取比赛详情失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取比赛完整赛程安排
     */
    @GetMapping("/{competitionId}/schedule")
    public ApiResponse<?> getCompetitionSchedule(@PathVariable Long competitionId,
                                               @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println("=== 调试比赛赛程查询 ===");
            System.out.println("比赛ID: " + competitionId);
            System.out.println("用户: " + userDetails.getUser().getRealName());
            System.out.println("用户校区: " + userDetails.getUser().getCampus().getName());
            
            MonthlyCompetition competition = monthlyCompetitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("比赛不存在"));
            
            System.out.println("比赛名称: " + competition.getName());
            System.out.println("比赛校区: " + competition.getCampus().getName());
            System.out.println("用户校区ID: " + userDetails.getUser().getCampus().getId());
            System.out.println("比赛校区ID: " + competition.getCampus().getId());
                
            // 检查权限：只能查看本校区的比赛
            if (!competition.getCampus().getId().equals(userDetails.getUser().getCampus().getId())) {
                System.out.println("权限检查失败：校区不匹配");
                return ApiResponse.error("无权限查看其他校区的比赛");
            }
            
            System.out.println("权限检查通过，获取赛程...");
            Map<String, Object> schedule = competitionService.getCompetitionSchedule(competitionId);
            System.out.println("赛程数据: " + (schedule != null ? "有数据" : "无数据"));
            
            return ApiResponse.success(schedule);
        } catch (Exception e) {
            System.out.println("获取比赛赛程异常: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("获取比赛赛程失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取学员在某场比赛中的对战安排
     */
    @GetMapping("/{competitionId}/my-matches")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<?> getMyMatches(@PathVariable Long competitionId,
                                     @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            MonthlyCompetition competition = monthlyCompetitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("比赛不存在"));
                
            // 检查权限：只能查看本校区的比赛
            if (!competition.getCampus().getId().equals(userDetails.getUser().getCampus().getId())) {
                return ApiResponse.error("无权限查看其他校区的比赛");
            }
            
            // 检查是否报名了该比赛
            boolean isRegistered = competitionRegistrationRepository
                .existsByCompetitionIdAndStudentId(competitionId, userDetails.getUser().getId());
            if (!isRegistered) {
                return ApiResponse.error("您未报名该比赛");
            }
            
            List<CompetitionMatch> matches = competitionService.getStudentMatches(
                competitionId, userDetails.getUser().getId());
            return ApiResponse.success(matches);
        } catch (Exception e) {
            return ApiResponse.error("获取个人对战安排失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成或重新生成比赛赛程（管理员功能）
     */
    @PostMapping("/{competitionId}/generate-schedule")
    @PreAuthorize("hasRole('CAMPUS_ADMIN') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> generateSchedule(@PathVariable Long competitionId,
                                         @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            MonthlyCompetition competition = monthlyCompetitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("比赛不存在"));
                
            // 校区管理员权限检查
            if (userDetails.getUser().getRole().name().equals("CAMPUS_ADMIN")) {
                if (!competition.getCampus().getId().equals(userDetails.getUser().getCampus().getId())) {
                    return ApiResponse.error("无权限管理其他校区的比赛");
                }
            }
            
            competitionService.generateAndSaveCompetitionSchedule(competitionId);
            
            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "COMPETITION_SCHEDULE_GENERATE", 
                "生成比赛赛程: " + competition.getName());
            
            return ApiResponse.success("比赛赛程生成成功");
        } catch (Exception e) {
            return ApiResponse.error("生成赛程失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取比赛的报名统计信息
     */
    @GetMapping("/{competitionId}/registration-stats")
    public ApiResponse<?> getRegistrationStats(@PathVariable Long competitionId,
                                             @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            MonthlyCompetition competition = monthlyCompetitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("比赛不存在"));
                
            // 检查权限：只能查看本校区的比赛
            if (!competition.getCampus().getId().equals(userDetails.getUser().getCampus().getId())) {
                return ApiResponse.error("无权限查看其他校区的比赛");
            }
            
            List<CompetitionRegistration> registrations = competitionRegistrationRepository
                .findByCompetitionId(competitionId);
            
            Map<String, Long> stats = registrations.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    reg -> reg.getCompetitionGroup().getDescription(),
                    java.util.stream.Collectors.counting()
                ));
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalRegistrations", registrations.size());
            result.put("groupStats", stats);
            result.put("competition", competition);
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("获取报名统计失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取某个组别的具体对战安排
     */
    @GetMapping("/{competitionId}/group/{group}/matches")
    public ApiResponse<?> getGroupMatches(@PathVariable Long competitionId,
                                        @PathVariable CompetitionGroup group,
                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            MonthlyCompetition competition = monthlyCompetitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("比赛不存在"));
                
            // 检查权限：只能查看本校区的比赛
            if (!competition.getCampus().getId().equals(userDetails.getUser().getCampus().getId())) {
                return ApiResponse.error("无权限查看其他校区的比赛");
            }
            
            List<CompetitionMatch> matches = competitionMatchRepository
                .findByCompetitionIdAndCompetitionGroupOrderByRoundNumberAscMatchNumberAsc(competitionId, group);
            
            Map<String, Object> result = new HashMap<>();
            result.put("group", group.getDescription());
            result.put("matches", matches);
            result.put("totalMatches", matches.size());
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("获取组别对战安排失败: " + e.getMessage());
        }
    }
} 
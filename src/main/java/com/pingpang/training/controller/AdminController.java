package com.pingpang.training.controller;

import com.pingpang.training.dto.ApiResponse;
import com.pingpang.training.dto.CoachUpdateRequest;
import com.pingpang.training.entity.*;
import com.pingpang.training.enums.ApprovalStatus;
import com.pingpang.training.enums.BookingStatus;
import com.pingpang.training.enums.MessageType;
import com.pingpang.training.enums.PaymentMethod;
import com.pingpang.training.enums.UserRole;
import com.pingpang.training.repository.*;
import com.pingpang.training.security.UserDetailsImpl;
import com.pingpang.training.service.CampusService;
import com.pingpang.training.service.CompetitionService;
import com.pingpang.training.service.SystemLogService;
import com.pingpang.training.service.SystemMessageService;
import com.pingpang.training.service.UserService;
import com.pingpang.training.service.CoachChangeService;
import com.pingpang.training.service.PaymentService;
import com.pingpang.training.utils.ValidationUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('CAMPUS_ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private CampusService campusService;

    @Autowired
    private SystemLogService systemLogService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CoachRepository coachRepository;

    @Autowired
    private CourseBookingRepository courseBookingRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private MonthlyCompetitionRepository monthlyCompetitionRepository;

    @Autowired
    private CompetitionRegistrationRepository competitionRegistrationRepository;

    @Autowired
    private CompetitionService competitionService;

    @Autowired
    private SystemMessageService systemMessageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CoachChangeService coachChangeService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private CourseEvaluationRepository courseEvaluationRepository;


    @GetMapping("/evaluations")
    public ApiResponse<?> getAllEvaluations(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(required = false) Long campusId,
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User admin = userDetails.getUser();
            List<CourseEvaluation> allEvaluations;
            
            if (admin.getRole() == UserRole.SUPER_ADMIN) {

                allEvaluations = courseEvaluationRepository.findAll();
            } else {

                if (admin.getCampus() == null) {
                    return ApiResponse.error("管理员未绑定校区");
                }
                Long adminCampusId = admin.getCampus().getId();
                allEvaluations = courseEvaluationRepository.findAll().stream()
                    .filter(eval -> eval.getBooking().getStudent().getCampus().getId().equals(adminCampusId) ||
                                   eval.getBooking().getCoach().getCampus().getId().equals(adminCampusId))
                    .collect(java.util.stream.Collectors.toList());
            }
            

            int startIndex = page * size;
            int endIndex = Math.min(startIndex + size, allEvaluations.size());
            
            List<CourseEvaluation> paginatedEvaluations = allEvaluations.subList(startIndex, endIndex);
            
            Map<String, Object> result = new HashMap<>();
            result.put("evaluations", paginatedEvaluations);
            result.put("totalElements", allEvaluations.size());
            result.put("totalPages", (int) Math.ceil((double) allEvaluations.size() / size));
            result.put("currentPage", page);
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            System.err.println("获取评价记录失败: " + e.getMessage());
            return ApiResponse.error("获取评价记录失败: " + e.getMessage());
        }
    }
    

    @GetMapping("/evaluations/statistics")
    public ApiResponse<?> getEvaluationStatistics(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User admin = userDetails.getUser();
            List<CourseEvaluation> allEvaluations;
            
            if (admin.getRole() == UserRole.SUPER_ADMIN) {
                allEvaluations = courseEvaluationRepository.findAll();
            } else {
                if (admin.getCampus() == null) {
                    return ApiResponse.error("管理员未绑定校区");
                }
                Long adminCampusId = admin.getCampus().getId();
                allEvaluations = courseEvaluationRepository.findAll().stream()
                    .filter(eval -> eval.getBooking().getStudent().getCampus().getId().equals(adminCampusId) ||
                                   eval.getBooking().getCoach().getCampus().getId().equals(adminCampusId))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            // 统计数据
            long totalEvaluations = allEvaluations.size();
            long studentEvaluationsCount = allEvaluations.stream()
                .filter(eval -> eval.getStudentEvaluation() != null && !eval.getStudentEvaluation().trim().isEmpty())
                .count();
            long coachEvaluationsCount = allEvaluations.stream()
                .filter(eval -> eval.getCoachEvaluation() != null && !eval.getCoachEvaluation().trim().isEmpty())
                .count();
            
            // 评分统计（仅学员评分）
            Double averageRating = allEvaluations.stream()
                .filter(eval -> eval.getStudentRating() != null && eval.getStudentRating() > 0)
                .mapToInt(CourseEvaluation::getStudentRating)
                .average()
                .orElse(0.0);
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalEvaluations", totalEvaluations);
            statistics.put("studentEvaluationsCount", studentEvaluationsCount);
            statistics.put("coachEvaluationsCount", coachEvaluationsCount);
            statistics.put("averageRating", Math.round(averageRating * 100.0) / 100.0);
            statistics.put("studentEvaluationRate", totalEvaluations > 0 ? 
                Math.round((double) studentEvaluationsCount / totalEvaluations * 100.0) / 100.0 : 0.0);
            statistics.put("coachEvaluationRate", totalEvaluations > 0 ? 
                Math.round((double) coachEvaluationsCount / totalEvaluations * 100.0) / 100.0 : 0.0);
            
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            System.err.println("获取评价统计失败: " + e.getMessage());
            return ApiResponse.error("获取评价统计失败: " + e.getMessage());
        }
    }
    

    @GetMapping("/evaluations/coach/{coachId}")
    public ApiResponse<?> getCoachEvaluations(@PathVariable Long coachId,
                                             @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User admin = userDetails.getUser();
            

            Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("教练不存在"));
            
            if (admin.getRole() == UserRole.CAMPUS_ADMIN) {
                if (admin.getCampus() == null || !coach.getCampus().getId().equals(admin.getCampus().getId())) {
                    return ApiResponse.error("无权限查看此教练的评价");
                }
            }
            
            List<CourseEvaluation> evaluations = courseEvaluationRepository.findByCoachId(coachId);
            return ApiResponse.success(evaluations);
        } catch (Exception e) {
            System.err.println("获取教练评价失败: " + e.getMessage());
            return ApiResponse.error("获取教练评价失败: " + e.getMessage());
        }
    }
    

    @GetMapping("/evaluations/student/{studentId}")
    public ApiResponse<?> getStudentEvaluations(@PathVariable Long studentId,
                                               @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User admin = userDetails.getUser();
            
            // 验证学员权限
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学员不存在"));
            
            if (admin.getRole() == UserRole.CAMPUS_ADMIN) {
                if (admin.getCampus() == null || !student.getCampus().getId().equals(admin.getCampus().getId())) {
                    return ApiResponse.error("无权限查看此学员的评价");
                }
            }
            
            List<CourseEvaluation> evaluations = courseEvaluationRepository.findByStudentId(studentId);
            return ApiResponse.success(evaluations);
        } catch (Exception e) {
            System.err.println("获取学员评价失败: " + e.getMessage());
            return ApiResponse.error("获取学员评价失败: " + e.getMessage());
        }
    }

    // 校区管理
    @PostMapping("/campus")
    public ApiResponse<?> createCampus(@Valid @RequestBody Campus campus,
                                      @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User admin = userDetails.getUser();
            return ApiResponse.success("校区创建成功", campusService.createCampus(campus, admin));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/campus/{id}")
    public ApiResponse<?> updateCampus(@PathVariable Long id, @Valid @RequestBody Campus campus,
                                      @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User admin = userDetails.getUser();
            campus.setId(id);
            return ApiResponse.success("校区更新成功", campusService.updateCampus(campus, admin));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/campus/{id}")
    public ApiResponse<?> deleteCampus(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User admin = userDetails.getUser();
            campusService.deleteCampus(id, admin);
            return ApiResponse.success("校区删除成功", null);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/campus")
    public ApiResponse<?> getAllCampuses() {
        try {
            return ApiResponse.success(campusService.getAllActiveCampuses());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 教练审核
    @GetMapping("/pending-coaches")
    public ApiResponse<?> getPendingCoaches(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User user = userDetails.getUser();
            Long campusId = null;
            

            if (user.getRole() == UserRole.CAMPUS_ADMIN) {
                if (user.getCampus() == null) {
                    return ApiResponse.error("管理员未绑定校区");
                }
                campusId = user.getCampus().getId();
            }
            
            return ApiResponse.success(userService.getPendingCoaches(campusId));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/approve-coach/{coachId}")
    public ApiResponse<?> approveCoach(@PathVariable Long coachId,
                                      @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User admin = userDetails.getUser();
            return ApiResponse.success("教练审核通过", userService.approveCoach(coachId, admin));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/reject-coach/{coachId}")
    public ApiResponse<?> rejectCoach(@PathVariable Long coachId,
                                     @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User admin = userDetails.getUser();
            return ApiResponse.success("教练审核拒绝", userService.rejectCoach(coachId, admin));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 系统日志
    @GetMapping("/logs")
    public ApiResponse<?> getSystemLogs(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User user = userDetails.getUser();
            Long campusId = null;
            
            // SUPER_ADMIN 可以查看所有校区的系统日志，CAMPUS_ADMIN 只能查看自己校区的
            if (user.getRole() == UserRole.CAMPUS_ADMIN) {
                if (user.getCampus() == null) {
                    return ApiResponse.error("管理员未绑定校区");
                }
                campusId = user.getCampus().getId();
            }
            
            return ApiResponse.success(systemLogService.getLogsByCampus(campusId));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    // 用户管理 - 新增
    @GetMapping("/users")
    public ApiResponse<?> getAllUsers(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User user = userDetails.getUser();
            List<User> users;
            

            if (user.getRole() == UserRole.SUPER_ADMIN) {
                users = userService.getAllUsers();
            } else {
                if (user.getCampus() == null) {
                    return ApiResponse.error("管理员未绑定校区");
                }
                users = userService.getUsersByCampusId(user.getCampus().getId());
            }
            
            return ApiResponse.success(users);
        } catch (Exception e) {
            return ApiResponse.error("获取用户列表失败");
        }
    }
    
    @PostMapping("/toggle-user-status/{userId}")
    public ApiResponse<?> toggleUserStatus(@PathVariable Long userId,
                                          @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User admin = userDetails.getUser();
            User targetUser = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            // 权限检查
            if (admin.getRole() == UserRole.CAMPUS_ADMIN) {
                if (admin.getCampus() == null || !admin.getCampus().getId().equals(targetUser.getCampus().getId())) {
                    return ApiResponse.error("无权限管理该用户");
                }
            }
            
            targetUser.setActive(!targetUser.getActive());
            userService.save(targetUser);
            
            String action = targetUser.getActive() ? "启用" : "禁用";
            systemLogService.log(admin, "USER_STATUS_TOGGLE", 
                action + "用户: " + targetUser.getRealName());
            
            return ApiResponse.success("用户状态已更新", targetUser);
        } catch (Exception e) {
            return ApiResponse.error("更新用户状态失败: " + e.getMessage());
        }
    }
    
    // 学员管理
    @GetMapping("/students")
    public ApiResponse<?> getCampusStudents(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User user = userDetails.getUser();
            List<Student> students;
            

            if (user.getRole() == UserRole.SUPER_ADMIN) {
                students = studentRepository.findAll();
            } else {
                if (user.getCampus() == null) {
                    return ApiResponse.error("管理员未绑定校区");
                }
                students = studentRepository.findByCampusId(user.getCampus().getId());
            }
            
            return ApiResponse.success(students);
        } catch (Exception e) {
            return ApiResponse.error("获取学员列表失败");
        }
    }
    
    @PostMapping("/toggle-student-status/{studentId}")
    public ApiResponse<?> toggleStudentStatus(@PathVariable Long studentId,
                                             @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User admin = userDetails.getUser();
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学员不存在"));
            
            // 权限检查
            if (admin.getRole() == UserRole.CAMPUS_ADMIN) {
                if (admin.getCampus() == null || !admin.getCampus().getId().equals(student.getCampus().getId())) {
                    return ApiResponse.error("无权限管理该学员");
                }
            }
            
            student.setActive(!student.getActive());
            studentRepository.save(student);
            
            String action = student.getActive() ? "启用" : "禁用";
            systemLogService.log(admin, "STUDENT_STATUS_TOGGLE", 
                action + "学员: " + student.getRealName());
            
            return ApiResponse.success("学员状态已更新", student);
        } catch (Exception e) {
            return ApiResponse.error("更新学员状态失败: " + e.getMessage());
        }
    }

    @GetMapping("/students/active")
    public ApiResponse<?> getActiveStudents(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long campusId = userDetails.getUser().getCampus().getId();
            List<Student> students = studentRepository.findByCampusIdAndActiveTrue(campusId);
            return ApiResponse.success(students);
        } catch (Exception e) {
            return ApiResponse.error("获取活跃学员失败");
        }
    }

    @PostMapping("/students/{studentId}/disable")
    public ApiResponse<?> disableStudent(@PathVariable Long studentId,
                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学员不存在"));

            if (!student.getCampus().getId().equals(userDetails.getUser().getCampus().getId())) {
                return ApiResponse.error("无权限管理该学员");
            }

            student.setActive(false);
            studentRepository.save(student);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "STUDENT_DISABLE", 
                "禁用学员: " + student.getRealName());

            return ApiResponse.success("学员已禁用", student);
        } catch (Exception e) {
            return ApiResponse.error("禁用学员失败: " + e.getMessage());
        }
    }

    @PostMapping("/students/{studentId}/enable")
    public ApiResponse<?> enableStudent(@PathVariable Long studentId,
                                       @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学员不存在"));

            if (!student.getCampus().getId().equals(userDetails.getUser().getCampus().getId())) {
                return ApiResponse.error("无权限管理该学员");
            }

            student.setActive(true);
            studentRepository.save(student);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "STUDENT_ENABLE", 
                "启用学员: " + student.getRealName());

            return ApiResponse.success("学员已启用", student);
        } catch (Exception e) {
            return ApiResponse.error("启用学员失败: " + e.getMessage());
        }
    }

    // 教练管理
    @GetMapping("/coaches")
    public ApiResponse<?> getCampusCoaches(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User user = userDetails.getUser();
            List<Coach> coaches;
            

            if (user.getRole() == UserRole.SUPER_ADMIN) {
                coaches = coachRepository.findAll();
            } else {
                if (user.getCampus() == null) {
                    return ApiResponse.error("管理员未绑定校区");
                }
                coaches = coachRepository.findByCampusId(user.getCampus().getId());
            }
            
            return ApiResponse.success(coaches);
        } catch (Exception e) {
            return ApiResponse.error("获取教练列表失败");
        }
    }
    
    @PostMapping("/toggle-coach-status/{coachId}")
    public ApiResponse<?> toggleCoachStatus(@PathVariable Long coachId,
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User admin = userDetails.getUser();
            Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("教练不存在"));
            
            // 权限检查
            if (admin.getRole() == UserRole.CAMPUS_ADMIN) {
                if (admin.getCampus() == null || !admin.getCampus().getId().equals(coach.getCampus().getId())) {
                    return ApiResponse.error("无权限管理该教练");
                }
            }
            
            coach.setActive(!coach.getActive());
            coachRepository.save(coach);
            
            String action = coach.getActive() ? "启用" : "禁用";
            systemLogService.log(admin, "COACH_STATUS_TOGGLE", 
                action + "教练: " + coach.getRealName());
            
            return ApiResponse.success("教练状态已更新", coach);
        } catch (Exception e) {
            return ApiResponse.error("更新教练状态失败: " + e.getMessage());
        }
    }

    @GetMapping("/coaches/approved")
    public ApiResponse<?> getApprovedCoaches(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long campusId = userDetails.getUser().getCampus().getId();
            List<Coach> coaches = coachRepository.findApprovedByCampusId(campusId);
            return ApiResponse.success(coaches);
        } catch (Exception e) {
            return ApiResponse.error("获取已审核教练失败");
        }
    }

    @GetMapping("/debug/coaches")
    public ApiResponse<?> debugCoaches() {
        try {
            List<Coach> allCoaches = coachRepository.findAll();
            Map<String, Object> debug = new HashMap<>();
            debug.put("totalCoaches", allCoaches.size());
            
            long activeCount = allCoaches.stream().filter(c -> c.getActive()).count();
            long approvedCount = allCoaches.stream()
                .filter(c -> c.getApprovalStatus() == ApprovalStatus.APPROVED).count();
            long activeAndApprovedCount = allCoaches.stream()
                .filter(c -> c.getActive() && c.getApprovalStatus() == ApprovalStatus.APPROVED).count();
            
            debug.put("activeCoaches", activeCount);
            debug.put("approvedCoaches", approvedCount);
            debug.put("activeAndApprovedCoaches", activeAndApprovedCount);
            debug.put("allCoachesData", allCoaches);
            
            return ApiResponse.success(debug);
        } catch (Exception e) {
            return ApiResponse.error("调试失败: " + e.getMessage());
        }
    }

    @PostMapping("/coaches/{coachId}/activate")
    public ApiResponse<?> activateCoach(@PathVariable Long coachId) {
        try {
            Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("教练不存在"));
            
            coach.setActive(true);
            coach.setApprovalStatus(ApprovalStatus.APPROVED);
            coachRepository.save(coach);
            
            return ApiResponse.success("教练已激活", coach);
        } catch (Exception e) {
            return ApiResponse.error("激活教练失败: " + e.getMessage());
        }
    }

    @GetMapping("/debug/competitions")
    public ApiResponse<?> debugCompetitions() {
        try {
            List<MonthlyCompetition> allCompetitions = monthlyCompetitionRepository.findAll();
            Map<String, Object> debug = new HashMap<>();
            debug.put("totalCompetitions", allCompetitions.size());
            
            long openCount = allCompetitions.stream()
                .filter(MonthlyCompetition::getRegistrationOpen).count();
            long upcomingCount = allCompetitions.stream()
                .filter(c -> c.getCompetitionDate().isAfter(LocalDateTime.now())).count();
            
            debug.put("openCompetitions", openCount);
            debug.put("upcomingCompetitions", upcomingCount);
            debug.put("allCompetitionsData", allCompetitions);
            
            return ApiResponse.success(debug);
        } catch (Exception e) {
            return ApiResponse.error("调试失败: " + e.getMessage());
        }
    }

    @PostMapping("/create-test-competition")
    public ApiResponse<?> createTestCompetition(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            MonthlyCompetition competition = new MonthlyCompetition();
            competition.setName("测试月赛 - " + LocalDateTime.now().getMonth());
            competition.setCampus(userDetails.getUser().getCampus());
            competition.setCompetitionDate(LocalDateTime.now().plusDays(7)); // 一周后
            competition.setRegistrationStartDate(LocalDateTime.now()); // 现在开始报名
            competition.setRegistrationEndDate(LocalDateTime.now().plusDays(5)); // 5天后截止报名
            competition.setRegistrationFee(BigDecimal.valueOf(50));
            competition.setMaxParticipants(32);
            competition.setRegistrationOpen(true);
            competition.setDescription("测试用比赛，用于验证系统功能");
            
            monthlyCompetitionRepository.save(competition);
            
            return ApiResponse.success("测试比赛创建成功", competition);
        } catch (Exception e) {
            return ApiResponse.error("创建测试比赛失败: " + e.getMessage());
        }
    }

    @PostMapping("/coaches/{coachId}/disable")
    public ApiResponse<?> disableCoach(@PathVariable Long coachId,
                                      @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("教练不存在"));

            if (!coach.getCampus().getId().equals(userDetails.getUser().getCampus().getId())) {
                return ApiResponse.error("无权限管理该教练");
            }

            coach.setActive(false);
            coachRepository.save(coach);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "COACH_DISABLE", 
                "禁用教练: " + coach.getRealName());

            return ApiResponse.success("教练已禁用", coach);
        } catch (Exception e) {
            return ApiResponse.error("禁用教练失败: " + e.getMessage());
        }
    }

    @PostMapping("/coaches/{coachId}/enable")
    public ApiResponse<?> enableCoach(@PathVariable Long coachId,
                                     @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("教练不存在"));

            if (!coach.getCampus().getId().equals(userDetails.getUser().getCampus().getId())) {
                return ApiResponse.error("无权限管理该教练");
            }

            coach.setActive(true);
            coachRepository.save(coach);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "COACH_ENABLE", 
                "启用教练: " + coach.getRealName());

            return ApiResponse.success("教练已启用", coach);
        } catch (Exception e) {
            return ApiResponse.error("启用教练失败: " + e.getMessage());
        }
    }

    // 课程管理
    @GetMapping("/bookings")
    public ApiResponse<?> getCampusBookings(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User user = userDetails.getUser();
            List<CourseBooking> bookings;
            

            if (user.getRole() == UserRole.SUPER_ADMIN) {
                bookings = courseBookingRepository.findAll();
            } else {
                if (user.getCampus() == null) {
                    return ApiResponse.error("管理员未绑定校区");
                }
                bookings = courseBookingRepository.findByCampusId(user.getCampus().getId());
            }
            
            return ApiResponse.success(bookings);
        } catch (Exception e) {
            return ApiResponse.error("获取课程预约失败");
        }
    }

    @GetMapping("/bookings/pending")
    public ApiResponse<?> getPendingBookings(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long campusId = userDetails.getUser().getCampus().getId();
            List<CourseBooking> bookings = courseBookingRepository.findByCampusIdAndStatus(campusId, BookingStatus.PENDING);
            return ApiResponse.success(bookings);
        } catch (Exception e) {
            return ApiResponse.error("获取待处理预约失败");
        }
    }

    @GetMapping("/bookings/today")
    public ApiResponse<?> getTodayBookings(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long campusId = userDetails.getUser().getCampus().getId();
            LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            List<CourseBooking> bookings = courseBookingRepository.findByCampusIdAndTimeRange(
                campusId, startOfDay, endOfDay);
            return ApiResponse.success(bookings);
        } catch (Exception e) {
            return ApiResponse.error("获取今日课程失败");
        }
    }

    // 财务管理
    @GetMapping("/payments")
    public ApiResponse<?> getCampusPayments(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long campusId = userDetails.getUser().getCampus().getId();
            List<PaymentRecord> payments = paymentRecordRepository.findByCampusId(campusId);
            return ApiResponse.success(payments);
        } catch (Exception e) {
            return ApiResponse.error("获取缴费记录失败");
        }
    }

    @GetMapping("/financial-summary")
    public ApiResponse<?> getFinancialSummary(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long campusId = userDetails.getUser().getCampus().getId();
            
            // 学员总数
            long totalStudents = studentRepository.countByCampusId(campusId);
            long activeStudents = studentRepository.countByCampusIdAndActiveTrue(campusId);
            
            // 教练总数
            long totalCoaches = coachRepository.countByCampusId(campusId);
            long approvedCoaches = coachRepository.countByCampusIdAndApprovalStatus(campusId, ApprovalStatus.APPROVED);
            
            // 课程统计
            long totalBookings = courseBookingRepository.countByCampusId(campusId);
            long completedBookings = courseBookingRepository.countByCampusIdAndStatus(campusId, BookingStatus.COMPLETED);
            
            // 财务统计
            BigDecimal totalRevenue = paymentRecordRepository.getTotalRevenueByCampusId(campusId);
            long totalPayments = paymentRecordRepository.countByCampusId(campusId);
            
            // 比赛统计
            long totalCompetitions = monthlyCompetitionRepository.countByCampusId(campusId);
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalStudents", totalStudents);
            statistics.put("activeStudents", activeStudents);
            statistics.put("totalCoaches", totalCoaches);
            statistics.put("approvedCoaches", approvedCoaches);
            statistics.put("totalBookings", totalBookings);
            statistics.put("completedBookings", completedBookings);
            statistics.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
            statistics.put("totalPayments", totalPayments);
            statistics.put("totalCompetitions", totalCompetitions);
            
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            return ApiResponse.error("获取财务汇总失败");
        }
    }

    // 比赛管理
    @GetMapping("/competitions")
    public ApiResponse<?> getCampusCompetitions(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long campusId = userDetails.getUser().getCampus().getId();
            List<MonthlyCompetition> competitions = monthlyCompetitionRepository.findByCampusId(campusId);
            return ApiResponse.success(competitions);
        } catch (Exception e) {
            return ApiResponse.error("获取比赛列表失败");
        }
    }

    @PostMapping("/competitions")
    public ApiResponse<?> createCompetition(@Valid @RequestBody MonthlyCompetition competition,
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Campus campus = userDetails.getUser().getCampus();
            competition.setCampus(campus);
            
            MonthlyCompetition savedCompetition = monthlyCompetitionRepository.save(competition);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "COMPETITION_CREATE", 
                "创建比赛: " + competition.getName());

            return ApiResponse.success("比赛创建成功", savedCompetition);
        } catch (Exception e) {
            return ApiResponse.error("创建比赛失败: " + e.getMessage());
        }
    }

    @PutMapping("/competitions/{competitionId}")
    public ApiResponse<?> updateCompetition(@PathVariable Long competitionId,
                                           @Valid @RequestBody MonthlyCompetition competition,
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            MonthlyCompetition existing = monthlyCompetitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("比赛不存在"));

            if (!existing.getCampus().getId().equals(userDetails.getUser().getCampus().getId())) {
                return ApiResponse.error("无权限管理该比赛");
            }

            existing.setName(competition.getName());
            existing.setCompetitionDate(competition.getCompetitionDate());
            existing.setRegistrationFee(competition.getRegistrationFee());
            existing.setRegistrationOpen(competition.getRegistrationOpen());
            existing.setDescription(competition.getDescription());

            MonthlyCompetition savedCompetition = monthlyCompetitionRepository.save(existing);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "COMPETITION_UPDATE", 
                "更新比赛: " + competition.getName());

            return ApiResponse.success("比赛更新成功", savedCompetition);
        } catch (Exception e) {
            return ApiResponse.error("更新比赛失败: " + e.getMessage());
        }
    }

    @PostMapping("/competitions/{competitionId}/close-registration")
    public ApiResponse<?> closeCompetitionRegistration(@PathVariable Long competitionId,
                                                      @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            MonthlyCompetition competition = monthlyCompetitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("比赛不存在"));

            if (!competition.getCampus().getId().equals(userDetails.getUser().getCampus().getId())) {
                return ApiResponse.error("无权限管理该比赛");
            }

            competition.setRegistrationOpen(false);
            monthlyCompetitionRepository.save(competition);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "COMPETITION_CLOSE_REG", 
                "关闭比赛报名: " + competition.getName());

            return ApiResponse.success("比赛报名已关闭", competition);
        } catch (Exception e) {
            return ApiResponse.error("关闭报名失败: " + e.getMessage());
        }
    }

    @GetMapping("/competitions/{competitionId}/registrations")
    public ApiResponse<?> getCompetitionRegistrations(@PathVariable Long competitionId,
                                                     @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            MonthlyCompetition competition = monthlyCompetitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("比赛不存在"));

            if (!competition.getCampus().getId().equals(userDetails.getUser().getCampus().getId())) {
                return ApiResponse.error("无权限查看该比赛报名");
            }

            List<CompetitionRegistration> registrations = competitionRegistrationRepository.findByCompetitionId(competitionId);
            return ApiResponse.success(registrations);
        } catch (Exception e) {
            return ApiResponse.error("获取比赛报名失败");
        }
    }

    // 数据导出功能
    @GetMapping("/export/students")
    public ApiResponse<?> exportStudents(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long campusId = userDetails.getUser().getCampus().getId();
            List<Student> students = studentRepository.findByCampusId(campusId);
            
            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "DATA_EXPORT", "导出学员数据");
            
            return ApiResponse.success("学员数据导出成功", students);
        } catch (Exception e) {
            return ApiResponse.error("导出学员数据失败");
        }
    }

    @GetMapping("/export/coaches")
    public ApiResponse<?> exportCoaches(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long campusId = userDetails.getUser().getCampus().getId();
            List<Coach> coaches = coachRepository.findByCampusId(campusId);
            
            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "DATA_EXPORT", "导出教练数据");
            
            return ApiResponse.success("教练数据导出成功", coaches);
        } catch (Exception e) {
            return ApiResponse.error("导出教练数据失败");
        }
    }

    @GetMapping("/export/bookings")
    public ApiResponse<?> exportBookings(@RequestParam(required = false) String startDate,
                                        @RequestParam(required = false) String endDate,
                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long campusId = userDetails.getUser().getCampus().getId();
            List<CourseBooking> bookings;
            
            if (startDate != null && endDate != null) {
                LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
                LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
                bookings = courseBookingRepository.findByCampusIdAndTimeRange(campusId, start, end);
            } else {
                bookings = courseBookingRepository.findByCampusId(campusId);
            }
            
            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "DATA_EXPORT", "导出课程数据");
            
            return ApiResponse.success("课程数据导出成功", bookings);
        } catch (Exception e) {
            return ApiResponse.error("导出课程数据失败");
        }
    }

    // 比赛管理
    @GetMapping("/competitions/{competitionId}/schedule")
    @PreAuthorize("hasRole('CAMPUS_ADMIN') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getCompetitionSchedule(@PathVariable Long competitionId,
                                               @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // 验证权限：校区管理员只能查看本校区比赛
            MonthlyCompetition competition = monthlyCompetitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("比赛不存在"));
            
            if (userDetails.getUser().getRole() == UserRole.CAMPUS_ADMIN) {
                if (!competition.getCampus().getId().equals(userDetails.getUser().getCampus().getId())) {
                    return ApiResponse.error("无权限查看其他校区的比赛");
                }
            }
            
            // 生成比赛赛程
            Map<String, Object> schedule = competitionService.generateCompetitionSchedule(competitionId);
            
            return ApiResponse.success("比赛赛程", schedule);
        } catch (Exception e) {
            return ApiResponse.error("获取比赛赛程失败: " + e.getMessage());
        }
    }

    @PostMapping("/competitions/{competitionId}/generate-schedule")
    @PreAuthorize("hasRole('CAMPUS_ADMIN') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> generateCompetitionSchedule(@PathVariable Long competitionId,
                                                    @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            MonthlyCompetition competition = monthlyCompetitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("比赛不存在"));
            
            if (userDetails.getUser().getRole() == UserRole.CAMPUS_ADMIN) {
                if (!competition.getCampus().getId().equals(userDetails.getUser().getCampus().getId())) {
                    return ApiResponse.error("无权限操作其他校区的比赛");
                }
            }
            
            // 检查报名是否截止
            if (competition.getRegistrationOpen()) {
                return ApiResponse.error("比赛报名尚未截止，无法生成赛程");
            }
            
            // 生成比赛赛程
            Map<String, Object> schedule = competitionService.generateCompetitionSchedule(competitionId);
            
            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "COMPETITION_SCHEDULE", 
                "生成比赛赛程: " + competition.getName());
            
            return ApiResponse.success("比赛赛程已生成", schedule);
        } catch (Exception e) {
            return ApiResponse.error("生成比赛赛程失败: " + e.getMessage());
        }
    }

    // 消息管理功能
    
    @GetMapping("/messages")
    public ApiResponse<?> getCampusMessages(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User user = userDetails.getUser();
            List<SystemMessage> messages;
            
            if (user.getRole() == UserRole.SUPER_ADMIN) {

                messages = systemMessageService.getUserMessages(user.getId());
            } else {
                // 校区管理员查看自己的消息
                messages = systemMessageService.getUserMessages(user.getId());
            }
            
            return ApiResponse.success(messages);
        } catch (Exception e) {
            return ApiResponse.error("获取消息列表失败");
        }
    }
    
    @GetMapping("/messages/unread")
    public ApiResponse<?> getUnreadMessages(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long userId = userDetails.getUser().getId();
            List<SystemMessage> unreadMessages = systemMessageService.getUnreadMessages(userId);
            return ApiResponse.success(unreadMessages);
        } catch (Exception e) {
            return ApiResponse.error("获取未读消息失败");
        }
    }
    
    @PostMapping("/messages/{messageId}/read")
    public ApiResponse<?> markMessageAsRead(@PathVariable Long messageId,
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long userId = userDetails.getUser().getId();
            SystemMessage message = systemMessageService.markAsRead(messageId, userId);
            return ApiResponse.success("消息已标记为已读", message);
        } catch (Exception e) {
            return ApiResponse.error("标记消息失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/send-message")
    public ApiResponse<?> sendMessage(@RequestParam Long receiverId,
                                     @RequestParam String title,
                                     @RequestParam String content,
                                     @RequestParam(required = false) String messageType,
                                     @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User receiver = userService.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("接收者不存在"));
            
            User sender = userDetails.getUser();
            
            // 权限检查：只能发送消息给本校区用户（超级管理员除外）
            if (sender.getRole() == UserRole.CAMPUS_ADMIN) {
                if (!receiver.getCampus().getId().equals(sender.getCampus().getId())) {
                    return ApiResponse.error("只能向本校区用户发送消息");
                }
            }
            
            MessageType msgType = messageType != null ? 
                MessageType.valueOf(messageType) : MessageType.ADMIN_NOTIFICATION;
            
            SystemMessage message = systemMessageService.sendMessage(
                sender, receiver, msgType, title, content, null);
            
            return ApiResponse.success("消息发送成功", message);
        } catch (Exception e) {
            return ApiResponse.error("发送消息失败: " + e.getMessage());
        }
    }
    
    // 教练员信息修改功能优化
    
    @GetMapping("/search-coach")
    public ApiResponse<?> searchCoach(@RequestParam(required = false) String phone,
                                     @RequestParam(required = false) String name,
                                     @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User admin = userDetails.getUser();
            List<Coach> coaches = new ArrayList<>();
            
            if (phone != null && !phone.trim().isEmpty()) {
                // 根据手机号查找
                Optional<User> userOpt = userRepository.findByPhoneAndActiveTrue(phone.trim());
                if (userOpt.isPresent() && userOpt.get().getRole() == UserRole.COACH) {
                    User user = userOpt.get();
                    // 权限检查
                    if (admin.getRole() == UserRole.CAMPUS_ADMIN && 
                        !user.getCampus().getId().equals(admin.getCampus().getId())) {
                        return ApiResponse.error("无权限查看其他校区教练");
                    }
                    coaches.add(coachRepository.findById(user.getId()).orElse(null));
                }
            } else if (name != null && !name.trim().isEmpty()) {
                // 根据姓名模糊查找
                List<User> users = userRepository.findByRealNameContainingAndActiveTrue(name.trim());
                for (User user : users) {
                    if (user.getRole() == UserRole.COACH) {
                        // 权限检查
                        if (admin.getRole() == UserRole.CAMPUS_ADMIN && 
                            !user.getCampus().getId().equals(admin.getCampus().getId())) {
                            continue;
                        }
                        coaches.add(coachRepository.findById(user.getId()).orElse(null));
                    }
                }
            } else {
                return ApiResponse.error("请提供手机号或姓名进行搜索");
            }
            
            coaches.removeIf(Objects::isNull);
            return ApiResponse.success(coaches);
        } catch (Exception e) {
            return ApiResponse.error("搜索教练失败: " + e.getMessage());
        }
    }
    
    @PutMapping("/update-coach-info/{coachId}")
    public ApiResponse<?> updateCoachInfo(@PathVariable Long coachId,
                                         @RequestBody CoachUpdateRequest request,
                                         @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User admin = userDetails.getUser();
            Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("教练不存在"));
            
            // 权限检查
            if (admin.getRole() == UserRole.CAMPUS_ADMIN && 
                !coach.getCampus().getId().equals(admin.getCampus().getId())) {
                return ApiResponse.error("无权限修改其他校区教练信息");
            }
            
            // 使用ValidationUtils进行数据验证
            ValidationUtils.ValidationResult validationResult = ValidationUtils.validateCoachUpdateInfo(
                request.getRealName(), request.getPhone(), request.getEmail(), request.getAge());
            
            if (!validationResult.isValid()) {
                // 发送消息提醒管理员重新输入
                systemMessageService.sendMessage(null, admin, MessageType.SYSTEM,
                    "数据验证失败", "教练信息修改失败：" + validationResult.getMessage() + 
                    "，请重新输入正确的信息", null);
                return ApiResponse.error(validationResult.getMessage());
            }
            
            // 额外验证：检查手机号是否被其他用户使用
            if (request.getPhone() != null && !request.getPhone().equals(coach.getPhone())) {
                if (userRepository.existsByPhone(request.getPhone())) {
                    String errorMsg = "手机号已被其他用户使用，请使用其他手机号";
                    systemMessageService.sendMessage(null, admin, MessageType.SYSTEM,
                        "数据验证失败", errorMsg, null);
                    return ApiResponse.error(errorMsg);
                }
            }
            
            // 额外验证：检查邮箱是否被其他用户使用
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty() && 
                !request.getEmail().equals(coach.getEmail())) {
                Optional<User> existingUser = userRepository.findByUsername(request.getEmail());
                if (existingUser.isPresent() && !existingUser.get().getId().equals(coachId)) {
                    String errorMsg = "邮箱已被其他用户使用，请使用其他邮箱";
                    systemMessageService.sendMessage(null, admin, MessageType.SYSTEM,
                        "数据验证失败", errorMsg, null);
                    return ApiResponse.error(errorMsg);
                }
            }
            
            // 更新教练信息
            if (request.getRealName() != null) coach.setRealName(request.getRealName());
            if (request.getGender() != null) coach.setGender(request.getGender());
            if (request.getAge() != null) coach.setAge(request.getAge());
            if (request.getPhone() != null) coach.setPhone(request.getPhone());
            if (request.getEmail() != null) coach.setEmail(request.getEmail());
            if (request.getLevel() != null) coach.setLevel(request.getLevel());
            if (request.getAchievements() != null) coach.setAchievements(request.getAchievements());
            
            Coach savedCoach = coachRepository.save(coach);
            
            // 发送成功通知给教练
            systemMessageService.sendAdminOperationNotification(admin, coach, 
                "个人信息修改", "您的个人信息已被管理员修改，请登录查看详情");
            
            // 发送成功通知给管理员
            systemMessageService.sendMessage(null, admin, MessageType.SYSTEM,
                "操作成功", "教练 " + coach.getRealName() + " 的信息已成功更新", coachId);
            
            // 记录系统日志
            systemLogService.log(admin, "COACH_INFO_UPDATE", 
                "修改教练信息: " + coach.getRealName());
            
            return ApiResponse.success("教练信息更新成功", savedCoach);
        } catch (Exception e) {
            return ApiResponse.error("更新教练信息失败: " + e.getMessage());
        }
    }
    
    // 管理员审核教练更换申请功能
    
    @GetMapping("/coach-change-requests")
    public ApiResponse<?> getCoachChangeRequests(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User admin = userDetails.getUser();
            List<CoachChangeRequest> requests = coachChangeService.getPendingRequests(
                admin.getId(), admin.getRole());
            return ApiResponse.success(requests);
        } catch (Exception e) {
            return ApiResponse.error("获取教练更换申请失败");
        }
    }
    
    @GetMapping("/coach-change-requests/{requestId}")
    public ApiResponse<?> getCoachChangeRequestDetails(@PathVariable Long requestId,
                                                      @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long adminId = userDetails.getUser().getId();
            CoachChangeRequest request = coachChangeService.getRequestDetails(requestId, adminId);
            return ApiResponse.success(request);
        } catch (Exception e) {
            return ApiResponse.error("获取申请详情失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/coach-change-requests/{requestId}/admin-approval")
    public ApiResponse<?> adminApproval(@PathVariable Long requestId,
                                       @RequestParam boolean approved,
                                       @RequestParam(required = false) String comment,
                                       @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long adminId = userDetails.getUser().getId();
            CoachChangeRequest request = coachChangeService.adminApproval(
                requestId, adminId, approved, comment);
            return ApiResponse.success("审核完成", request);
        } catch (Exception e) {
            return ApiResponse.error("审核失败: " + e.getMessage());
        }
    }
    
    // 线下支付录入功能
    
    @PostMapping("/offline-payment")
    public ApiResponse<?> recordOfflinePayment(@RequestParam Long studentId,
                                              @RequestParam BigDecimal amount,
                                              @RequestParam String description,
                                              @RequestParam(required = false) String receiptNumber,
                                              @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User admin = userDetails.getUser();
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学员不存在"));
            
            // 权限检查：校区管理员只能为本校区学员录入支付
            if (admin.getRole() == UserRole.CAMPUS_ADMIN && 
                !student.getCampus().getId().equals(admin.getCampus().getId())) {
                return ApiResponse.error("无权限为其他校区学员录入支付");
            }
            
            // 验证金额
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ApiResponse.error("支付金额必须大于0");
            }
            
            if (amount.compareTo(BigDecimal.valueOf(10000)) > 0) {
                return ApiResponse.error("单次录入金额不能超过10000元");
            }
            
            // 创建支付记录
            PaymentRecord paymentRecord = new PaymentRecord();
            paymentRecord.setStudent(student);
            paymentRecord.setAmount(amount);
            paymentRecord.setPaymentMethod(PaymentMethod.OFFLINE);
            paymentRecord.setTransactionNo("OFFLINE-" + System.currentTimeMillis());
            paymentRecord.setDescription("管理员录入线下支付: " + description);
            paymentRecord.setReceiptNumber(receiptNumber);
            paymentRecord.setStatus("SUCCESS");
            paymentRecord.setCreatedAt(LocalDateTime.now());
            paymentRecord.setOperator(admin.getRealName()); // 记录操作员
            
            PaymentRecord savedRecord = paymentRecordRepository.save(paymentRecord);
            
            // 更新学员账户余额
            student.setAccountBalance(student.getAccountBalance().add(amount));
            studentRepository.save(student);
            
            // 发送通知消息
            systemMessageService.sendMessage(admin, student, MessageType.PAYMENT_NOTIFICATION,
                "线下支付录入通知", 
                "管理员 " + admin.getRealName() + " 为您录入了线下支付：" + amount + " 元。" +
                (description != null ? "备注：" + description : ""), 
                savedRecord.getId());
            
            // 记录系统日志
            systemLogService.log(admin, "OFFLINE_PAYMENT_RECORD", 
                "为学员 " + student.getRealName() + " 录入线下支付 " + amount + " 元");
            
            Map<String, Object> result = new HashMap<>();
            result.put("paymentRecord", savedRecord);
            result.put("newBalance", student.getAccountBalance());
            
            return ApiResponse.success("线下支付录入成功", result);
        } catch (Exception e) {
            return ApiResponse.error("录入线下支付失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/students/{studentId}/payment-history")
    public ApiResponse<?> getStudentPaymentHistory(@PathVariable Long studentId,
                                                  @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User admin = userDetails.getUser();
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学员不存在"));
            
            // 权限检查
            if (admin.getRole() == UserRole.CAMPUS_ADMIN && 
                !student.getCampus().getId().equals(admin.getCampus().getId())) {
                return ApiResponse.error("无权限查看其他校区学员支付记录");
            }
            
            List<PaymentRecord> paymentHistory = paymentRecordRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
            return ApiResponse.success(paymentHistory);
        } catch (Exception e) {
            return ApiResponse.error("获取支付记录失败");
        }
    }
    
    @PostMapping("/verify-offline-payment/{paymentId}")
    public ApiResponse<?> verifyOfflinePayment(@PathVariable Long paymentId,
                                              @RequestParam String verificationNotes,
                                              @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User admin = userDetails.getUser();
            PaymentRecord payment = paymentRecordRepository.findById(paymentId)
                .orElseThrow(() -> new RuntimeException("支付记录不存在"));
            
            // 权限检查
            if (admin.getRole() == UserRole.CAMPUS_ADMIN && 
                !payment.getStudent().getCampus().getId().equals(admin.getCampus().getId())) {
                return ApiResponse.error("无权限验证其他校区的支付记录");
            }
            
            if (payment.getPaymentMethod() != PaymentMethod.OFFLINE) {
                return ApiResponse.error("只能验证线下支付记录");
            }
            
            // 更新验证状态
            payment.setVerificationNotes(verificationNotes);
            payment.setVerifiedBy(admin.getRealName());
            payment.setVerifiedAt(LocalDateTime.now());
            
            PaymentRecord updatedRecord = paymentRecordRepository.save(payment);
            
            // 发送通知
            systemMessageService.sendMessage(admin, payment.getStudent(), MessageType.PAYMENT_NOTIFICATION,
                "线下支付验证通知", 
                "您的线下支付记录已通过管理员验证。验证备注：" + verificationNotes, 
                paymentId);
            
            // 记录系统日志
            systemLogService.log(admin, "PAYMENT_VERIFICATION", 
                "验证学员 " + payment.getStudent().getRealName() + " 的线下支付记录");
            
            return ApiResponse.success("支付记录验证完成", updatedRecord);
        } catch (Exception e) {
            return ApiResponse.error("验证支付记录失败: " + e.getMessage());
        }
    }
} 
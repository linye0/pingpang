package com.pingpang.training.controller;

import com.pingpang.training.dto.ApiResponse;
import com.pingpang.training.dto.BookingRequest;
import com.pingpang.training.dto.ChangePasswordRequest;
import com.pingpang.training.dto.PaymentRequest;
import com.pingpang.training.dto.ProfileUpdateRequest;
import com.pingpang.training.entity.*;
import com.pingpang.training.enums.ApprovalStatus;
import com.pingpang.training.enums.BookingStatus;
import com.pingpang.training.enums.CompetitionGroup;
import com.pingpang.training.enums.PaymentMethod;
import com.pingpang.training.repository.*;
import com.pingpang.training.security.UserDetailsImpl;
import com.pingpang.training.service.SystemLogService;
import com.pingpang.training.service.UserService;
import com.pingpang.training.service.SystemMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private CoachStudentRelationRepository coachStudentRelationRepository;

    @Autowired
    private CoachRepository coachRepository;

    @Autowired
    private CourseBookingRepository courseBookingRepository;

    @Autowired
    private CourseEvaluationRepository courseEvaluationRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private MonthlyCompetitionRepository monthlyCompetitionRepository;

    @Autowired
    private CompetitionRegistrationRepository competitionRegistrationRepository;

    @Autowired
    private SystemLogService systemLogService;

    @Autowired
    private UserService userService;

    @Autowired
    private SystemMessageService systemMessageService;

    // 教练管理
    @GetMapping("/my-coaches")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getMyCoaches(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<CoachStudentRelation> relations = coachStudentRelationRepository.findByStudentId(userDetails.getUser().getId());
            return ApiResponse.success(relations);
        } catch (Exception e) {
            return ApiResponse.error("获取教练列表失败");
        }
    }

    @GetMapping("/available-coaches")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getAvailableCoaches(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long campusId = userDetails.getUser().getCampus().getId();
            List<Coach> coaches = coachRepository.findApprovedByCampusId(campusId);
            return ApiResponse.success(coaches);
        } catch (Exception e) {
            return ApiResponse.error("获取可选教练失败");
        }
    }

    @PostMapping("/apply-coach/{coachId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> applyForCoach(@PathVariable Long coachId,
                                       @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // 检查是否已经申请过
            if (coachStudentRelationRepository.findByStudentIdAndCoachId(
                userDetails.getUser().getId(), coachId).isPresent()) {
                return ApiResponse.error("已经申请过该教练");
            }

            Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("教练不存在"));
            
            Student student = studentRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("学员信息不存在"));

            CoachStudentRelation relation = new CoachStudentRelation();
            relation.setCoach(coach);
            relation.setStudent(student);
            relation.setStatus(ApprovalStatus.PENDING);

            coachStudentRelationRepository.save(relation);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "COACH_APPLY", 
                "申请教练: " + coach.getRealName());

            return ApiResponse.success("教练申请已提交，等待审核", relation);
        } catch (Exception e) {
            return ApiResponse.error("申请教练失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/cancel-application/{relationId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> cancelCoachApplication(@PathVariable Long relationId,
                                                @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            CoachStudentRelation relation = coachStudentRelationRepository.findById(relationId)
                .orElseThrow(() -> new RuntimeException("申请不存在"));

            if (!relation.getStudent().getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("无权限操作此申请");
            }

            if (relation.getStatus() != ApprovalStatus.PENDING) {
                return ApiResponse.error("只能取消待审核的申请");
            }

            coachStudentRelationRepository.delete(relation);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "COACH_APPLY_CANCEL", 
                "取消教练申请: " + relation.getCoach().getRealName());

            return ApiResponse.success("申请已取消", null);
        } catch (Exception e) {
            return ApiResponse.error("取消申请失败: " + e.getMessage());
        }
    }

    // 课程预约管理
    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getMyBookings(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<CourseBooking> bookings = courseBookingRepository.findByStudentId(userDetails.getUser().getId());
            return ApiResponse.success(bookings);
        } catch (Exception e) {
            return ApiResponse.error("获取预约列表失败");
        }
    }

    @PostMapping("/book-course")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> bookCourse(@Valid @RequestBody BookingRequest request,
                                    @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // 验证教练关系
            CoachStudentRelation relation = coachStudentRelationRepository.findByStudentIdAndCoachId(
                userDetails.getUser().getId(), request.getCoachId())
                .orElseThrow(() -> new RuntimeException("没有与该教练的师生关系"));

            if (relation.getStatus() != ApprovalStatus.APPROVED) {
                return ApiResponse.error("师生关系未通过审核");
            }

            // 检查时间冲突
            List<CourseBooking> conflicts = courseBookingRepository.findByCoachIdAndTimeRange(
                request.getCoachId(), request.getStartTime(), request.getEndTime());
            if (!conflicts.isEmpty()) {
                return ApiResponse.error("该时间段已被预约");
            }

            // 创建预约
            CourseBooking booking = new CourseBooking();
            booking.setStudent(studentRepository.findById(userDetails.getUser().getId()).get());
            booking.setCoach(coachRepository.findById(request.getCoachId()).get());
            booking.setStartTime(request.getStartTime());
            booking.setEndTime(request.getEndTime());
            booking.setTableNumber(request.getTableNumber());
            String remarks = request.getRemarks() != null ? request.getRemarks() : "";
            booking.setRemarks(remarks);
            
            booking.setStatus(BookingStatus.PENDING);

            courseBookingRepository.save(booking);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "COURSE_BOOK", 
                "预约课程: " + booking.getCoach().getRealName());

            return ApiResponse.success("课程预约成功", booking);
        } catch (Exception e) {
            return ApiResponse.error("预约课程失败: " + e.getMessage());
        }
    }

    @PostMapping("/request-cancel-booking/{bookingId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> requestCancelBooking(@PathVariable Long bookingId,
                                              @RequestParam(required = false) String reason,
                                              @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            CourseBooking booking = courseBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("预约不存在"));

            if (!booking.getStudent().getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("无权限操作此预约");
            }

            if (booking.getStatus() == BookingStatus.COMPLETED) {
                return ApiResponse.error("已完成的课程不能取消");
            }

            LocalDateTime now = LocalDateTime.now();
            if (booking.getStartTime().isBefore(now.plusHours(24))) {
                return ApiResponse.error("课程开始前24小时内不能取消预约");
            }

            Student student = studentRepository.findById(userDetails.getUser().getId()).get();
            if (student.getCancellationCount() >= 3) {
                return ApiResponse.error("本月取消次数已达上限(3次)");
            }

            // 更新取消计数
            student.setCancellationCount(student.getCancellationCount() + 1);
            studentRepository.save(student);

            booking.setStatus(BookingStatus.PENDING_CANCELLATION);
            if (reason != null && !reason.trim().isEmpty()) {
                String existingRemarks = booking.getRemarks() != null ? booking.getRemarks() : "";
                booking.setRemarks(existingRemarks + "\n学员申请取消原因: " + reason);
            }
            courseBookingRepository.save(booking);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "BOOKING_CANCEL_REQUEST", 
                "申请取消课程预约: " + booking.getCoach().getRealName());

            return ApiResponse.success("取消申请已提交，等待教练确认", booking);
        } catch (Exception e) {
            return ApiResponse.error("申请取消预约失败: " + e.getMessage());
        }
    }

    // 课程评价
    @GetMapping("/my-evaluations")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getMyEvaluations(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<CourseEvaluation> evaluations = courseEvaluationRepository.findByStudentId(
                userDetails.getUser().getId());
            return ApiResponse.success(evaluations);
        } catch (Exception e) {
            return ApiResponse.error("获取评价记录失败");
        }
    }

    @GetMapping("/pending-evaluations")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getPendingEvaluations(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<CourseEvaluation> evaluations = courseEvaluationRepository.findPendingStudentEvaluations(
                userDetails.getUser().getId());
            return ApiResponse.success(evaluations);
        } catch (Exception e) {
            return ApiResponse.error("获取待评价课程失败");
        }
    }

    @PostMapping("/evaluations/{evaluationId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<?> submitStudentEvaluation(@PathVariable Long evaluationId,
                                                 @RequestParam String evaluation,
                                                 @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println(" UserController提交学员评价开始 ");
            System.out.println("评价ID: " + evaluationId);
            
            CourseEvaluation courseEvaluation = courseEvaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("评价记录不存在"));

            if (!courseEvaluation.getBooking().getStudent().getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("无权限操作此评价");
            }

            courseEvaluation.setStudentEvaluation(evaluation);
            courseEvaluation = courseEvaluationRepository.save(courseEvaluation);
            System.out.println("评价记录保存成功");

            try {
                systemLogService.log(userDetails.getUser(), "EVALUATION_SUBMIT", 
                    "提交课后评价: " + courseEvaluation.getBooking().getCoach().getRealName());
            } catch (Exception logException) {
                // 记录日志失败不应影响主要操作
                System.err.println("System log failed but main operation succeeded: " + logException.getMessage());
            }

            System.out.println(" UserController提交学员评价成功 ");
            return ApiResponse.success("评价已提交", courseEvaluation);
        } catch (Exception e) {
            System.err.println(" UserController提交学员评价失败 ");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("提交评价失败: " + e.getMessage());
        }
    }

    // 缴费管理
    @GetMapping("/balance")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getBalance(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Student student = studentRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("学员信息不存在"));
            return ApiResponse.success(student.getAccountBalance());
        } catch (Exception e) {
            return ApiResponse.error("获取余额失败");
        }
    }

    @GetMapping("/payment-records")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getPaymentRecords(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<PaymentRecord> records = paymentRecordRepository.findByStudentId(userDetails.getUser().getId());
            return ApiResponse.success(records);
        } catch (Exception e) {
            return ApiResponse.error("获取缴费记录失败");
        }
    }

    @PostMapping("/recharge")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> recharge(@Valid @RequestBody PaymentRequest request,
                                  @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Student student = studentRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("学员信息不存在"));

            // 创建缴费记录
            PaymentRecord record = new PaymentRecord();
            record.setStudent(student);
            record.setAmount(request.getAmount());
            record.setPaymentMethod(request.getPaymentMethod());
            record.setTransactionNo(generateTransactionNo());
            record.setDescription("账户充值");

            paymentRecordRepository.save(record);

            // 更新学员余额
            student.setAccountBalance(student.getAccountBalance().add(request.getAmount()));
            studentRepository.save(student);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "ACCOUNT_RECHARGE", 
                "账户充值: ¥" + request.getAmount());

            return ApiResponse.success("充值成功", record);
        } catch (Exception e) {
            return ApiResponse.error("充值失败: " + e.getMessage());
        }
    }

    // 参赛管理
    @GetMapping("/competitions")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getAvailableCompetitions(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long campusId = userDetails.getUser().getCampus().getId();
            LocalDateTime now = LocalDateTime.now();
            List<MonthlyCompetition> competitions = monthlyCompetitionRepository.findUpcomingCompetitionsByCampus(
                campusId, now);
            return ApiResponse.success(competitions);
        } catch (Exception e) {
            return ApiResponse.error("获取比赛列表失败");
        }
    }

    @GetMapping("/my-competitions")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getMyCompetitions(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<CompetitionRegistration> registrations = competitionRegistrationRepository.findByStudentId(
                userDetails.getUser().getId());
            return ApiResponse.success(registrations);
        } catch (Exception e) {
            return ApiResponse.error("获取参赛记录失败");
        }
    }

    @PostMapping("/register-competition/{competitionId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> registerForCompetition(@PathVariable Long competitionId,
                                                @RequestParam CompetitionGroup group,
                                                @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // 检查是否已经报名
            if (competitionRegistrationRepository.existsByCompetitionIdAndStudentId(
                competitionId, userDetails.getUser().getId())) {
                return ApiResponse.error("已经报名该比赛");
            }

            MonthlyCompetition competition = monthlyCompetitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("比赛不存在"));

            if (!competition.getRegistrationOpen()) {
                return ApiResponse.error("比赛报名已关闭");
            }

            Student student = studentRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("学员信息不存在"));

            // 检查余额
            if (student.getAccountBalance().compareTo(competition.getRegistrationFee()) < 0) {
                return ApiResponse.error("账户余额不足，请先充值");
            }

            // 创建报名记录
            CompetitionRegistration registration = new CompetitionRegistration();
            registration.setCompetition(competition);
            registration.setStudent(student);
            registration.setCompetitionGroup(group);

            competitionRegistrationRepository.save(registration);

            // 扣除报名费
            student.setAccountBalance(student.getAccountBalance().subtract(competition.getRegistrationFee()));
            studentRepository.save(student);

            // 创建扣费记录
            PaymentRecord record = new PaymentRecord();
            record.setStudent(student);
            record.setAmount(competition.getRegistrationFee().negate());
            record.setPaymentMethod(PaymentMethod.BALANCE);
            record.setTransactionNo(generateTransactionNo());
            record.setDescription("比赛报名费: " + competition.getName());

            paymentRecordRepository.save(record);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "COMPETITION_REGISTER", 
                "报名比赛: " + competition.getName());

            return ApiResponse.success("比赛报名成功", registration);
        } catch (Exception e) {
            return ApiResponse.error("报名比赛失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/cancel-competition/{registrationId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> cancelCompetitionRegistration(@PathVariable Long registrationId,
                                                       @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            CompetitionRegistration registration = competitionRegistrationRepository.findById(registrationId)
                .orElseThrow(() -> new RuntimeException("报名记录不存在"));

            if (!registration.getStudent().getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("无权限操作此报名");
            }

            // 检查是否可以取消（比赛前一天才能取消）
            LocalDateTime oneDayBefore = registration.getCompetition().getCompetitionDate().minusDays(1);
            if (LocalDateTime.now().isAfter(oneDayBefore)) {
                return ApiResponse.error("比赛前一天不能取消报名");
            }

            // 退还报名费
            Student student = registration.getStudent();
            student.setAccountBalance(student.getAccountBalance().add(registration.getCompetition().getRegistrationFee()));
            studentRepository.save(student);

            // 创建退费记录
            PaymentRecord record = new PaymentRecord();
            record.setStudent(student);
            record.setAmount(registration.getCompetition().getRegistrationFee());
            record.setPaymentMethod(PaymentMethod.BALANCE);
            record.setTransactionNo(generateTransactionNo());
            record.setDescription("取消比赛报名退费: " + registration.getCompetition().getName());

            paymentRecordRepository.save(record);

            // 删除报名记录
            competitionRegistrationRepository.delete(registration);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "COMPETITION_CANCEL", 
                "取消比赛报名: " + registration.getCompetition().getName());

            return ApiResponse.success("比赛报名已取消，报名费已退还", null);
        } catch (Exception e) {
            return ApiResponse.error("取消报名失败: " + e.getMessage());
        }
    }

    // 个人信息
    @GetMapping("/profile")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getProfile(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Student student = studentRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("学员信息不存在"));
            return ApiResponse.success(student);
        } catch (Exception e) {
            return ApiResponse.error("获取个人信息失败");
        }
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> updateProfile(@Valid @RequestBody ProfileUpdateRequest request,
                                       @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Student existingStudent = studentRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("学员信息不存在"));

            // 更新基本用户信息
            if (request.getRealName() != null && !request.getRealName().trim().isEmpty()) {
                existingStudent.setRealName(request.getRealName());
            }
            if (request.getGender() != null) {
                existingStudent.setGender(request.getGender());
            }
            if (request.getAge() != null) {
                existingStudent.setAge(request.getAge());
            }
            if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
                existingStudent.setPhone(request.getPhone());
            }
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                existingStudent.setEmail(request.getEmail());
            }
            if (request.getAvatar() != null) {
                existingStudent.setAvatar(request.getAvatar());
            }

            // 更新学员特有信息
            if (request.getEmergencyContact() != null) {
                existingStudent.setEmergencyContact(request.getEmergencyContact());
            }
            if (request.getEmergencyPhone() != null) {
                existingStudent.setEmergencyPhone(request.getEmergencyPhone());
            }

            Student savedStudent = studentRepository.save(existingStudent);

            // 记录操作日志 - 使用独立事务避免影响主要操作
            try {
                systemLogService.log(userDetails.getUser(), "PROFILE_UPDATE", "更新学员信息");
            } catch (Exception logException) {
                System.err.println("System log failed but main operation succeeded: " + logException.getMessage());
            }

            return ApiResponse.success("个人信息更新成功", savedStudent);
        } catch (Exception e) {
            return ApiResponse.error("更新个人信息失败: " + e.getMessage());
        }
    }

    // 修改密码
    @PostMapping("/change-password")
    @PreAuthorize("hasRole('STUDENT') or hasRole('COACH') or hasRole('CAMPUS_ADMIN') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            userService.changePassword(userDetails.getUser().getId(), 
                                     request.getOldPassword(), 
                                     request.getNewPassword());
            return ApiResponse.success("密码修改成功");
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getStudentStatistics(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long studentId = userDetails.getUser().getId();
            
            long coachesCount = coachStudentRelationRepository.countByStudentIdAndStatus(studentId, ApprovalStatus.APPROVED);
            long pendingCoachesCount = coachStudentRelationRepository.countByStudentIdAndStatus(studentId, ApprovalStatus.PENDING);
            long bookingsCount = courseBookingRepository.countByStudentId(studentId);
            long completedBookingsCount = courseBookingRepository.countByStudentIdAndStatus(studentId, BookingStatus.COMPLETED);
            long competitionsCount = competitionRegistrationRepository.countByStudentId(studentId);
            long evaluationsCount = courseEvaluationRepository.findPendingStudentEvaluations(studentId).size();

            Student student = studentRepository.findById(studentId).get();


            Map<String, Object> statisticsData = new HashMap<>();
            statisticsData.put("totalCoaches", coachesCount);
            statisticsData.put("pendingCoaches", pendingCoachesCount);
            statisticsData.put("totalBookings", bookingsCount);
            statisticsData.put("completedBookings", completedBookingsCount);
            statisticsData.put("totalCompetitions", competitionsCount);
            statisticsData.put("pendingEvaluations", evaluationsCount);
            statisticsData.put("accountBalance", student.getAccountBalance());
            
            return ApiResponse.success(statisticsData);
        } catch (Exception e) {
            return ApiResponse.error("获取统计信息失败");
        }
    }
    

    @GetMapping("/messages")
    public ApiResponse<?> getUserMessages(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long userId = userDetails.getUser().getId();
            List<SystemMessage> messages = systemMessageService.getUserMessages(userId);
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
    
    @GetMapping("/messages/unread-count")
    public ApiResponse<?> getUnreadMessageCount(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long userId = userDetails.getUser().getId();
            long unreadCount = systemMessageService.getUnreadCount(userId);
            Map<String, Long> result = new HashMap<>();
            result.put("unreadCount", unreadCount);
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("获取未读消息数量失败");
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
    
    @PostMapping("/messages/mark-all-read")
    public ApiResponse<?> markAllMessagesAsRead(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long userId = userDetails.getUser().getId();
            List<SystemMessage> unreadMessages = systemMessageService.getUnreadMessages(userId);
            List<Long> messageIds = unreadMessages.stream()
                .map(SystemMessage::getId)
                .collect(java.util.stream.Collectors.toList());
            
            systemMessageService.markMultipleAsRead(messageIds, userId);
            return ApiResponse.success("所有消息已标记为已读", null);
        } catch (Exception e) {
            return ApiResponse.error("批量标记失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/messages/{messageId}")
    public ApiResponse<?> deleteMessage(@PathVariable Long messageId,
                                       @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long userId = userDetails.getUser().getId();
            systemMessageService.deleteMessage(messageId, userId);
            return ApiResponse.success("消息已删除", null);
        } catch (Exception e) {
            return ApiResponse.error("删除消息失败: " + e.getMessage());
        }
    }


    private String generateTransactionNo() {
        return "TXN" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
} 
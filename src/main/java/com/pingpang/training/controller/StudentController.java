package com.pingpang.training.controller;

import com.pingpang.training.dto.ApiResponse;
import com.pingpang.training.dto.BookingRequest;
import com.pingpang.training.dto.PaymentRequest;
import com.pingpang.training.entity.*;
import com.pingpang.training.enums.ApprovalStatus;
import com.pingpang.training.enums.BookingStatus;
import com.pingpang.training.enums.CompetitionGroup;
import com.pingpang.training.repository.*;
import com.pingpang.training.security.UserDetailsImpl;
import com.pingpang.training.service.BookingService;
import com.pingpang.training.service.PaymentService;
import com.pingpang.training.service.SystemLogService;
import com.pingpang.training.service.CompetitionService;
import com.pingpang.training.service.CoachChangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.util.stream.Collectors;
import java.util.ArrayList;
import com.pingpang.training.enums.UserRole;
import com.pingpang.training.entity.CoachChangeRequest;
import java.util.Optional;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
public class StudentController {

    @Autowired
    private CoachRepository coachRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CoachStudentRelationRepository coachStudentRelationRepository;

    @Autowired
    private CourseBookingRepository courseBookingRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private MonthlyCompetitionRepository monthlyCompetitionRepository;

    @Autowired
    private CompetitionRegistrationRepository competitionRegistrationRepository;

    @Autowired
    private CourseEvaluationRepository courseEvaluationRepository;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SystemLogService systemLogService;

    @Autowired
    private CompetitionService competitionService;

    @Autowired
    private CoachChangeService coachChangeService;

    @Autowired
    private CoachWorkingTimeRepository coachWorkingTimeRepository;

    // 获取教练列表
    @GetMapping("/coaches")
    public ApiResponse<?> getAllCoaches(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long campusId = userDetails.getUser().getCampus().getId();
            System.out.println(" 获取教练列表 ");
            System.out.println("校区ID: " + campusId);
            List<Coach> coaches = coachRepository.findApprovedByCampusId(campusId);
            System.out.println("查询到的教练数量: " + coaches.size());
            for (Coach coach : coaches) {
                System.out.println("教练: " + coach.getRealName() + ", 状态: " + coach.getApprovalStatus() + ", 校区: " + coach.getCampus().getId());
            }
            return ApiResponse.success(coaches);
        } catch (Exception e) {
            System.err.println("获取教练列表失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("获取教练列表失败: " + e.getMessage());
        }
    }

    // 搜索教练
    @GetMapping("/coaches/search")
    public ApiResponse<?> searchCoaches(@RequestParam(required = false) String name,
                                       @RequestParam(required = false) String gender,
                                       @RequestParam(required = false) Integer age,
                                       @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long campusId = userDetails.getUser().getCampus().getId();
            List<Coach> coaches = coachRepository.searchCoaches(campusId, name, gender, age);
            return ApiResponse.success(coaches);
        } catch (Exception e) {
            return ApiResponse.error("搜索教练失败: " + e.getMessage());
        }
    }

    // 选择教练
    @PostMapping("/select-coach/{coachId}")
    public ApiResponse<?> selectCoach(@PathVariable Long coachId,
                                     @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // 检查是否已经申请过
            if (coachStudentRelationRepository.findByStudentIdAndCoachId(
                userDetails.getUser().getId(), coachId).isPresent()) {
                return ApiResponse.error("已经申请过该教练");
            }

            // 检查学员是否已达到上限（2位教练）
            long currentCoachCount = coachStudentRelationRepository.countByStudentIdAndStatus(
                userDetails.getUser().getId(), ApprovalStatus.APPROVED);
            if (currentCoachCount >= 2) {
                return ApiResponse.error("最多只能选择2位教练");
            }

            Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("教练不存在"));
            
            Student student = studentRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("学员信息不存在"));

            // 检查教练学员数量限制（20名学员）
            long coachStudentCount = coachStudentRelationRepository.countByCoachIdAndStatus(
                coachId, ApprovalStatus.APPROVED);
            if (coachStudentCount >= 20) {
                return ApiResponse.error("该教练学员已满（最多20名学员）");
            }

            // 创建师生关系申请
            CoachStudentRelation relation = new CoachStudentRelation();
            relation.setCoach(coach);
            relation.setStudent(student);
            relation.setStatus(ApprovalStatus.PENDING);

            coachStudentRelationRepository.save(relation);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "COACH_APPLY", 
                "申请选择教练: " + coach.getRealName());

            return ApiResponse.success("教练申请已提交，等待教练确认", relation);
        } catch (Exception e) {
            return ApiResponse.error("申请失败: " + e.getMessage());
        }
    }

    // 获取我的教练
    @GetMapping("/my-coaches")
    public ApiResponse<?> getMyCoaches(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<CoachStudentRelation> relations = coachStudentRelationRepository
                .findByStudentIdAndStatus(userDetails.getUser().getId(), ApprovalStatus.APPROVED);
            return ApiResponse.success(relations);
        } catch (Exception e) {
            return ApiResponse.error("获取我的教练失败: " + e.getMessage());
        }
    }

    @GetMapping("/pending-coach-applications")
    public ApiResponse<?> getPendingCoachApplications(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<CoachStudentRelation> relations = coachStudentRelationRepository.findByStudentIdAndStatus(
                userDetails.getUser().getId(), ApprovalStatus.PENDING);
            return ApiResponse.success(relations);
        } catch (Exception e) {
            return ApiResponse.error("获取待审核申请失败");
        }
    }

    // 课程预约
    @GetMapping("/coaches/{coachId}/schedule")
    public ApiResponse<?> getCoachSchedule(@PathVariable Long coachId,
                                          @RequestParam(required = false) String startTime,
                                          @RequestParam(required = false) String endTime,
                                          @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println(" 获取教练可预约时间段 ");
            System.out.println("教练ID: " + coachId);
            System.out.println("开始时间: " + startTime);
            System.out.println("结束时间: " + endTime);
            
            // 解析时间参数
            LocalDateTime start = startTime != null ? LocalDateTime.parse(startTime.replace("Z", "")) : LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime end = endTime != null ? LocalDateTime.parse(endTime.replace("Z", "")) : start.plusDays(1);
            
            // 1. 获取教练的工作时间设置
            LocalDate queryDate = start.toLocalDate();
            int dayOfWeek = queryDate.getDayOfWeek().getValue();
            
            System.out.println("查询日期: " + queryDate + ", 星期: " + dayOfWeek);
            
            List<CoachWorkingTime> workingTimes = coachWorkingTimeRepository
                .findCoachWorkingTimeForDate(coachId, dayOfWeek, queryDate);
            
            System.out.println("找到工作时间安排: " + workingTimes.size() + " 个");
            
            // 2. 获取教练的已有预约
            List<CourseBooking> existingBookings = courseBookingRepository
                .findByCoachIdAndTimeRange(coachId, start, end);
            
            System.out.println("找到已有预约: " + existingBookings.size() + " 个");
            
            // 3. 计算真正的可预约时间段
            List<Map<String, Object>> availableSlots = new ArrayList<>();
            
            for (CoachWorkingTime workingTime : workingTimes) {
                if (!workingTime.getIsAvailable()) {
                    System.out.println("跳过不可用的工作时间: " + workingTime.getStartTime() + "-" + workingTime.getEndTime());
                    continue;
                }
                
                // 检查该时间段是否被预约占用
                LocalDateTime slotStart = queryDate.atTime(workingTime.getStartTime());
                LocalDateTime slotEnd = queryDate.atTime(workingTime.getEndTime());
                
                boolean isOccupied = existingBookings.stream().anyMatch(booking -> {
                    LocalDateTime bookingStart = booking.getStartTime();
                    LocalDateTime bookingEnd = booking.getEndTime();
                    

                    boolean timeOverlap = !(slotEnd.isBefore(bookingStart) || slotStart.isAfter(bookingEnd));
                    boolean validStatus = booking.getStatus() == BookingStatus.PENDING || 
                                        booking.getStatus() == BookingStatus.CONFIRMED;
                    
                    return timeOverlap && validStatus;
                });
                
                if (!isOccupied) {
                    Map<String, Object> slot = new HashMap<>();
                    slot.put("startTime", slotStart);
                    slot.put("endTime", slotEnd);
                    slot.put("available", true);
                    slot.put("workingTimeId", workingTime.getId());
                    slot.put("remarks", workingTime.getRemarks());
                    
                    availableSlots.add(slot);
                    System.out.println("可预约时间段: " + slotStart + " - " + slotEnd);
                } else {
                    System.out.println("时间段被占用: " + slotStart + " - " + slotEnd);
                }
            }
            
            // 4. 构建返回数据
            Map<String, Object> result = new HashMap<>();
            result.put("availableSlots", availableSlots);
            result.put("existingBookings", existingBookings);
            result.put("workingTimes", workingTimes);
            result.put("queryDate", queryDate);
            result.put("dayOfWeek", dayOfWeek);
            
            System.out.println("最终可预约时间段数量: " + availableSlots.size());
            
            return ApiResponse.success(result);
            
        } catch (Exception e) {
            System.err.println("获取教练可预约时间段失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("获取教练课表失败: " + e.getMessage());
        }
    }

    @GetMapping("/available-tables")
    @PreAuthorize("hasRole('STUDENT') or hasRole('COACH') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getAvailableTables(@RequestParam String startTime,
                                            @RequestParam String endTime) {
        try {
            System.out.println("=== 获取可用球台 API 被调用 ===");
            System.out.println("startTime: " + startTime);
            System.out.println("endTime: " + endTime);
            

            LocalDateTime start;
            LocalDateTime end;
            
            try {
                start = LocalDateTime.parse(startTime.replace("Z", ""));
                end = LocalDateTime.parse(endTime.replace("Z", ""));
            } catch (Exception e1) {
                try {
                    start = LocalDateTime.parse(startTime);
                    end = LocalDateTime.parse(endTime);
                } catch (Exception e2) {
                    start = LocalDateTime.parse(startTime.substring(0, 19));
                    end = LocalDateTime.parse(endTime.substring(0, 19));
                }
            }
            
            System.out.println("解析后的时间 - start: " + start + ", end: " + end);
            
            List<String> availableTables = bookingService.getAvailableTables(start, end);
            System.out.println("可用球台: " + availableTables);
            
            return ApiResponse.success(availableTables);
        } catch (Exception e) {
            System.err.println("获取球台失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("获取可用球台失败: " + e.getMessage());
        }
    }

    @PostMapping("/book-course")
    public ApiResponse<?> bookCourse(@Valid @RequestBody BookingRequest request,
                                    @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // 记录预约尝试
            System.out.println("开始处理预约请求 - 用户: " + userDetails.getUser().getUsername() + 
                             ", 教练ID: " + request.getCoachId() + 
                             ", 时间: " + request.getStartTime() + " - " + request.getEndTime() +
                             ", 球台: " + request.getTableNumber());
            
            Student student = studentRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("学员信息不存在"));
            
            CourseBooking booking = bookingService.createBooking(request, student);
            
            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "COURSE_BOOK", 
                "预约课程: " + booking.getCoach().getRealName() + 
                ", 球台: " + booking.getTableNumber() + 
                ", 时间: " + booking.getStartTime());
            
            System.out.println("预约成功 - 预约ID: " + booking.getId() + 
                             ", 球台: " + booking.getTableNumber() + 
                             ", 费用: " + booking.getCost());
            
            return ApiResponse.success("课程预约成功", booking);
        } catch (IllegalArgumentException e) {
            // 业务逻辑错误（如球台不可用、余额不足等）
            System.err.println("预约业务逻辑错误: " + e.getMessage());
            return ApiResponse.error("预约失败: " + e.getMessage());
        } catch (IllegalStateException e) {
            // 并发冲突等状态错误
            System.err.println("预约状态错误: " + e.getMessage());
            return ApiResponse.error("预约失败: " + e.getMessage());
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // 数据库约束冲突
            System.err.println("预约数据约束冲突: " + e.getMessage());
            String errorMsg = "预约失败";
            if (e.getMessage().contains("球台") && e.getMessage().contains("已被预约")) {
                errorMsg = "该球台在此时间段已被预约，请选择其他时间或球台";
            } else if (e.getMessage().contains("教练") && e.getMessage().contains("课程安排")) {
                errorMsg = "该教练在此时间段已有其他课程安排";
            } else {
                errorMsg = "预约冲突，请重新选择时间和球台";
            }
            return ApiResponse.error(errorMsg);
        } catch (Exception e) {
            // 其他未预期的错误
            System.err.println("预约系统错误: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("系统繁忙，请稍后重试");
        }
    }

    @GetMapping("/my-bookings")
    public ApiResponse<?> getMyBookings(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<CourseBooking> bookings = courseBookingRepository.findByStudentId(
                userDetails.getUser().getId());
            return ApiResponse.success(bookings);
        } catch (Exception e) {
            return ApiResponse.error("获取我的预约失败");
        }
    }

    // 申请取消预约
    @PostMapping("/cancel-booking/{bookingId}")
    public ApiResponse<?> requestCancelBooking(@PathVariable Long bookingId,
                                              @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            CourseBooking booking = courseBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("预约不存在"));

            if (!booking.getStudent().getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("无权限操作此预约");
            }

            // 检查是否符合24小时规则
            LocalDateTime now = LocalDateTime.now();
            if (booking.getStartTime().isBefore(now.plusHours(24))) {
                return ApiResponse.error("距离上课时间不足24小时，无法取消");
            }

            // 检查本月取消次数
            Student student = studentRepository.findById(userDetails.getUser().getId()).get();
            if (student.getCancellationCount() >= 3) {
                return ApiResponse.error("本月取消次数已达上限(3次)");
            }

            // 设置为待取消状态，等待教练确认
            booking.setStatus(BookingStatus.PENDING_CANCELLATION);
            courseBookingRepository.save(booking);

            // 增加学员取消次数
            student.setCancellationCount(student.getCancellationCount() + 1);
            studentRepository.save(student);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "BOOKING_CANCEL_REQUEST", 
                "申请取消课程预约: " + booking.getCoach().getRealName());

            return ApiResponse.success("取消申请已提交，等待教练确认");
        } catch (Exception e) {
            return ApiResponse.error("操作失败: " + e.getMessage());
        }
    }

    // 确认教练的取消申请
    @PostMapping("/confirm-coach-cancel/{bookingId}")
    public ApiResponse<?> confirmCoachCancel(@PathVariable Long bookingId,
                                            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            CourseBooking booking = courseBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("预约不存在"));

            if (!booking.getStudent().getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("无权限操作此预约");
            }

            if (booking.getStatus() != BookingStatus.PENDING_CANCELLATION) {
                return ApiResponse.error("该预约不是待取消确认状态");
            }

            // 确认取消，退款给学员
            Student student = booking.getStudent();
            if (booking.getCost() != null) {
                student.setAccountBalance(student.getAccountBalance().add(booking.getCost()));
                studentRepository.save(student);
            }

            booking.setStatus(BookingStatus.CANCELLED);
            courseBookingRepository.save(booking);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "BOOKING_CANCEL_CONFIRM", 
                "确认取消课程预约: " + booking.getCoach().getRealName());

            return ApiResponse.success("已确认取消预约，费用已退还", booking);
        } catch (Exception e) {
            return ApiResponse.error("操作失败: " + e.getMessage());
        }
    }

    // 拒绝教练的取消申请
    @PostMapping("/reject-coach-cancel/{bookingId}")
    public ApiResponse<?> rejectCoachCancel(@PathVariable Long bookingId,
                                           @RequestParam(required = false) String reason,
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            CourseBooking booking = courseBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("预约不存在"));

            if (!booking.getStudent().getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("无权限操作此预约");
            }

            if (booking.getStatus() != BookingStatus.PENDING_CANCELLATION) {
                return ApiResponse.error("该预约不是待取消确认状态");
            }

            // 拒绝取消，恢复到原状态
            booking.setStatus(BookingStatus.CONFIRMED);
            if (reason != null && !reason.trim().isEmpty()) {
                String existingRemarks = booking.getRemarks() != null ? booking.getRemarks() : "";
                booking.setRemarks(existingRemarks + "\n学员拒绝取消原因: " + reason);
            }
            courseBookingRepository.save(booking);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "BOOKING_CANCEL_REJECT", 
                "拒绝取消课程预约: " + booking.getCoach().getRealName());

            return ApiResponse.success("已拒绝取消申请", booking);
        } catch (Exception e) {
            return ApiResponse.error("操作失败: " + e.getMessage());
        }
    }

    // 支付功能
    @GetMapping("/balance")
    public ApiResponse<?> getBalance(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Student student = studentRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("学员信息不存在"));
            return ApiResponse.success(student.getAccountBalance());
        } catch (Exception e) {
            return ApiResponse.error("获取账户余额失败");
        }
    }

    @PostMapping("/recharge")
    public ApiResponse<?> recharge(@Valid @RequestBody PaymentRequest request,
                                  @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Student student = studentRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("学员信息不存在"));
            
            PaymentRecord payment = paymentService.processRecharge(request, student);
            
            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "ACCOUNT_RECHARGE", 
                "账户充值: " + request.getAmount());
            
            return ApiResponse.success("充值成功", payment);
        } catch (Exception e) {
            return ApiResponse.error("充值失败: " + e.getMessage());
        }
    }

    @GetMapping("/payment-records")
    public ApiResponse<?> getPaymentRecords(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<PaymentRecord> records = paymentRecordRepository.findByStudentId(
                userDetails.getUser().getId());
            return ApiResponse.success(records);
        } catch (Exception e) {
            return ApiResponse.error("获取支付记录失败");
        }
    }

    // 比赛报名
    @GetMapping("/competitions")
    public ApiResponse<?> getAvailableCompetitions(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println("=== 获取可报名比赛开始 ===");
            
            // 获取用户信息
            Long userId = userDetails.getUser().getId();
            Long campusId = userDetails.getUser().getCampus().getId();
            System.out.println("用户ID: " + userId);
            System.out.println("校区ID: " + campusId);
            System.out.println("校区名称: " + userDetails.getUser().getCampus().getName());
            
            // 首先查询所有比赛
            List<MonthlyCompetition> allCompetitions = monthlyCompetitionRepository.findAll();
            System.out.println("数据库中总比赛数量: " + allCompetitions.size());
            
            for (MonthlyCompetition comp : allCompetitions) {
                System.out.println("比赛: " + comp.getName() + 
                    ", 校区ID: " + comp.getCampus().getId() + 
                    ", 校区名: " + comp.getCampus().getName() + 
                    ", 报名开放: " + comp.getRegistrationOpen() +
                    ", 比赛日期: " + comp.getCompetitionDate());
            }
            
            // 按校区查询开放注册的比赛
            List<MonthlyCompetition> competitions = monthlyCompetitionRepository
                .findByCampusIdAndRegistrationOpenTrue(campusId);
            System.out.println("当前校区开放报名的比赛数量: " + competitions.size());
            
            if (competitions.isEmpty()) {
                System.out.println(" 当前校区没有开放报名的比赛 ");
                
                // 检查是否有该校区的比赛
                List<MonthlyCompetition> campusCompetitions = monthlyCompetitionRepository.findByCampusId(campusId);
                System.out.println("当前校区所有比赛数量（包括关闭报名的）: " + campusCompetitions.size());
                
                for (MonthlyCompetition comp : campusCompetitions) {
                    System.out.println("校区比赛: " + comp.getName() + 
                        ", 报名开放: " + comp.getRegistrationOpen() +
                        ", 报名开始: " + comp.getRegistrationStartDate() +
                        ", 报名结束: " + comp.getRegistrationEndDate());
                }
            }
            
            System.out.println("返回比赛数据 ");
            return ApiResponse.success(competitions);
        } catch (Exception e) {
            System.err.println("获取可报名比赛失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("获取可报名比赛失败: " + e.getMessage());
        }
    }

    @PostMapping("/competitions/{competitionId}/register")
    @Transactional(propagation = Propagation.REQUIRED)
    public ApiResponse<?> registerCompetition(@PathVariable Long competitionId,
                                             @RequestParam CompetitionGroup group,
                                             @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long studentId = userDetails.getUser().getId();
            
            System.out.println(" 比赛报名开始 ");
            System.out.println("比赛ID: " + competitionId);
            System.out.println("组别: " + group);
            System.out.println("学员ID: " + studentId);
            
            // 检查是否已报名（双重检查，防止并发问题）
            if (competitionRegistrationRepository.existsByCompetitionIdAndStudentId(
                competitionId, studentId)) {
                System.out.println("学员已报名该比赛");
                return ApiResponse.error("已报名该比赛");
            }
            
            MonthlyCompetition competition = monthlyCompetitionRepository.findById(competitionId)
                .orElseThrow(() -> new RuntimeException("比赛不存在"));
            
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学员不存在"));
            
            System.out.println("比赛费用: " + competition.getRegistrationFee());
            System.out.println("学员余额: " + student.getAccountBalance());
            
            // 检查账户余额
            if (student.getAccountBalance().compareTo(competition.getRegistrationFee()) < 0) {
                System.out.println("账户余额不足");
                return ApiResponse.error("账户余额不足，请先充值");
            }
            
            // 先创建报名记录（最容易失败的操作先执行）
            CompetitionRegistration registration = new CompetitionRegistration();
            registration.setCompetition(competition);
            registration.setStudent(student);
            registration.setCompetitionGroup(group);
            
            System.out.println("保存报名记录");
            competitionRegistrationRepository.save(registration);
            
            // 再扣除报名费（事务内一起提交）
            System.out.println("扣除报名费");
            student.setAccountBalance(student.getAccountBalance().subtract(competition.getRegistrationFee()));
            studentRepository.save(student);
            
            // 记录支付（在同一事务内）
            System.out.println("创建支付记录");
            PaymentRecord payment = new PaymentRecord();
            payment.setStudent(student);
            payment.setAmount(competition.getRegistrationFee().negate());
            payment.setPaymentMethod(com.pingpang.training.enums.PaymentMethod.ACCOUNT_DEDUCT);
            payment.setTransactionNo("COMP-" + System.currentTimeMillis());
            payment.setDescription("比赛报名费: " + competition.getName());
            paymentRecordRepository.save(payment);
            
            // 记录操作日志
            try {
                systemLogService.log(userDetails.getUser(), "COMPETITION_REGISTER", 
                    "报名比赛: " + competition.getName());
            } catch (Exception logError) {
                System.err.println("记录日志失败，但不影响主流程: " + logError.getMessage());
            }
            
            System.out.println("比赛报名成功");
            return ApiResponse.success("比赛报名成功", registration);
        } catch (Exception e) {
            System.err.println("比赛报名失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("比赛报名失败: " + e.getMessage());
        }
    }

    @GetMapping("/my-competitions")
    public ApiResponse<?> getMyCompetitions(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long studentId = userDetails.getUser().getId();
            System.out.println(" 获取我的比赛 ");
            System.out.println("学员ID: " + studentId);
            
            List<CompetitionRegistration> registrations = competitionRegistrationRepository
                .findByStudentId(studentId);
            
            System.out.println("找到报名记录数量: " + registrations.size());
            
            // 手动初始化懒加载关联以避免序列化问题
            for (CompetitionRegistration reg : registrations) {
                System.out.println("报名记录ID: " + reg.getId());
                if (reg.getCompetition() != null) {
                    // 触发competition的加载
                    reg.getCompetition().getName();
                    System.out.println("比赛名称: " + reg.getCompetition().getName());
                    System.out.println("比赛ID: " + reg.getCompetition().getId());
                } else {
                    System.out.println("警告：报名记录中的比赛对象为null");
                }
                if (reg.getStudent() != null) {
                    // 触发student的加载
                    reg.getStudent().getRealName();
                    System.out.println("学员姓名: " + reg.getStudent().getRealName());
                }
            }
            
            System.out.println("返回报名记录数据");
            return ApiResponse.success(registrations);
        } catch (Exception e) {
            System.err.println("获取我的比赛失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("获取我的比赛失败: " + e.getMessage());
        }
    }

    @GetMapping("/competitions/{competitionId}/schedule")
    public ApiResponse<?> getCompetitionSchedule(@PathVariable Long competitionId,
                                                @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // 验证是否已报名
            if (!competitionRegistrationRepository.existsByCompetitionIdAndStudentId(
                competitionId, userDetails.getUser().getId())) {
                return ApiResponse.error("未报名该比赛，无法查看赛程");
            }
            
            // 使用CompetitionService生成详细比赛赛程
            Map<String, Object> schedule = competitionService.generateCompetitionSchedule(competitionId);
            
            return ApiResponse.success("比赛赛程", schedule);
        } catch (Exception e) {
            System.err.println("获取比赛赛程失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("获取比赛赛程失败: " + e.getMessage());
        }
    }

    @GetMapping("/pending-evaluations")
    public ApiResponse<?> getPendingEvaluations(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long studentId = userDetails.getUser().getId();
            
            // 获取已完成但未评价的课程
            List<CourseBooking> completedBookings = courseBookingRepository
                .findByStudentIdAndStatus(studentId, BookingStatus.COMPLETED);
            
            List<CourseEvaluation> pendingEvaluations = new ArrayList<>();
            
            for (CourseBooking booking : completedBookings) {
                // 检查是否已有评价记录
                Optional<CourseEvaluation> existingEvaluation = courseEvaluationRepository
                    .findByBookingId(booking.getId());
                
                if (existingEvaluation.isPresent()) {
                    CourseEvaluation evaluation = existingEvaluation.get();
                    // 如果学员还未评价，则加入待评价列表
                    if (evaluation.getStudentEvaluation() == null || evaluation.getStudentEvaluation().trim().isEmpty()) {
                        pendingEvaluations.add(evaluation);
                    }
                } else {
                    // 如果没有评价记录，创建一个新的
                    CourseEvaluation newEvaluation = new CourseEvaluation();
                    newEvaluation.setBooking(booking);
                    newEvaluation = courseEvaluationRepository.save(newEvaluation);
                    pendingEvaluations.add(newEvaluation);
                }
            }
            
            return ApiResponse.success(pendingEvaluations);
        } catch (Exception e) {
            System.err.println("获取待评价课程失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("获取待评价课程失败: " + e.getMessage());
        }
    }
    

    @GetMapping("/my-evaluations")
    public ApiResponse<?> getMyEvaluations(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long studentId = userDetails.getUser().getId();
            List<CourseEvaluation> evaluations = courseEvaluationRepository.findByStudentId(studentId);
            return ApiResponse.success(evaluations);
        } catch (Exception e) {
            System.err.println("获取评价记录失败: " + e.getMessage());
            return ApiResponse.error("获取评价记录失败: " + e.getMessage());
        }
    }
    
    /**
     * 学员提交对教练的评价
     */
    @PostMapping("/evaluations/{evaluationId}")
    public ApiResponse<?> submitStudentEvaluation(@PathVariable Long evaluationId,
                                                 @RequestParam String evaluation,
                                                 @RequestParam(required = false) Integer rating,
                                                 @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long studentId = userDetails.getUser().getId();
            
            // 获取评价记录
            CourseEvaluation courseEvaluation = courseEvaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("评价记录不存在"));
            
            // 验证权限
            if (!courseEvaluation.getBooking().getStudent().getId().equals(studentId)) {
                return ApiResponse.error("无权限操作此评价");
            }
            
            // 检查是否已评价
            if (courseEvaluation.getStudentEvaluation() != null && 
                !courseEvaluation.getStudentEvaluation().trim().isEmpty()) {
                return ApiResponse.error("您已经评价过此课程");
            }
            
            // 验证评价内容
            if (evaluation == null || evaluation.trim().isEmpty()) {
                return ApiResponse.error("评价内容不能为空");
            }
            
            if (evaluation.length() > 500) {
                return ApiResponse.error("评价内容不能超过500字");
            }
            
            // 更新评价
            courseEvaluation.setStudentEvaluation(evaluation.trim());
            if (rating != null && rating >= 1 && rating <= 5) {
                courseEvaluation.setStudentRating(rating);
            }
            
            courseEvaluationRepository.save(courseEvaluation);
            
            // 记录操作日志
            try {
                systemLogService.log(userDetails.getUser(), "STUDENT_EVALUATION", 
                    "学员评价课程: " + courseEvaluation.getBooking().getCoach().getRealName());
            } catch (Exception logError) {
                System.err.println("日志记录失败: " + logError.getMessage());
            }
            
            return ApiResponse.success("评价提交成功", courseEvaluation);
        } catch (Exception e) {
            System.err.println("提交评价失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("提交评价失败: " + e.getMessage());
        }
    }

    // 更换教练
    @PostMapping("/change-coach")
    public ApiResponse<?> changeCoach(@RequestParam Long currentCoachId,
                                     @RequestParam Long newCoachId,
                                     @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long studentId = userDetails.getUser().getId();
            
            // 验证当前师生关系
            CoachStudentRelation currentRelation = coachStudentRelationRepository
                .findByCoachIdAndStudentIdAndStatus(currentCoachId, studentId, ApprovalStatus.APPROVED)
                .orElseThrow(() -> new RuntimeException("当前师生关系不存在"));
            
            // 检查新教练
            Coach newCoach = coachRepository.findById(newCoachId)
                .orElseThrow(() -> new RuntimeException("新教练不存在"));
            
            // 检查新教练学员数量
            long newCoachStudentCount = coachStudentRelationRepository
                .countByCoachIdAndStatus(newCoachId, ApprovalStatus.APPROVED);
            if (newCoachStudentCount >= 20) {
                return ApiResponse.error("新教练学员已满");
            }
            
            // 创建更换申请（需要三方同意）
            // 这里简化处理，直接更换
            currentRelation.setStatus(ApprovalStatus.REJECTED);
            coachStudentRelationRepository.save(currentRelation);
            
            CoachStudentRelation newRelation = new CoachStudentRelation();
            newRelation.setCoach(newCoach);
            newRelation.setStudent(studentRepository.findById(studentId).get());
            newRelation.setStatus(ApprovalStatus.PENDING);
            coachStudentRelationRepository.save(newRelation);
            
            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "COACH_CHANGE", 
                "申请更换教练: " + currentRelation.getCoach().getRealName() + " -> " + newCoach.getRealName());
            
            return ApiResponse.success("更换教练申请已提交", newRelation);
        } catch (Exception e) {
            return ApiResponse.error("更换教练失败: " + e.getMessage());
        }
    }

    // 个人统计
    @GetMapping("/statistics")
    public ApiResponse<?> getStudentStatistics(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long studentId = userDetails.getUser().getId();
            Student student = studentRepository.findById(studentId).get();
            
            long totalCoaches = coachStudentRelationRepository.countByStudentIdAndStatus(
                studentId, ApprovalStatus.APPROVED);
            long totalBookings = courseBookingRepository.countByStudentId(studentId);
            long completedBookings = courseBookingRepository.countByStudentIdAndStatus(
                studentId, BookingStatus.COMPLETED);
            long pendingBookings = courseBookingRepository.countByStudentIdAndStatus(
                studentId, BookingStatus.PENDING);
            long totalCompetitions = competitionRegistrationRepository.countByStudentId(studentId);
            long pendingEvaluations = courseEvaluationRepository
                .findPendingStudentEvaluations(studentId).size();
            
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("accountBalance", student.getAccountBalance());
            statistics.put("cancellationCount", student.getCancellationCount());
            statistics.put("totalCoaches", totalCoaches);
            statistics.put("totalBookings", totalBookings);
            statistics.put("completedBookings", completedBookings);
            statistics.put("pendingBookings", pendingBookings);
            statistics.put("totalCompetitions", totalCompetitions);
            statistics.put("pendingEvaluations", pendingEvaluations);
            
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            return ApiResponse.error("获取统计信息失败");
        }
    }
    
    // 教练更换功能
    
    @PostMapping("/request-coach-change")
    public ApiResponse<?> requestCoachChange(@RequestParam Long newCoachId,
                                           @RequestParam String reason,
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long studentId = userDetails.getUser().getId();
            CoachChangeRequest request = coachChangeService.submitCoachChangeRequest(studentId, newCoachId, reason);
            return ApiResponse.success("教练更换申请已提交", request);
        } catch (Exception e) {
            return ApiResponse.error("申请教练更换失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/coach-change-requests")
    public ApiResponse<?> getCoachChangeRequests(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long studentId = userDetails.getUser().getId();
            List<CoachChangeRequest> requests = coachChangeService.getPendingRequests(
                studentId, UserRole.STUDENT);
            return ApiResponse.success(requests);
        } catch (Exception e) {
            return ApiResponse.error("获取教练更换申请失败");
        }
    }
    
    @GetMapping("/coach-change-requests/{requestId}")
    public ApiResponse<?> getCoachChangeRequestDetails(@PathVariable Long requestId,
                                                      @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long studentId = userDetails.getUser().getId();
            CoachChangeRequest request = coachChangeService.getRequestDetails(requestId, studentId);
            return ApiResponse.success(request);
        } catch (Exception e) {
            return ApiResponse.error("获取申请详情失败: " + e.getMessage());
        }
    }
}
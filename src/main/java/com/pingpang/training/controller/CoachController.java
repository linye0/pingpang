package com.pingpang.training.controller;

import com.pingpang.training.dto.ApiResponse;
import com.pingpang.training.dto.ProfileUpdateRequest;
import com.pingpang.training.entity.*;
import com.pingpang.training.enums.ApprovalStatus;
import com.pingpang.training.enums.BookingStatus;
import com.pingpang.training.enums.UserRole;
import com.pingpang.training.repository.*;
import com.pingpang.training.security.UserDetailsImpl;
import com.pingpang.training.service.SystemLogService;
import com.pingpang.training.service.CoachChangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.pingpang.training.entity.Student;
import com.pingpang.training.repository.StudentRepository;

@RestController
@RequestMapping("/api/coach")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('COACH') or hasRole('SUPER_ADMIN')")
public class CoachController {

    @Autowired
    private CoachStudentRelationRepository coachStudentRelationRepository;

    @Autowired
    private CourseBookingRepository courseBookingRepository;

    @Autowired
    private CourseEvaluationRepository courseEvaluationRepository;

    @Autowired
    private CoachRepository coachRepository;

    @Autowired
    private SystemLogService systemLogService;
    
    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CoachWorkingTimeRepository coachWorkingTimeRepository;

    @Autowired
    private CoachChangeService coachChangeService;

    // 学员管理
    @GetMapping("/my-students")
    public ApiResponse<?> getMyStudents(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<CoachStudentRelation> relations = coachStudentRelationRepository.findByCoachIdAndStatus(
                userDetails.getUser().getId(), ApprovalStatus.APPROVED);
            return ApiResponse.success(relations);
        } catch (Exception e) {
            return ApiResponse.error("获取学员列表失败");
        }
    }

    @GetMapping("/pending-relations")
    public ApiResponse<?> getPendingRelations(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<CoachStudentRelation> relations = coachStudentRelationRepository.findByCoachIdAndStatus(
                userDetails.getUser().getId(), ApprovalStatus.PENDING);
            return ApiResponse.success(relations);
        } catch (Exception e) {
            return ApiResponse.error("获取待处理申请失败");
        }
    }

    @PostMapping("/approve-relation/{relationId}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<?> approveStudentRelation(@PathVariable Long relationId,
                                                @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println("=== 教练同意学员申请开始 ===");
            System.out.println("申请ID: " + relationId);
            System.out.println("教练ID: " + userDetails.getUser().getId());
            
            CoachStudentRelation relation = coachStudentRelationRepository.findById(relationId)
                .orElseThrow(() -> new RuntimeException("申请不存在"));

            if (!relation.getCoach().getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("无权限操作此申请");
            }

            relation.setStatus(ApprovalStatus.APPROVED);
            relation = coachStudentRelationRepository.save(relation);
            System.out.println("师生关系保存成功，状态: " + relation.getStatus());


            try {
                systemLogService.log(userDetails.getUser(), "RELATION_APPROVE", 
                    "同意学员申请: " + relation.getStudent().getRealName());
            } catch (Exception logException) {
                // 记录日志失败不应影响主要操作
                System.err.println("System log failed but main operation succeeded: " + logException.getMessage());
            }

            System.out.println("=== 教练同意学员申请成功 ===");
            return ApiResponse.success("学员申请已同意", relation);
        } catch (Exception e) {
            System.err.println("=== 教练同意学员申请失败 ===");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("操作失败: " + e.getMessage());
        }
    }

    @PostMapping("/reject-relation/{relationId}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<?> rejectStudentRelation(@PathVariable Long relationId,
                                               @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println("教练拒绝学员申请开始 ");
            System.out.println("申请ID: " + relationId);
            System.out.println("教练ID: " + userDetails.getUser().getId());
            
            CoachStudentRelation relation = coachStudentRelationRepository.findById(relationId)
                .orElseThrow(() -> new RuntimeException("申请不存在"));

            if (!relation.getCoach().getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("无权限操作此申请");
            }

            relation.setStatus(ApprovalStatus.REJECTED);
            relation = coachStudentRelationRepository.save(relation);
            System.out.println("师生关系保存成功，状态: " + relation.getStatus());


            try {
                systemLogService.log(userDetails.getUser(), "RELATION_REJECT", 
                    "拒绝学员申请: " + relation.getStudent().getRealName());
            } catch (Exception logException) {
                // 记录日志失败不应影响主要操作
                System.err.println("System log failed but main operation succeeded: " + logException.getMessage());
            }

            System.out.println(" 教练拒绝学员申请成功 ");
            return ApiResponse.success("学员申请已拒绝", relation);
        } catch (Exception e) {
            System.err.println(" 教练拒绝学员申请失败 ");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("操作失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/terminate-relation/{relationId}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<?> terminateRelation(@PathVariable Long relationId,
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println(" 教练终止师生关系开始 ");
            System.out.println("关系ID: " + relationId);
            System.out.println("教练ID: " + userDetails.getUser().getId());
            
            CoachStudentRelation relation = coachStudentRelationRepository.findById(relationId)
                .orElseThrow(() -> new RuntimeException("师生关系不存在"));

            if (!relation.getCoach().getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("无权限操作此关系");
            }

            relation.setStatus(ApprovalStatus.REJECTED); // 使用REJECTED状态表示已终止
            relation = coachStudentRelationRepository.save(relation);
            System.out.println("师生关系保存成功，状态: " + relation.getStatus());


            try {
                systemLogService.log(userDetails.getUser(), "RELATION_TERMINATE", 
                    "终止师生关系: " + relation.getStudent().getRealName());
            } catch (Exception logException) {
                // 记录日志失败不应影响主要操作
                System.err.println("System log failed but main operation succeeded: " + logException.getMessage());
            }

            System.out.println(" 教练终止师生关系成功 ");
            return ApiResponse.success("师生关系已解除", relation);
        } catch (Exception e) {
            System.err.println(" 教练终止师生关系失败 ");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("操作失败: " + e.getMessage());
        }
    }

    // 课程预约管理
    @GetMapping("/my-bookings")
    public ApiResponse<?> getMyBookings(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println(" 获取教练所有课程开始 ");
            System.out.println("教练ID: " + userDetails.getUser().getId());
            
            List<CourseBooking> bookings = courseBookingRepository.findByCoachId(userDetails.getUser().getId());
            
            System.out.println("查询到的课程总数: " + bookings.size());
            
            // 详细输出每个课程
            for (CourseBooking booking : bookings) {
                System.out.println("课程详情: ID=" + booking.getId() + 
                    ", 开始时间=" + booking.getStartTime() + 
                    ", 结束时间=" + booking.getEndTime() + 
                    ", 状态=" + booking.getStatus() +
                    ", 学员=" + (booking.getStudent() != null ? booking.getStudent().getRealName() : "null") +
                    ", 球台=" + booking.getTableNumber());
            }
            
            // 统计今日课程
            LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime todayEnd = todayStart.plusDays(1);
            
            System.out.println("今日时间范围: " + todayStart + " 到 " + todayEnd);
            
            long todayCount = bookings.stream()
                .filter(booking -> {
                    LocalDateTime startTime = booking.getStartTime();
                    boolean isToday = startTime.isAfter(todayStart) && startTime.isBefore(todayEnd);
                    boolean isActive = booking.getStatus().name().equals("PENDING") || booking.getStatus().name().equals("CONFIRMED");
                    
                    if (isToday) {
                        System.out.println("今日课程: " + booking.getId() + 
                            ", 时间=" + startTime + 
                            ", 状态=" + booking.getStatus() + 
                            ", 是否活跃=" + isActive);
                    }
                    
                    return isToday && isActive;
                })
                .count();
                
            System.out.println("今日课程统计: " + todayCount);
            System.out.println("=== 获取教练所有课程完成 ===");
            
            return ApiResponse.success(bookings);
        } catch (Exception e) {
            System.err.println("获取课程预约失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("获取课程预约失败: " + e.getMessage());
        }
    }

    @GetMapping("/pending-bookings")
    public ApiResponse<?> getPendingBookings(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<CourseBooking> bookings = courseBookingRepository.findByCoachIdAndStatus(
                userDetails.getUser().getId(), BookingStatus.PENDING);
            return ApiResponse.success(bookings);
        } catch (Exception e) {
            return ApiResponse.error("获取待处理预约失败");
        }
    }

    @PostMapping("/approve-booking/{bookingId}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<?> approveBooking(@PathVariable Long bookingId,
                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println("=== 教练确认预约开始 ===");
            System.out.println("预约ID: " + bookingId);
            
            CourseBooking booking = courseBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("预约不存在"));

            if (!booking.getCoach().getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("无权限操作此预约");
            }

            booking.setStatus(BookingStatus.CONFIRMED);
            booking = courseBookingRepository.save(booking);
            System.out.println("预约状态更新成功: " + booking.getStatus());

            // 记录操作日志 - 使用独立事务避免影响主要操作
            try {
                systemLogService.log(userDetails.getUser(), "BOOKING_APPROVE", 
                    "确认课程预约: " + booking.getStudent().getRealName());
            } catch (Exception logException) {
                // 记录日志失败不应影响主要操作
                System.err.println("System log failed but main operation succeeded: " + logException.getMessage());
            }

            System.out.println(" 教练确认预约成功 ");
            return ApiResponse.success("课程预约已确认", booking);
        } catch (Exception e) {
            System.err.println(" 教练确认预约失败 ");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("操作失败: " + e.getMessage());
        }
    }

    @PostMapping("/reject-booking/{bookingId}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<?> rejectBooking(@PathVariable Long bookingId,
                                       @RequestParam(required = false) String reason,
                                       @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println(" 教练拒绝预约开始 ");
            System.out.println("预约ID: " + bookingId);
            System.out.println("拒绝原因: " + reason);
            
            CourseBooking booking = courseBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("预约不存在"));

            if (!booking.getCoach().getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("无权限操作此预约");
            }

            booking.setStatus(BookingStatus.CANCELLED);
            if (reason != null && !reason.trim().isEmpty()) {
                String existingNotes = booking.getNotes() != null ? booking.getNotes() : "";
                booking.setNotes(existingNotes + "\n拒绝原因: " + reason);
            }
            booking = courseBookingRepository.save(booking);
            System.out.println("预约状态更新成功: " + booking.getStatus());


            try {
                systemLogService.log(userDetails.getUser(), "BOOKING_REJECT", 
                    "拒绝课程预约: " + booking.getStudent().getRealName());
            } catch (Exception logException) {
                // 记录日志失败不应影响主要操作
                System.err.println("System log failed but main operation succeeded: " + logException.getMessage());
            }

            System.out.println(" 教练拒绝预约成功 ");
            return ApiResponse.success("课程预约已拒绝", booking);
        } catch (Exception e) {
            System.err.println(" 教练拒绝预约失败 ");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("操作失败: " + e.getMessage());
        }
    }

    @PostMapping("/complete-booking/{bookingId}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<?> completeBooking(@PathVariable Long bookingId,
                                         @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println(" 教练完成课程开始 ");
            System.out.println("预约ID: " + bookingId);
            
            CourseBooking booking = courseBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("预约不存在"));

            if (!booking.getCoach().getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("无权限操作此预约");
            }

            booking.setStatus(BookingStatus.COMPLETED);
            booking = courseBookingRepository.save(booking);
            System.out.println("预约状态更新成功: " + booking.getStatus());

            // 创建评价记录
            if (!courseEvaluationRepository.existsByBookingId(bookingId)) {
                CourseEvaluation evaluation = new CourseEvaluation(booking);
                evaluation = courseEvaluationRepository.save(evaluation);
                System.out.println("评价记录创建成功: " + evaluation.getId());
            } else {
                System.out.println("评价记录已存在，跳过创建");
            }

            // 记录操作日志 - 使用独立事务避免影响主要操作
            try {
                systemLogService.log(userDetails.getUser(), "BOOKING_COMPLETE", 
                    "完成课程: " + booking.getStudent().getRealName());
            } catch (Exception logException) {
                // 记录日志失败不应影响主要操作
                System.err.println("System log failed but main operation succeeded: " + logException.getMessage());
            }

            System.out.println(" 教练完成课程成功 ");
            return ApiResponse.success("课程已标记完成", booking);
        } catch (Exception e) {
            System.err.println(" 教练完成课程失败 ");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("操作失败: " + e.getMessage());
        }
    }

    // 确认学员的取消申请
    @PostMapping("/confirm-cancel-booking/{bookingId}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<?> confirmCancelBooking(@PathVariable Long bookingId,
                                              @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println(" 教练确认取消预约开始 ");
            System.out.println("预约ID: " + bookingId);
            
            CourseBooking booking = courseBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("预约不存在"));

            if (!booking.getCoach().getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("无权限操作此预约");
            }

            if (booking.getStatus() != BookingStatus.PENDING_CANCELLATION) {
                return ApiResponse.error("该预约不是待取消确认状态");
            }

            // 确认取消，退款给学员
            Student student = booking.getStudent();
            if (booking.getCost() != null) {
                student.setAccountBalance(student.getAccountBalance().add(booking.getCost()));
                student = studentRepository.save(student);
                System.out.println("学员退款成功，新余额: " + student.getAccountBalance());
            }

            booking.setStatus(BookingStatus.CANCELLED);
            booking = courseBookingRepository.save(booking);
            System.out.println("预约状态更新成功: " + booking.getStatus());

            // 记录操作日志 - 使用独立事务避免影响主要操作
            try {
                systemLogService.log(userDetails.getUser(), "BOOKING_CANCEL_CONFIRM", 
                    "确认取消课程预约: " + booking.getStudent().getRealName());
            } catch (Exception logException) {
                // 记录日志失败不应影响主要操作
                System.err.println("System log failed but main operation succeeded: " + logException.getMessage());
            }

            System.out.println(" 教练确认取消预约成功 ");
            return ApiResponse.success("已确认取消预约，费用已退还给学员", booking);
        } catch (Exception e) {
            System.err.println("教练确认取消预约失败 ");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("操作失败: " + e.getMessage());
        }
    }

    // 拒绝学员的取消申请
    @PostMapping("/reject-cancel-booking/{bookingId}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<?> rejectCancelBooking(@PathVariable Long bookingId,
                                             @RequestParam(required = false) String reason,
                                             @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println(" 教练拒绝取消申请开始 ");
            System.out.println("预约ID: " + bookingId);
            System.out.println("拒绝原因: " + reason);
            
            CourseBooking booking = courseBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("预约不存在"));

            if (!booking.getCoach().getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("无权限操作此预约");
            }

            if (booking.getStatus() != BookingStatus.PENDING_CANCELLATION) {
                return ApiResponse.error("该预约不是待取消确认状态");
            }

            // 拒绝取消，恢复到原状态
            booking.setStatus(BookingStatus.CONFIRMED);
            if (reason != null && !reason.trim().isEmpty()) {
                String existingRemarks = booking.getRemarks() != null ? booking.getRemarks() : "";
                booking.setRemarks(existingRemarks + "\n教练拒绝取消原因: " + reason);
            }
            booking = courseBookingRepository.save(booking);
            System.out.println("预约状态更新成功: " + booking.getStatus());


            try {
                systemLogService.log(userDetails.getUser(), "BOOKING_CANCEL_REJECT", 
                    "拒绝取消课程预约: " + booking.getStudent().getRealName());
            } catch (Exception logException) {
                // 记录日志失败不应影响主要操作
                System.err.println("System log failed but main operation succeeded: " + logException.getMessage());
            }

            System.out.println(" 教练拒绝取消申请成功");
            return ApiResponse.success("已拒绝取消申请", booking);
        } catch (Exception e) {
            System.err.println("=== 教练拒绝取消申请失败 ===");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("操作失败: " + e.getMessage());
        }
    }


    @GetMapping("/pending-evaluations")
    public ApiResponse<?> getPendingEvaluations(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long coachId = userDetails.getUser().getId();
            
            // 获取已完成但未评价的课程
            List<CourseBooking> completedBookings = courseBookingRepository
                .findByCoachIdAndStatus(coachId, BookingStatus.COMPLETED);
            
            List<CourseEvaluation> pendingEvaluations = new ArrayList<>();
            
            for (CourseBooking booking : completedBookings) {
                // 检查是否已有评价记录
                Optional<CourseEvaluation> existingEvaluation = courseEvaluationRepository
                    .findByBookingId(booking.getId());
                
                if (existingEvaluation.isPresent()) {
                    CourseEvaluation evaluation = existingEvaluation.get();
                    // 如果教练还未评价，则加入待评价列表
                    if (evaluation.getCoachEvaluation() == null || evaluation.getCoachEvaluation().trim().isEmpty()) {
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

    /**
     * 获取教练的评价记录
     */
    @GetMapping("/my-evaluations")
    public ApiResponse<?> getMyEvaluations(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long coachId = userDetails.getUser().getId();
            List<CourseEvaluation> evaluations = courseEvaluationRepository.findByCoachId(coachId);
            return ApiResponse.success(evaluations);
        } catch (Exception e) {
            System.err.println("获取评价记录失败: " + e.getMessage());
            return ApiResponse.error("获取评价记录失败: " + e.getMessage());
        }
    }


    @PostMapping("/evaluations/{evaluationId}")
    public ApiResponse<?> submitCoachEvaluation(@PathVariable Long evaluationId,
                                               @RequestParam String evaluation,
                                               @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long coachId = userDetails.getUser().getId();
            
            // 获取评价记录
            CourseEvaluation courseEvaluation = courseEvaluationRepository.findById(evaluationId)
                .orElseThrow(() -> new RuntimeException("评价记录不存在"));

            // 验证权限
            if (!courseEvaluation.getBooking().getCoach().getId().equals(coachId)) {
                return ApiResponse.error("无权限操作此评价");
            }
            
            // 检查是否已评价
            if (courseEvaluation.getCoachEvaluation() != null && 
                !courseEvaluation.getCoachEvaluation().trim().isEmpty()) {
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
            courseEvaluation.setCoachEvaluation(evaluation.trim());
            courseEvaluationRepository.save(courseEvaluation);

            // 记录操作日志
            try {
                systemLogService.log(userDetails.getUser(), "COACH_EVALUATION", 
                    "教练评价学员: " + courseEvaluation.getBooking().getStudent().getRealName());
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

    // 教练信息管理
    @GetMapping("/profile")
    public ApiResponse<?> getProfile(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Coach coach = coachRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("教练信息不存在"));
            return ApiResponse.success(coach);
        } catch (Exception e) {
            return ApiResponse.error("获取教练信息失败");
        }
    }

    @PutMapping("/profile")
    public ApiResponse<?> updateProfile(@Valid @RequestBody ProfileUpdateRequest request,
                                       @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Coach existingCoach = coachRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("教练信息不存在"));

            // 更新基本用户信息
            if (request.getRealName() != null && !request.getRealName().trim().isEmpty()) {
                existingCoach.setRealName(request.getRealName().trim());
            }
            if (request.getEmail() != null && !request.getEmail().trim().isEmpty()) {
                existingCoach.setEmail(request.getEmail().trim());
            }
            if (request.getGender() != null) {
                existingCoach.setGender(request.getGender());
            }
            if (request.getAge() != null && request.getAge() > 0) {
                existingCoach.setAge(request.getAge());
            }
            if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
                existingCoach.setPhone(request.getPhone());
            }
            if (request.getAvatar() != null) {
                existingCoach.setAvatar(request.getAvatar());
            }
            
            // 更新教练特有信息
            if (request.getAchievements() != null) {
                existingCoach.setAchievements(request.getAchievements());
            }
            // 注意：不允许修改等级、审核状态、手机号等敏感信息

            existingCoach.setUpdatedAt(LocalDateTime.now());
            Coach savedCoach = coachRepository.save(existingCoach);

            // 记录操作日志 - 使用独立事务避免影响主要操作
            try {
                systemLogService.log(userDetails.getUser(), "PROFILE_UPDATE", "更新教练信息");
            } catch (Exception logException) {
                // 记录日志失败不应影响主要操作
                System.err.println("System log failed but main operation succeeded: " + logException.getMessage());
            }

            return ApiResponse.success("教练信息更新成功", savedCoach);
        } catch (Exception e) {
            return ApiResponse.error("更新教练信息失败: " + e.getMessage());
        }
    }

    // 获取系统消息
    @GetMapping("/messages")
    public ApiResponse<?> getMessages(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // 这里应该从SystemMessage表获取消息，暂时返回空列表
            // 实际实现需要创建SystemMessage实体和相关的Repository
            return ApiResponse.success(new ArrayList<>());
        } catch (Exception e) {
            return ApiResponse.error("获取消息失败: " + e.getMessage());
        }
    }

    // 标记消息为已读
    @PostMapping("/messages/{messageId}/read")
    public ApiResponse<?> markMessageAsRead(@PathVariable Long messageId,
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // 实际实现需要更新SystemMessage的isRead状态
            return ApiResponse.success("消息已标记为已读");
        } catch (Exception e) {
            return ApiResponse.error("操作失败: " + e.getMessage());
        }
    }

    // 标记所有消息为已读
    @PostMapping("/messages/read-all")
    public ApiResponse<?> markAllMessagesAsRead(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // 实际实现需要批量更新用户的所有未读消息
            return ApiResponse.success("所有消息已标记为已读");
        } catch (Exception e) {
            return ApiResponse.error("操作失败: " + e.getMessage());
        }
    }

    // 申请取消预约
    @PostMapping("/cancel-booking/{bookingId}")
    public ApiResponse<?> requestCancelBooking(@PathVariable Long bookingId,
                                              @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            CourseBooking booking = courseBookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("预约不存在"));

            if (!booking.getCoach().getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("无权限操作此预约");
            }

            // 检查是否符合24小时规则
            LocalDateTime now = LocalDateTime.now();
            if (booking.getStartTime().isBefore(now.plusHours(24))) {
                return ApiResponse.error("距离上课时间不足24小时，无法取消");
            }

            // 检查本月取消次数
            Coach coach = coachRepository.findById(userDetails.getUser().getId()).get();
            if (coach.getCancellationCount() >= 3) {
                return ApiResponse.error("本月取消次数已达上限(3次)");
            }

            // 设置为待取消状态，等待学员确认
            booking.setStatus(BookingStatus.PENDING_CANCELLATION);
            courseBookingRepository.save(booking);

            // 增加教练取消次数
            coach.setCancellationCount(coach.getCancellationCount() + 1);
            coachRepository.save(coach);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "BOOKING_CANCEL_REQUEST", 
                "申请取消课程预约: " + booking.getStudent().getRealName());

            return ApiResponse.success("取消申请已提交，等待学员确认");
        } catch (Exception e) {
            return ApiResponse.error("操作失败: " + e.getMessage());
        }
    }

    // 获取周课表
    @GetMapping("/weekly-schedule")
    public ApiResponse<?> getWeeklySchedule(@RequestParam String startTime,
                                           @RequestParam String endTime,
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println("=== 获取周课表开始 ===");
            System.out.println("教练ID: " + userDetails.getUser().getId());
            System.out.println("原始开始时间: " + startTime);
            System.out.println("原始结束时间: " + endTime);
            
            // 处理时间字符串，支持ISO格式
            LocalDateTime start = LocalDateTime.parse(startTime.replace("Z", ""));
            LocalDateTime end = LocalDateTime.parse(endTime.replace("Z", ""));
            
            System.out.println("解析后开始时间: " + start);
            System.out.println("解析后结束时间: " + end);
            
            List<CourseBooking> schedule = courseBookingRepository.findByCoachIdAndTimeRange(
                userDetails.getUser().getId(), start, end);
            
            System.out.println("查询到的课程数量: " + schedule.size());
            
            // 输出每个课程的详细信息
            for (CourseBooking booking : schedule) {
                System.out.println("课程: ID=" + booking.getId() + 
                    ", 开始时间=" + booking.getStartTime() + 
                    ", 结束时间=" + booking.getEndTime() + 
                    ", 状态=" + booking.getStatus() +
                    ", 学员=" + (booking.getStudent() != null ? booking.getStudent().getRealName() : "null"));
            }
            
            System.out.println(" 获取周课表完成 ");
            return ApiResponse.success(schedule);
        } catch (Exception e) {
            System.err.println("获取课表失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("获取课表失败: " + e.getMessage());
        }
    }

    // 统计信息
    @GetMapping("/statistics")
    public ApiResponse<?> getCoachStatistics(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long coachId = userDetails.getUser().getId();
            
            long totalStudents = coachStudentRelationRepository.countByCoachIdAndStatus(coachId, ApprovalStatus.APPROVED);
            long pendingRelations = coachStudentRelationRepository.countByCoachIdAndStatus(coachId, ApprovalStatus.PENDING);
            long totalBookings = courseBookingRepository.countByCoachId(coachId);
            long pendingBookings = courseBookingRepository.countByCoachIdAndStatus(coachId, BookingStatus.PENDING);
            long completedBookings = courseBookingRepository.countByCoachIdAndStatus(coachId, BookingStatus.COMPLETED);
            long pendingEvaluations = courseEvaluationRepository.findPendingCoachEvaluations(coachId).size();

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalStudents", totalStudents);
            statistics.put("pendingRelations", pendingRelations);
            statistics.put("totalBookings", totalBookings);
            statistics.put("pendingBookings", pendingBookings);
            statistics.put("completedBookings", completedBookings);
            statistics.put("pendingEvaluations", pendingEvaluations);
            
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            return ApiResponse.error("获取统计信息失败");
        }
    }

    // 获取特定学员的课程安排
    @GetMapping("/student-bookings/{studentId}")
    public ApiResponse<?> getStudentBookings(@PathVariable Long studentId,
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println("获取学员课表开始 ");
            System.out.println("学员ID: " + studentId);
            System.out.println("教练ID: " + userDetails.getUser().getId());
            System.out.println("教练用户名: " + userDetails.getUsername());
            
            // 验证是否是该教练的学员
            System.out.println("验证师生关系...");
            Optional<CoachStudentRelation> relationOpt = coachStudentRelationRepository
                .findByCoachIdAndStudentIdAndStatus(userDetails.getUser().getId(), studentId, ApprovalStatus.APPROVED);
            
            System.out.println("师生关系查询结果: " + relationOpt.isPresent());
            
            if (!relationOpt.isPresent()) {
                System.out.println("无师生关系，返回错误");
                return ApiResponse.error("无权限查看该学员信息");
            }
            
            System.out.println("师生关系验证通过，查询课程安排");
            
            // 获取该学员与当前教练的课程安排
            List<CourseBooking> bookings = courseBookingRepository
                .findByCoachIdAndStudentIdOrderByStartTimeDesc(userDetails.getUser().getId(), studentId);
            
            System.out.println("找到课程数量: " + bookings.size());
            
            // 手动初始化懒加载的关联对象
            for (CourseBooking booking : bookings) {
                if (booking.getStudent() != null) {
                    booking.getStudent().getRealName(); // 触发学员信息加载
                }
                if (booking.getCoach() != null) {
                    booking.getCoach().getRealName(); // 触发教练信息加载
                }
            }
            
            System.out.println("获取学员课表成功 ");
            return ApiResponse.success(bookings);
        } catch (Exception e) {
            System.err.println(" 获取学员课表失败 ");
            System.err.println("错误类型: " + e.getClass().getSimpleName());
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("获取学员课表失败: " + e.getMessage());
        }
    }

    // 获取特定学员的评价记录
    @GetMapping("/student-evaluations/{studentId}")
    public ApiResponse<?> getStudentEvaluations(@PathVariable Long studentId,
                                              @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println(" 获取学员评价记录开始 ");
            System.out.println("学员ID: " + studentId);
            System.out.println("教练ID: " + userDetails.getUser().getId());
            System.out.println("教练用户名: " + userDetails.getUsername());
            
            // 验证是否是该教练的学员
            System.out.println("验证师生关系...");
            Optional<CoachStudentRelation> relationOpt = coachStudentRelationRepository
                .findByCoachIdAndStudentIdAndStatus(userDetails.getUser().getId(), studentId, ApprovalStatus.APPROVED);
            
            System.out.println("师生关系查询结果: " + relationOpt.isPresent());
            
            if (!relationOpt.isPresent()) {
                System.out.println("无师生关系，返回错误");
                return ApiResponse.error("无权限查看该学员信息");
            }
            
            System.out.println("师生关系验证通过，查询评价记录...");
            
            // 获取该学员与当前教练的评价记录
            List<CourseEvaluation> evaluations = courseEvaluationRepository
                .findByCoachIdAndStudentId(userDetails.getUser().getId(), studentId);
            
            System.out.println("找到评价记录数量: " + evaluations.size());
            
            // 手动初始化懒加载的关联对象
            for (CourseEvaluation evaluation : evaluations) {
                if (evaluation.getBooking() != null) {
                    CourseBooking booking = evaluation.getBooking();
                    if (booking.getStudent() != null) {
                        booking.getStudent().getRealName();
                    }
                    if (booking.getCoach() != null) {
                        booking.getCoach().getRealName();
                    }
                }
            }
            
            System.out.println(" 获取学员评价记录成功 ");
            return ApiResponse.success(evaluations);
        } catch (Exception e) {
            System.err.println(" 获取学员评价记录失败 ");
            System.err.println("错误类型: " + e.getClass().getSimpleName());
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("获取学员评价记录失败: " + e.getMessage());
        }
    }

    @GetMapping("/working-time")
    public ApiResponse<?> getWorkingTime(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println(" 获取教练工作时间安排 ");
            System.out.println("教练ID: " + userDetails.getUser().getId());
            
            // 获取周工作时间安排
            List<CoachWorkingTime> weeklySchedule = coachWorkingTimeRepository
                .findByCoachIdAndSpecificDateIsNull(userDetails.getUser().getId());
            
            System.out.println("找到工作时间安排数量: " + weeklySchedule.size());
            
            Map<String, Object> result = new HashMap<>();
            result.put("weeklySchedule", weeklySchedule);
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            System.err.println("获取工作时间安排失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("获取工作时间安排失败: " + e.getMessage());
        }
    }
    
    // 设置教练的工作时间安排
    @PostMapping("/working-time")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<?> setWorkingTime(@RequestBody CoachWorkingTime workingTime,
                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println(" 设置教练工作时间安排 ");
            System.out.println("教练ID: " + userDetails.getUser().getId());
            System.out.println("星期: " + workingTime.getDayOfWeek());
            System.out.println("开始时间: " + workingTime.getStartTime());
            System.out.println("结束时间: " + workingTime.getEndTime());
            
            // 设置教练信息
            Coach coach = coachRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("教练信息不存在"));
            workingTime.setCoach(coach);
            
            // 保存工作时间安排
            CoachWorkingTime savedWorkingTime = coachWorkingTimeRepository.save(workingTime);
            
            System.out.println("工作时间安排保存成功，ID: " + savedWorkingTime.getId());
            return ApiResponse.success("工作时间设置成功", savedWorkingTime);
        } catch (Exception e) {
            System.err.println("设置工作时间安排失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("设置工作时间失败: " + e.getMessage());
        }
    }
    
    // 批量设置一周的工作时间
    @PostMapping("/working-time/weekly")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<?> setWeeklyWorkingTime(@RequestBody List<CoachWorkingTime> weeklySchedule,
                                              @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println(" 批量设置教练周工作时间 ");
            System.out.println("教练ID: " + userDetails.getUser().getId());
            System.out.println("设置的时间段数量: " + weeklySchedule.size());
            
            Coach coach = coachRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("教练信息不存在"));
            
            List<CoachWorkingTime> savedSchedule = new ArrayList<>();
            
            for (CoachWorkingTime workingTime : weeklySchedule) {
                // 设置教练信息
                workingTime.setCoach(coach);
                
                // 如果是周重复设置，清空specificDate
                if (workingTime.getSpecificDate() == null) {
                    // 先删除该星期几的现有安排
                    coachWorkingTimeRepository.deleteByCoachIdAndDayOfWeekAndSpecificDateIsNull(
                        coach.getId(), workingTime.getDayOfWeek());
                }
                
                // 保存新的工作时间安排
                CoachWorkingTime saved = coachWorkingTimeRepository.save(workingTime);
                savedSchedule.add(saved);
                
                System.out.println(String.format("设置成功 - 星期%d: %s-%s", 
                    workingTime.getDayOfWeek(), 
                    workingTime.getStartTime(), 
                    workingTime.getEndTime()));
            }
            
            System.out.println("批量设置工作时间完成");
            return ApiResponse.success("周工作时间设置成功", savedSchedule);
        } catch (Exception e) {
            System.err.println("批量设置工作时间失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("设置周工作时间失败: " + e.getMessage());
        }
    }
    
    // 删除工作时间安排
    @DeleteMapping("/working-time/{id}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<?> deleteWorkingTime(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println(" 删除工作时间安排 ");
            System.out.println("工作时间ID: " + id);
            
            CoachWorkingTime workingTime = coachWorkingTimeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("工作时间安排不存在"));
            
            // 验证权限
            if (!workingTime.getCoach().getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("无权限删除此工作时间安排");
            }
            
            coachWorkingTimeRepository.delete(workingTime);
            
            System.out.println("工作时间安排删除成功");
            return ApiResponse.success("工作时间删除成功");
        } catch (Exception e) {
            System.err.println("删除工作时间安排失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("删除工作时间失败: " + e.getMessage());
        }
    }
    
    // 更新工作时间的可用状态
    @PutMapping("/working-time/{id}/availability")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<?> updateWorkingTimeAvailability(@PathVariable Long id,
                                                        @RequestParam Boolean isAvailable,
                                                        @RequestParam(required = false) String remarks,
                                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println(" 更新工作时间可用状态 ");
            System.out.println("工作时间ID: " + id);
            System.out.println("新状态: " + isAvailable);
            
            CoachWorkingTime workingTime = coachWorkingTimeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("工作时间安排不存在"));
            
            // 验证权限
            if (!workingTime.getCoach().getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("无权限修改此工作时间安排");
            }
            
            workingTime.setIsAvailable(isAvailable);
            if (remarks != null) {
                workingTime.setRemarks(remarks);
            }
            
            CoachWorkingTime updated = coachWorkingTimeRepository.save(workingTime);
            
            System.out.println("工作时间状态更新成功");
            return ApiResponse.success("工作时间状态更新成功", updated);
        } catch (Exception e) {
            System.err.println("更新工作时间状态失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("更新工作时间状态失败: " + e.getMessage());
        }
    }
    
    // 教练更换审核功能
    
    @GetMapping("/coach-change-requests")
    public ApiResponse<?> getCoachChangeRequests(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long coachId = userDetails.getUser().getId();
            List<CoachChangeRequest> requests = coachChangeService.getPendingRequests(
                coachId, UserRole.COACH);
            return ApiResponse.success(requests);
        } catch (Exception e) {
            return ApiResponse.error("获取教练更换申请失败");
        }
    }
    
    @GetMapping("/coach-change-requests/{requestId}")
    public ApiResponse<?> getCoachChangeRequestDetails(@PathVariable Long requestId,
                                                      @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long coachId = userDetails.getUser().getId();
            CoachChangeRequest request = coachChangeService.getRequestDetails(requestId, coachId);
            return ApiResponse.success(request);
        } catch (Exception e) {
            return ApiResponse.error("获取申请详情失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/coach-change-requests/{requestId}/current-coach-approval")
    public ApiResponse<?> currentCoachApproval(@PathVariable Long requestId,
                                              @RequestParam boolean approved,
                                              @RequestParam(required = false) String comment,
                                              @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long coachId = userDetails.getUser().getId();
            CoachChangeRequest request = coachChangeService.currentCoachApproval(
                requestId, coachId, approved, comment);
            return ApiResponse.success("审核完成", request);
        } catch (Exception e) {
            return ApiResponse.error("审核失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/coach-change-requests/{requestId}/new-coach-approval")
    public ApiResponse<?> newCoachApproval(@PathVariable Long requestId,
                                          @RequestParam boolean approved,
                                          @RequestParam(required = false) String comment,
                                          @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long coachId = userDetails.getUser().getId();
            CoachChangeRequest request = coachChangeService.newCoachApproval(
                requestId, coachId, approved, comment);
            return ApiResponse.success("审核完成", request);
        } catch (Exception e) {
            return ApiResponse.error("审核失败: " + e.getMessage());
        }
    }

    // 简单的测试端点
    @GetMapping("/test")
    public ApiResponse<?> testEndpoint(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Map<String, Object> result = new HashMap<>();
        result.put("message", "API正常工作");
        result.put("timestamp", new java.util.Date());
        result.put("coachId", userDetails != null ? userDetails.getUser().getId() : "未登录");
        result.put("coachName", userDetails != null ? userDetails.getUser().getRealName() : "未登录");
        return ApiResponse.success(result);
    }

    // 调试方法：检查教练课程数据
    @GetMapping("/debug/my-data")
    public ApiResponse<?> debugMyData(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println(" 调试教练数据 ");
            System.out.println("教练ID: " + userDetails.getUser().getId());
            System.out.println("教练姓名: " + userDetails.getUser().getRealName());
            
            // 获取所有课程
            List<CourseBooking> allBookings = courseBookingRepository.findByCoachId(userDetails.getUser().getId());
            System.out.println("所有课程数量: " + allBookings.size());
            
            Map<String, Object> result = new HashMap<>();
            result.put("coachId", userDetails.getUser().getId());
            result.put("coachName", userDetails.getUser().getRealName());
            result.put("totalBookings", allBookings.size());
            
            if (allBookings.isEmpty()) {
                result.put("message", "教练暂无课程数据");
                result.put("suggestion", "请先创建一些测试课程数据");
            } else {
                // 按状态分组统计
                Map<String, Long> statusStats = allBookings.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                        b -> b.getStatus().name(),
                        java.util.stream.Collectors.counting()
                    ));
                result.put("statusStats", statusStats);
                
                // 最近的几个课程
                List<Map<String, Object>> recentBookings = allBookings.stream()
                    .limit(5)
                    .map(booking -> {
                        Map<String, Object> bookingInfo = new HashMap<>();
                        bookingInfo.put("id", booking.getId());
                        bookingInfo.put("startTime", booking.getStartTime());
                        bookingInfo.put("endTime", booking.getEndTime());
                        bookingInfo.put("status", booking.getStatus());
                        bookingInfo.put("studentName", booking.getStudent() != null ? booking.getStudent().getRealName() : "null");
                        bookingInfo.put("tableNumber", booking.getTableNumber());
                        return bookingInfo;
                    })
                    .collect(java.util.stream.Collectors.toList());
                result.put("recentBookings", recentBookings);
            }
            
            System.out.println("调试数据准备完成");
            return ApiResponse.success(result);
        } catch (Exception e) {
            System.err.println("调试数据获取失败: " + e.getMessage());
            e.printStackTrace();
            
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("error", e.getMessage());
            errorResult.put("errorType", e.getClass().getSimpleName());
            errorResult.put("message", "获取调试数据时发生错误");
            
            return ApiResponse.error("调试失败: " + e.getMessage());
        }
    }

    // 创建测试课程数据
    @PostMapping("/debug/create-test-data")
    public ApiResponse<?> createTestData(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println(" 创建测试课程数据 ");
            Long coachId = userDetails.getUser().getId();
            System.out.println("教练ID: " + coachId);
            
            // 查找一个学员
            List<Student> students = studentRepository.findAll();
            if (students.isEmpty()) {
                return ApiResponse.error("系统中没有学员数据，无法创建测试课程");
            }
            Student testStudent = students.get(0);
            
            // 获取教练对象
            Coach coach = (Coach) userDetails.getUser();
            
            List<CourseBooking> testBookings = new ArrayList<>();
            
            // 创建今天的测试课程
            LocalDateTime today = LocalDateTime.now().withHour(10).withMinute(0).withSecond(0).withNano(0);
            CourseBooking todayBooking = new CourseBooking();
            todayBooking.setCoach(coach);
            todayBooking.setStudent(testStudent);
            todayBooking.setStartTime(today);
            todayBooking.setEndTime(today.plusHours(1));
            todayBooking.setTableNumber("T001");
            todayBooking.setStatus(BookingStatus.CONFIRMED);
            todayBooking.setCost(new java.math.BigDecimal("100"));
            todayBooking.setRemarks("测试课程-今日");
            testBookings.add(todayBooking);
            
            // 创建明天的测试课程
            LocalDateTime tomorrow = today.plusDays(1);
            CourseBooking tomorrowBooking = new CourseBooking();
            tomorrowBooking.setCoach(coach);
            tomorrowBooking.setStudent(testStudent);
            tomorrowBooking.setStartTime(tomorrow);
            tomorrowBooking.setEndTime(tomorrow.plusHours(1));
            tomorrowBooking.setTableNumber("T002");
            tomorrowBooking.setStatus(BookingStatus.PENDING);
            tomorrowBooking.setCost(new java.math.BigDecimal("100"));
            tomorrowBooking.setRemarks("测试课程-明日");
            testBookings.add(tomorrowBooking);
            
            // 创建本周的其他测试课程
            for (int i = 1; i <= 3; i++) {
                LocalDateTime futureDate = today.plusDays(i + 1).withHour(14 + i).withMinute(0);
                CourseBooking futureBooking = new CourseBooking();
                futureBooking.setCoach(coach);
                futureBooking.setStudent(testStudent);
                futureBooking.setStartTime(futureDate);
                futureBooking.setEndTime(futureDate.plusHours(1));
                futureBooking.setTableNumber("T00" + (i + 2));
                futureBooking.setStatus(i % 2 == 0 ? BookingStatus.CONFIRMED : BookingStatus.PENDING);
                futureBooking.setCost(new java.math.BigDecimal("100"));
                futureBooking.setRemarks("测试课程-第" + (i + 2) + "天");
                testBookings.add(futureBooking);
            }
            
            // 保存所有测试数据
            courseBookingRepository.saveAll(testBookings);
            
            System.out.println("创建了 " + testBookings.size() + " 个测试课程");
            
            Map<String, Object> result = new HashMap<>();
            result.put("message", "测试课程数据创建成功");
            result.put("createdCount", testBookings.size());
            result.put("testStudent", testStudent.getRealName());
            result.put("courses", testBookings.stream().map(booking -> {
                Map<String, Object> courseInfo = new HashMap<>();
                courseInfo.put("startTime", booking.getStartTime());
                courseInfo.put("endTime", booking.getEndTime());
                courseInfo.put("status", booking.getStatus());
                courseInfo.put("tableNumber", booking.getTableNumber());
                return courseInfo;
            }).collect(java.util.stream.Collectors.toList()));
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            System.err.println("创建测试数据失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("创建测试数据失败: " + e.getMessage());
        }
    }

    @GetMapping("/debug/create-test-data")
    public ApiResponse<?> createTestDataGet(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return createTestData(userDetails);
    }
} 
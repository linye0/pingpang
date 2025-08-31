package com.pingpang.training.service;

import com.pingpang.training.dto.BookingRequest;
import com.pingpang.training.entity.CourseBooking;
import com.pingpang.training.entity.Student;
import com.pingpang.training.entity.Coach;
import com.pingpang.training.entity.CoachStudentRelation;
import com.pingpang.training.enums.BookingStatus;
import com.pingpang.training.enums.ApprovalStatus;
import com.pingpang.training.enums.CoachLevel;
import com.pingpang.training.repository.CoachRepository;
import com.pingpang.training.repository.CourseBookingRepository;
import com.pingpang.training.repository.CoachStudentRelationRepository;
import com.pingpang.training.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    @Autowired
    private CourseBookingRepository courseBookingRepository;

    @Autowired
    private CoachRepository coachRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CoachStudentRelationRepository coachStudentRelationRepository;
    
    @Autowired
    private NotificationService notificationService;

    // 获取教练课表
    @Transactional(readOnly = true)
    public List<CourseBooking> getCoachSchedule(Long coachId, LocalDate date) {
        LocalDateTime startOfWeek = date.atStartOfDay();
        LocalDateTime endOfWeek = startOfWeek.plusDays(7);
        
        return courseBookingRepository.findByCoachIdAndTimeRange(coachId, startOfWeek, endOfWeek);
    }

    // 获取可用球台
    @Transactional(readOnly = true)
    public List<String> getAvailableTables(LocalDateTime startTime, LocalDateTime endTime) {
        List<String> allTables = Arrays.asList("T001", "T002", "T003", "T004", "T005", 
            "T006", "T007", "T008", "T009", "T010");
        
        List<String> occupiedTables = courseBookingRepository
            .findOccupiedTables(startTime, endTime);
        
        return allTables.stream()
            .filter(table -> !occupiedTables.contains(table))
            .collect(Collectors.toList());
    }

    // 创建课程预约 - 改进并发控制和事务管理
    @Transactional(rollbackFor = Exception.class)
    public CourseBooking createBooking(BookingRequest request, Student student) {
        try {
            // 验证时间
            LocalDateTime startTime = request.getStartTime();
            LocalDateTime endTime = request.getEndTime();
            
            if (startTime.isBefore(LocalDateTime.now().plusHours(1))) {
                throw new IllegalArgumentException("预约时间至少需要提前1小时");
            }
            
            if (endTime.isBefore(startTime)) {
                throw new IllegalArgumentException("结束时间不能早于开始时间");
            }
            
            // 获取教练信息
            Coach coach = coachRepository.findById(request.getCoachId())
                .orElseThrow(() -> new IllegalArgumentException("教练不存在"));
            
            // *** 关键验证：检查师生双选关系是否已确认 ***
            List<CoachStudentRelation> relations = coachStudentRelationRepository
                .findByStudentIdAndCoachIdAndStatus(student.getId(), coach.getId(), ApprovalStatus.APPROVED);
            if (relations.isEmpty()) {
                throw new IllegalArgumentException("您尚未与该教练建立师生关系，请先申请选择该教练并等待确认");
            }
            
            // 计算费用
            long hours = java.time.Duration.between(startTime, endTime).toHours();
            if (hours == 0) hours = 1; // 最少按1小时计算
            
            BigDecimal hourlyRate = getCoachHourlyRate(coach.getLevel());
            BigDecimal totalCost = hourlyRate.multiply(BigDecimal.valueOf(hours));
            
            // 检查学员余额
            if (student.getAccountBalance().compareTo(totalCost) < 0) {
                throw new IllegalArgumentException("账户余额不足，请先充值");
            }
            
            // 选择球台 - 在事务中进行更严格的检查
            String tableNumber = request.getTableNumber();
            if (tableNumber == null || tableNumber.trim().isEmpty()) {
                List<String> availableTables = getAvailableTables(startTime, endTime);
                if (availableTables.isEmpty()) {
                    throw new IllegalArgumentException("该时间段没有可用球台");
                }
                tableNumber = availableTables.get(0); // 自动分配第一个可用球台
            }
            
            // *** 关键改进：在事务中再次检查球台冲突，包括指定球台的冲突检查 ***
            List<CourseBooking> tableConflicts = courseBookingRepository.findConflictingBookingsWithTable(
                tableNumber, startTime, endTime);
            if (!tableConflicts.isEmpty()) {
                throw new IllegalArgumentException("指定球台不可用");
            }
            
            // 检查教练时间冲突
            List<CourseBooking> coachConflicts = courseBookingRepository.findConflictingBookings(
                coach.getId(), startTime, endTime);
            if (!coachConflicts.isEmpty()) {
                throw new IllegalArgumentException("该教练在此时间段已有其他课程安排");
            }
            
            // 扣除费用
            student.setAccountBalance(student.getAccountBalance().subtract(totalCost));
            studentRepository.save(student);
            
            // 创建预约
            CourseBooking booking = new CourseBooking();
            booking.setStudent(student);
            booking.setCoach(coach);
            booking.setStartTime(startTime);
            booking.setEndTime(endTime);
            booking.setTableNumber(tableNumber);
            booking.setCost(totalCost);
            booking.setStatus(BookingStatus.PENDING);
            booking.setNotes(request.getRemarks() != null ? request.getRemarks() : "");
            
            // 保存预约
            CourseBooking savedBooking = courseBookingRepository.save(booking);
            
            // 再次验证保存后的数据一致性
            List<CourseBooking> finalCheck = courseBookingRepository.findConflictingBookingsWithTable(
                tableNumber, startTime, endTime);
            if (finalCheck.size() > 1) {
                // 如果发现多个冲突的预约，说明存在并发问题，回滚当前事务
                throw new IllegalStateException("检测到并发冲突，请重新尝试预约");
            }
            
            // 发送预约成功通知
            notificationService.sendBookingConfirmationNotification(savedBooking);
            
            return savedBooking;
            
        } catch (Exception e) {
            // 确保事务回滚
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    // 取消预约
    public CourseBooking cancelBooking(CourseBooking booking, String reason, Student student) {
        // 更新取消次数
        LocalDateTime resetDate = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
        if (student.getLastCancellationReset() == null || 
            !student.getLastCancellationReset().equals(resetDate)) {
            student.setCancellationCount(0);
            student.setLastCancellationReset(resetDate);
        }
        
        student.setCancellationCount(student.getCancellationCount() + 1);
        studentRepository.save(student);
        
        // 退款
        student.setAccountBalance(student.getAccountBalance().add(booking.getCost()));
        studentRepository.save(student);
        
        // 更新预约状态
        booking.setStatus(BookingStatus.CANCELLED);
        if (reason != null && !reason.trim().isEmpty()) {
            String existingNotes = booking.getNotes();
            existingNotes = existingNotes != null ? existingNotes : "";
            booking.setNotes(existingNotes + "\n取消原因: " + reason);
        }
        
        CourseBooking cancelledBooking = courseBookingRepository.save(booking);
        
        // 发送取消通知
        String cancellationReason = reason != null && !reason.trim().isEmpty() ? reason : "学员取消";
        notificationService.sendCancellationNotification(cancelledBooking, cancellationReason);
        
        return cancelledBooking;
    }

    // 根据教练等级获取时薪
    private BigDecimal getCoachHourlyRate(CoachLevel level) {
        switch (level) {
            case SENIOR:
                return BigDecimal.valueOf(200);
            case INTERMEDIATE:
                return BigDecimal.valueOf(150);
            case JUNIOR:
                return BigDecimal.valueOf(80);
            default:
                return BigDecimal.valueOf(80);
        }
    }

    // 检查时间冲突
    public boolean hasTimeConflict(Long coachId, LocalDateTime startTime, LocalDateTime endTime) {
        List<CourseBooking> conflicts = courseBookingRepository
            .findConflictingBookings(coachId, startTime, endTime);
        return !conflicts.isEmpty();
    }

    // 获取教练未来一周的空闲时间段
    public List<String> getCoachFreeTimeSlots(Long coachId, LocalDate date) {
        LocalDateTime startOfWeek = date.atStartOfDay();
        LocalDateTime endOfWeek = startOfWeek.plusDays(7);
        
        List<CourseBooking> bookings = courseBookingRepository
            .findByCoachIdAndTimeRange(coachId, startOfWeek, endOfWeek);
        
        // 这里应该计算空闲时间段，简化处理
        // 实际实现中需要根据营业时间和已预约时间计算空闲时段
        return Arrays.asList("09:00-10:00", "10:00-11:00", "14:00-15:00", "15:00-16:00");
    }
}
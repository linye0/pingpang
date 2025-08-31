package com.pingpang.training.repository;

import com.pingpang.training.entity.CourseBooking;
import com.pingpang.training.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CourseBookingRepository extends JpaRepository<CourseBooking, Long> {
    
    @Query("SELECT b FROM CourseBooking b JOIN FETCH b.student JOIN FETCH b.coach WHERE b.coach.id = :coachId AND b.startTime >= :startTime AND b.startTime <= :endTime AND b.status IN ('PENDING', 'CONFIRMED') ORDER BY b.startTime ASC")
    List<CourseBooking> findByCoachIdAndTimeRange(@Param("coachId") Long coachId, 
                                                  @Param("startTime") LocalDateTime startTime, 
                                                  @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT b FROM CourseBooking b JOIN FETCH b.student JOIN FETCH b.coach WHERE b.student.id = :studentId AND b.status IN ('PENDING', 'CONFIRMED') ORDER BY b.startTime ASC")
    List<CourseBooking> findActiveByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT b FROM CourseBooking b JOIN FETCH b.student JOIN FETCH b.coach WHERE b.coach.id = :coachId AND b.status = :status ORDER BY b.startTime ASC")
    List<CourseBooking> findByCoachIdAndStatus(@Param("coachId") Long coachId, @Param("status") BookingStatus status);
    
    @Query("SELECT DISTINCT b.tableNumber FROM CourseBooking b WHERE " +
           "((b.startTime <= :startTime AND b.endTime > :startTime) OR " +
           "(b.startTime < :endTime AND b.endTime >= :endTime) OR " +
           "(b.startTime >= :startTime AND b.endTime <= :endTime)) AND " +
           "b.status IN ('PENDING', 'CONFIRMED')")
    List<String> findOccupiedTables(@Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT b FROM CourseBooking b WHERE b.status = 'CONFIRMED' AND b.startTime <= :reminderTime AND b.startTime > :now")
    List<CourseBooking> findBookingsForReminder(@Param("reminderTime") LocalDateTime reminderTime, @Param("now") LocalDateTime now);
    
    // 新增的方法用于Dashboard - 使用原生方法名
    List<CourseBooking> findByStudentIdOrCoachId(Long studentId, Long coachId);
    
    // 添加更多查询方法
    @Query("SELECT b FROM CourseBooking b JOIN FETCH b.student JOIN FETCH b.coach WHERE b.coach.id = :coachId ORDER BY b.startTime DESC")
    List<CourseBooking> findByCoachId(@Param("coachId") Long coachId);
    
    @Query("SELECT b FROM CourseBooking b JOIN FETCH b.student JOIN FETCH b.coach WHERE b.student.id = :studentId ORDER BY b.startTime DESC")
    List<CourseBooking> findByStudentId(@Param("studentId") Long studentId);
    
    // 统计方法
    Long countByCoachId(Long coachId);
    
    Long countByStudentId(Long studentId);
    
    Long countByCoachIdAndStatus(Long coachId, BookingStatus status);
    
    Long countByStudentIdAndStatus(Long studentId, BookingStatus status);
    
    @Query("SELECT COUNT(b) FROM CourseBooking b WHERE b.coach.id = :coachId AND DATE(b.startTime) = DATE(:date)")
    Long countByCoachIdAndDate(@Param("coachId") Long coachId, @Param("date") LocalDateTime date);
    
    @Query("SELECT COUNT(b) FROM CourseBooking b WHERE b.student.id = :studentId AND DATE(b.startTime) = DATE(:date)")
    Long countByStudentIdAndDate(@Param("studentId") Long studentId, @Param("date") LocalDateTime date);
    
    // 按校区查询方法
    @Query("SELECT b FROM CourseBooking b WHERE b.student.campus.id = :campusId OR b.coach.campus.id = :campusId")
    List<CourseBooking> findByCampusId(@Param("campusId") Long campusId);
    
    @Query("SELECT b FROM CourseBooking b WHERE (b.student.campus.id = :campusId OR b.coach.campus.id = :campusId) AND b.status = :status")
    List<CourseBooking> findByCampusIdAndStatus(@Param("campusId") Long campusId, @Param("status") BookingStatus status);
    
    @Query("SELECT b FROM CourseBooking b WHERE (b.student.campus.id = :campusId OR b.coach.campus.id = :campusId) AND b.startTime BETWEEN :startTime AND :endTime")
    List<CourseBooking> findByCampusIdAndTimeRange(@Param("campusId") Long campusId, 
                                                  @Param("startTime") LocalDateTime startTime, 
                                                  @Param("endTime") LocalDateTime endTime);
    
    // 校区统计方法
    @Query("SELECT COUNT(b) FROM CourseBooking b WHERE b.student.campus.id = :campusId OR b.coach.campus.id = :campusId")
    Long countByCampusId(@Param("campusId") Long campusId);
    
    @Query("SELECT COUNT(b) FROM CourseBooking b WHERE (b.student.campus.id = :campusId OR b.coach.campus.id = :campusId) AND b.status = :status")
    Long countByCampusIdAndStatus(@Param("campusId") Long campusId, @Param("status") BookingStatus status);
    
    // 查找时间冲突的预约
    @Query("SELECT b FROM CourseBooking b WHERE b.coach.id = :coachId AND " +
           "((b.startTime <= :startTime AND b.endTime > :startTime) OR " +
           "(b.startTime < :endTime AND b.endTime >= :endTime) OR " +
           "(b.startTime >= :startTime AND b.endTime <= :endTime)) AND " +
           "b.status IN ('PENDING', 'CONFIRMED')")
    List<CourseBooking> findConflictingBookings(@Param("coachId") Long coachId,
                                               @Param("startTime") LocalDateTime startTime,
                                               @Param("endTime") LocalDateTime endTime);
    
    // 查找球台和时间冲突的预约
    @Query("SELECT b FROM CourseBooking b WHERE b.tableNumber = :tableNumber AND " +
           "((b.startTime <= :startTime AND b.endTime > :startTime) OR " +
           "(b.startTime < :endTime AND b.endTime >= :endTime) OR " +
           "(b.startTime >= :startTime AND b.endTime <= :endTime)) AND " +
           "b.status IN ('PENDING', 'CONFIRMED')")
    List<CourseBooking> findConflictingBookingsWithTable(@Param("tableNumber") String tableNumber,
                                                         @Param("startTime") LocalDateTime startTime,
                                                         @Param("endTime") LocalDateTime endTime);
    
    // 查找特定教练和学员的课程安排
    @Query("SELECT b FROM CourseBooking b JOIN FETCH b.student JOIN FETCH b.coach WHERE b.coach.id = :coachId AND b.student.id = :studentId ORDER BY b.startTime DESC")
    List<CourseBooking> findByCoachIdAndStudentIdOrderByStartTimeDesc(@Param("coachId") Long coachId, @Param("studentId") Long studentId);
    
    // 根据学员ID和状态查找预约
    List<CourseBooking> findByStudentIdAndStatus(Long studentId, BookingStatus status);
    
    // 通知服务需要的方法
    @Query("SELECT b FROM CourseBooking b WHERE b.startTime BETWEEN :startTime AND :endTime AND b.status = :status")
    List<CourseBooking> findByStartTimeBetweenAndStatus(@Param("startTime") LocalDateTime startTime, 
                                                       @Param("endTime") LocalDateTime endTime, 
                                                       @Param("status") BookingStatus status);
} 
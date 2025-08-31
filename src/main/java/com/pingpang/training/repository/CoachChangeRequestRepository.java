package com.pingpang.training.repository;

import com.pingpang.training.entity.CoachChangeRequest;
import com.pingpang.training.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CoachChangeRequestRepository extends JpaRepository<CoachChangeRequest, Long> {
    
    List<CoachChangeRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    
    List<CoachChangeRequest> findByCurrentCoachIdOrderByCreatedAtDesc(Long currentCoachId);
    
    List<CoachChangeRequest> findByNewCoachIdOrderByCreatedAtDesc(Long newCoachId);
    
    List<CoachChangeRequest> findByCampusAdminIdOrderByCreatedAtDesc(Long campusAdminId);
    
    List<CoachChangeRequest> findByFinalStatusOrderByCreatedAtDesc(ApprovalStatus finalStatus);
    
    @Query("SELECT r FROM CoachChangeRequest r WHERE r.student.campus.id = :campusId ORDER BY r.createdAt DESC")
    List<CoachChangeRequest> findByCampusIdOrderByCreatedAtDesc(@Param("campusId") Long campusId);
    
    @Query("SELECT r FROM CoachChangeRequest r WHERE r.currentCoach.id = :coachId AND r.currentCoachApproval = :status ORDER BY r.createdAt DESC")
    List<CoachChangeRequest> findByCurrentCoachIdAndCurrentCoachApproval(
        @Param("coachId") Long coachId, 
        @Param("status") ApprovalStatus status);
    
    @Query("SELECT r FROM CoachChangeRequest r WHERE r.newCoach.id = :coachId AND r.newCoachApproval = :status ORDER BY r.createdAt DESC")
    List<CoachChangeRequest> findByNewCoachIdAndNewCoachApproval(
        @Param("coachId") Long coachId, 
        @Param("status") ApprovalStatus status);
    
    @Query("SELECT r FROM CoachChangeRequest r WHERE r.campusAdmin.id = :adminId AND r.adminApproval = :status ORDER BY r.createdAt DESC")
    List<CoachChangeRequest> findByCampusAdminIdAndAdminApproval(
        @Param("adminId") Long adminId, 
        @Param("status") ApprovalStatus status);
    
    @Query("SELECT r FROM CoachChangeRequest r WHERE r.student.id = :studentId AND r.finalStatus = 'PENDING' ORDER BY r.createdAt DESC")
    List<CoachChangeRequest> findPendingRequestsByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT COUNT(r) FROM CoachChangeRequest r WHERE r.student.id = :studentId AND r.createdAt >= :startTime")
    Long countByStudentIdAndCreatedAtAfter(@Param("studentId") Long studentId, @Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT r FROM CoachChangeRequest r WHERE " +
           "(r.currentCoach.id = :coachId AND r.currentCoachApproval = 'PENDING') OR " +
           "(r.newCoach.id = :coachId AND r.newCoachApproval = 'PENDING') " +
           "ORDER BY r.createdAt DESC")
    List<CoachChangeRequest> findPendingRequestsByCoachId(@Param("coachId") Long coachId);
    
    @Query("SELECT r FROM CoachChangeRequest r WHERE r.student.campus.id = :campusId AND r.adminApproval = 'PENDING' ORDER BY r.createdAt DESC")
    List<CoachChangeRequest> findPendingRequestsByCampusId(@Param("campusId") Long campusId);
} 
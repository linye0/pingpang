package com.pingpang.training.repository;

import com.pingpang.training.entity.CoachStudentRelation;
import com.pingpang.training.enums.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoachStudentRelationRepository extends JpaRepository<CoachStudentRelation, Long> {
    
    @Query("SELECT r FROM CoachStudentRelation r WHERE r.student.id = :studentId AND r.status = 'APPROVED'")
    List<CoachStudentRelation> findApprovedByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT r FROM CoachStudentRelation r WHERE r.coach.id = :coachId AND r.status = 'APPROVED'")
    List<CoachStudentRelation> findApprovedByCoachId(@Param("coachId") Long coachId);
    
    @Query("SELECT r FROM CoachStudentRelation r WHERE r.coach.id = :coachId AND r.status = :status")
    List<CoachStudentRelation> findByCoachIdAndStatus(@Param("coachId") Long coachId, @Param("status") ApprovalStatus status);
    
    @Query("SELECT r FROM CoachStudentRelation r WHERE r.student.id = :studentId AND r.coach.id = :coachId")
    Optional<CoachStudentRelation> findByStudentIdAndCoachId(@Param("studentId") Long studentId, @Param("coachId") Long coachId);
    
    @Query("SELECT COUNT(r) FROM CoachStudentRelation r WHERE r.student.id = :studentId AND r.status = 'APPROVED'")
    Long countApprovedByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT COUNT(r) FROM CoachStudentRelation r WHERE r.coach.id = :coachId AND r.status = 'APPROVED'")
    Long countApprovedByCoachId(@Param("coachId") Long coachId);
    
    // 新增的方法用于Dashboard - 使用原生方法名
    List<CoachStudentRelation> findByStudentId(Long studentId);
    
    List<CoachStudentRelation> findByCoachIdAndStatus(Long coachId, String status);
    
    // 添加统计方法
    Long countByCoachIdAndStatus(Long coachId, ApprovalStatus status);
    
    Long countByStudentIdAndStatus(Long studentId, ApprovalStatus status);
    
    List<CoachStudentRelation> findByCoachId(Long coachId);
    
    List<CoachStudentRelation> findByStudentIdAndStatus(Long studentId, ApprovalStatus status);
    
    // 检查关系是否存在
    boolean existsByCoachIdAndStudentId(Long coachId, Long studentId);
    
    boolean existsByCoachIdAndStudentIdAndStatus(Long coachId, Long studentId, ApprovalStatus status);
    
    Optional<CoachStudentRelation> findByCoachIdAndStudentIdAndStatus(Long coachId, Long studentId, ApprovalStatus status);
    
    // 用于课程预约验证
    @Query("SELECT r FROM CoachStudentRelation r WHERE r.student.id = :studentId AND r.coach.id = :coachId AND r.status = :status")
    List<CoachStudentRelation> findByStudentIdAndCoachIdAndStatus(@Param("studentId") Long studentId, @Param("coachId") Long coachId, @Param("status") ApprovalStatus status);
} 
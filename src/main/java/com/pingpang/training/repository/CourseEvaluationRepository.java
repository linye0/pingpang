package com.pingpang.training.repository;

import com.pingpang.training.entity.CourseEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseEvaluationRepository extends JpaRepository<CourseEvaluation, Long> {
    
    Optional<CourseEvaluation> findByBookingId(Long bookingId);
    
    @Query("SELECT e FROM CourseEvaluation e WHERE e.booking.student.id = :studentId")
    List<CourseEvaluation> findByStudentId(@Param("studentId") Long studentId);
    
    @Query("SELECT e FROM CourseEvaluation e WHERE e.booking.coach.id = :coachId")
    List<CourseEvaluation> findByCoachId(@Param("coachId") Long coachId);
    
    @Query("SELECT e FROM CourseEvaluation e WHERE e.booking.coach.id = :coachId AND e.coachEvaluation IS NULL")
    List<CourseEvaluation> findPendingCoachEvaluations(@Param("coachId") Long coachId);
    
    @Query("SELECT e FROM CourseEvaluation e WHERE e.booking.student.id = :studentId AND e.studentEvaluation IS NULL")
    List<CourseEvaluation> findPendingStudentEvaluations(@Param("studentId") Long studentId);
    
    boolean existsByBookingId(Long bookingId);
    
    @Query("SELECT COUNT(e) FROM CourseEvaluation e WHERE e.booking.coach.id = :coachId AND e.studentEvaluation IS NOT NULL")
    Long countStudentEvaluationsByCoachId(@Param("coachId") Long coachId);
    
    @Query("SELECT COUNT(e) FROM CourseEvaluation e WHERE e.booking.student.id = :studentId AND e.coachEvaluation IS NOT NULL")
    Long countCoachEvaluationsByStudentId(@Param("studentId") Long studentId);
    
    // 查找特定教练和学员的评价记录
    @Query("SELECT e FROM CourseEvaluation e WHERE e.booking.coach.id = :coachId AND e.booking.student.id = :studentId ORDER BY e.updatedAt DESC")
    List<CourseEvaluation> findByCoachIdAndStudentId(@Param("coachId") Long coachId, @Param("studentId") Long studentId);
} 
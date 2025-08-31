package com.pingpang.training.repository;

import com.pingpang.training.entity.PaymentRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRecordRepository extends JpaRepository<PaymentRecord, Long> {
    
    List<PaymentRecord> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    
    @Query("SELECT SUM(pr.amount) FROM PaymentRecord pr WHERE pr.student.id = :studentId")
    BigDecimal sumAmountByStudentId(@Param("studentId") Long studentId);
    
    List<PaymentRecord> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    List<PaymentRecord> findByStudentId(Long studentId);
    
    @Query("SELECT pr FROM PaymentRecord pr WHERE pr.student.campus.id = :campusId ORDER BY pr.createdAt DESC")
    List<PaymentRecord> findByCampusId(@Param("campusId") Long campusId);
    
    @Query("SELECT SUM(pr.amount) FROM PaymentRecord pr WHERE pr.student.campus.id = :campusId AND pr.amount > 0")
    BigDecimal getTotalRevenueByCampusId(@Param("campusId") Long campusId);
    
    @Query("SELECT COUNT(pr) FROM PaymentRecord pr WHERE pr.student.campus.id = :campusId")
    Long countByCampusId(@Param("campusId") Long campusId);
    
    long countByStudentId(Long studentId);
} 
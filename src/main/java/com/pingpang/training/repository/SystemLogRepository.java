package com.pingpang.training.repository;

import com.pingpang.training.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {
    
    @Query("SELECT sl FROM SystemLog sl WHERE sl.user.campus.id = :campusId ORDER BY sl.createdAt DESC")
    List<SystemLog> findByCampusIdOrderByCreatedAtDesc(@Param("campusId") Long campusId);
    
    @Query("SELECT sl FROM SystemLog sl WHERE sl.user.campus.id = :campusId ORDER BY sl.createdAt DESC")
    Page<SystemLog> findByCampusIdOrderByCreatedAtDesc(@Param("campusId") Long campusId, Pageable pageable);
    
    List<SystemLog> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<SystemLog> findByOperationTypeOrderByCreatedAtDesc(String operationType);
    
    List<SystemLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startTime, LocalDateTime endTime);
    
    void deleteByCreatedAtBefore(LocalDateTime beforeTime);
    
    @Query("SELECT COUNT(sl) FROM SystemLog sl WHERE sl.user.campus.id = :campusId")
    long countByCampusId(@Param("campusId") Long campusId);
} 
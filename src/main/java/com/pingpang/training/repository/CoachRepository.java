package com.pingpang.training.repository;

import com.pingpang.training.entity.Coach;
import com.pingpang.training.enums.ApprovalStatus;
import com.pingpang.training.enums.Gender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoachRepository extends JpaRepository<Coach, Long> {
    
    @Query("SELECT c FROM Coach c WHERE c.campus.id = :campusId AND c.active = true AND c.approvalStatus IN ('APPROVED', 'PENDING')")
    List<Coach> findApprovedByCampusId(@Param("campusId") Long campusId);
    
    @Query("SELECT c FROM Coach c WHERE c.campus.id = :campusId AND c.active = true AND c.approvalStatus = :status")
    List<Coach> findByCampusIdAndApprovalStatus(@Param("campusId") Long campusId, @Param("status") ApprovalStatus status);
    
    @Query("SELECT c FROM Coach c WHERE c.approvalStatus = :status")
    List<Coach> findByApprovalStatus(@Param("status") ApprovalStatus status);
    
    @Query("SELECT c FROM Coach c WHERE c.campus.id = :campusId")
    List<Coach> findByCampusId(@Param("campusId") Long campusId);
    
    @Query("SELECT c FROM Coach c WHERE c.active = true AND c.approvalStatus = 'APPROVED' " +
           "AND (:name IS NULL OR c.realName LIKE %:name%) " +
           "AND (:gender IS NULL OR CAST(c.gender AS string) = :gender) " +
           "AND (:age IS NULL OR c.age = :age) " +
           "AND c.campus.id = :campusId")
    List<Coach> searchCoaches(@Param("campusId") Long campusId, 
                             @Param("name") String name, 
                             @Param("gender") String gender, 
                             @Param("age") Integer age);
                             
    // 统计方法
    @Query("SELECT COUNT(c) FROM Coach c WHERE c.campus.id = :campusId")
    Long countByCampusId(@Param("campusId") Long campusId);
    
    @Query("SELECT COUNT(c) FROM Coach c WHERE c.campus.id = :campusId AND c.approvalStatus = :status")
    Long countByCampusIdAndApprovalStatus(@Param("campusId") Long campusId, @Param("status") ApprovalStatus status);
} 
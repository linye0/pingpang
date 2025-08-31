package com.pingpang.training.repository;

import com.pingpang.training.entity.Campus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CampusRepository extends JpaRepository<Campus, Long> {
    
    List<Campus> findByActiveTrue();
    
    Optional<Campus> findByIsMainCampusTrue();
    
    boolean existsByName(String name);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.campus.id = :campusId")
    Long countUsersByCampusId(@Param("campusId") Long campusId);
    
    @Query("SELECT c FROM Campus c WHERE c.admin.id = :adminId")
    Optional<Campus> findByAdminId(@Param("adminId") Long adminId);
} 
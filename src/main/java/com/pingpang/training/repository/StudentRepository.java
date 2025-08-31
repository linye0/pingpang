package com.pingpang.training.repository;

import com.pingpang.training.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    
    @Query("SELECT s FROM Student s WHERE s.campus.id = :campusId AND s.active = true")
    List<Student> findActiveByCampusId(@Param("campusId") Long campusId);
    
    @Query("SELECT s FROM Student s WHERE s.realName LIKE %:name% AND s.active = true")
    List<Student> findByRealNameContaining(@Param("name") String name);
    
    // 按校区查询
    @Query("SELECT s FROM Student s WHERE s.campus.id = :campusId")
    List<Student> findByCampusId(@Param("campusId") Long campusId);
    
    @Query("SELECT s FROM Student s WHERE s.campus.id = :campusId AND s.active = true")
    List<Student> findByCampusIdAndActiveTrue(@Param("campusId") Long campusId);
    
    // 统计方法
    @Query("SELECT COUNT(s) FROM Student s WHERE s.campus.id = :campusId")
    Long countByCampusId(@Param("campusId") Long campusId);
    
    @Query("SELECT COUNT(s) FROM Student s WHERE s.campus.id = :campusId AND s.active = true")
    Long countByCampusIdAndActiveTrue(@Param("campusId") Long campusId);
    
    // 已经有内置的 findById(Long id) 方法，无需重复定义
} 
package com.pingpang.training.repository;

import com.pingpang.training.entity.User;
import com.pingpang.training.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUsername(String username);
    
    Optional<User> findByPhone(String phone);
    
    Boolean existsByUsername(String username);
    
    Boolean existsByPhone(String phone);
    
    List<User> findByRoleAndActiveTrue(UserRole role);
    
    List<User> findByCampusIdAndActiveTrue(Long campusId);
    
    List<User> findByCampusId(Long campusId);
    
    List<User> findByCampusIdAndRoleAndActiveTrue(Long campusId, UserRole role);
    
    @Query("SELECT u FROM User u WHERE u.campus.id = :campusId AND u.role = :role AND u.active = true")
    List<User> findActiveByCampusAndRole(@Param("campusId") Long campusId, @Param("role") UserRole role);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    Long countByRole(@Param("role") UserRole role);

    @Query("SELECT u FROM User u WHERE u.role = :role")
    List<User> findByRole(@Param("role") UserRole role);
    
    @Query("SELECT u FROM User u WHERE u.active = true")
    List<User> findEnabledUsers();

    @Query("SELECT u FROM User u WHERE u.active = false")
    List<User> findDisabledUsers();
    
    // 新增方法支持超级管理员功能
    List<User> findByRoleInAndActiveTrue(List<UserRole> roles);
    
    @Query("SELECT u FROM User u WHERE u.phone = :phone AND u.active = true")
    Optional<User> findByPhoneAndActiveTrue(@Param("phone") String phone);
    
    @Query("SELECT u FROM User u WHERE u.realName LIKE %:name% AND u.active = true")
    List<User> findByRealNameContainingAndActiveTrue(@Param("name") String name);
    
    @Query("SELECT u FROM User u WHERE u.role = :role AND u.campus.id = :campusId")
    List<User> findByRoleAndCampusId(@Param("role") UserRole role, @Param("campusId") Long campusId);
} 
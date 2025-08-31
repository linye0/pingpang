package com.pingpang.training.service;

import com.pingpang.training.entity.SystemLog;
import com.pingpang.training.entity.User;
import com.pingpang.training.repository.SystemLogRepository;
import com.pingpang.training.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SystemLogService {

    @Autowired
    private SystemLogRepository systemLogRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = Exception.class)
    public void log(User user, String operationType, String description) {
        try {
            if (user == null || user.getId() == null) {
                System.err.println("Warning: Cannot log system operation - user is null or has no ID");
                return;
            }
            
            // 为确保外键约束，重新从数据库获取用户对象
            User managedUser = userRepository.findById(user.getId()).orElse(null);
            if (managedUser == null) {
                System.err.println("Warning: Cannot log system operation - user not found in database: " + user.getId());
                return;
            }
            
            SystemLog log = new SystemLog();
            log.setUser(managedUser);
            log.setUserId(managedUser.getId()); // 设置备份用户ID
            log.setOperationType(operationType);
            log.setDescription(description);
            log.setCreatedAt(LocalDateTime.now());
            
            try {
                systemLogRepository.save(log);
            } catch (Exception fkException) {
                // 如果外键约束失败，尝试只保存备份信息
                System.err.println("Foreign key constraint error, saving log without user relationship: " + fkException.getMessage());
                SystemLog fallbackLog = new SystemLog();
                fallbackLog.setUser(null); // 清空外键关系
                fallbackLog.setUserId(managedUser.getId()); // 保留备份用户ID
                fallbackLog.setOperationType(operationType);
                fallbackLog.setDescription(description + " [User ID: " + managedUser.getId() + "]");
                fallbackLog.setCreatedAt(LocalDateTime.now());
                
                systemLogRepository.save(fallbackLog);
            }
        } catch (Exception e) {
            System.err.println("Error logging system operation: " + e.getMessage());
            // Don't throw exception to avoid breaking the main operation
        }
    }

    public List<SystemLog> getLogsByCampus(Long campusId) {
        if (campusId == null) {
            // SUPER_ADMIN 查看所有系统日志
            return systemLogRepository.findAll(Sort.by("createdAt").descending());
        } else {
            // CAMPUS_ADMIN 查看指定校区的系统日志
            return systemLogRepository.findByCampusIdOrderByCreatedAtDesc(campusId);
        }
    }

    public Page<SystemLog> getLogsByCampus(Long campusId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return systemLogRepository.findByCampusIdOrderByCreatedAtDesc(campusId, pageable);
    }

    public List<SystemLog> getLogsByUser(Long userId) {
        return systemLogRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<SystemLog> getLogsByOperationType(String operationType) {
        return systemLogRepository.findByOperationTypeOrderByCreatedAtDesc(operationType);
    }

    public List<SystemLog> getLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        return systemLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startTime, endTime);
    }

    public void deleteOldLogs(LocalDateTime beforeTime) {
        systemLogRepository.deleteByCreatedAtBefore(beforeTime);
    }

    public long countLogsByCampus(Long campusId) {
        return systemLogRepository.countByCampusId(campusId);
    }

    public List<SystemLog> getAllLogs() {
        return systemLogRepository.findAll(Sort.by("createdAt").descending());
    }
} 
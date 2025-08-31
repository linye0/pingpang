package com.pingpang.training.repository;

import com.pingpang.training.entity.SystemMessage;
import com.pingpang.training.enums.MessageType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemMessageRepository extends JpaRepository<SystemMessage, Long> {
    
    List<SystemMessage> findByReceiverIdOrderByCreatedAtDesc(Long receiverId);
    
    Page<SystemMessage> findByReceiverIdOrderByCreatedAtDesc(Long receiverId, Pageable pageable);
    
    List<SystemMessage> findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(Long receiverId);
    
    Long countByReceiverIdAndIsReadFalse(Long receiverId);
    
    List<SystemMessage> findBySenderIdOrderByCreatedAtDesc(Long senderId);
    
    List<SystemMessage> findByMessageTypeOrderByCreatedAtDesc(MessageType messageType);
    
    @Query("SELECT m FROM SystemMessage m WHERE m.receiver.id = :receiverId AND m.messageType = :messageType ORDER BY m.createdAt DESC")
    List<SystemMessage> findByReceiverIdAndMessageTypeOrderByCreatedAtDesc(
        @Param("receiverId") Long receiverId, 
        @Param("messageType") MessageType messageType);
    
    @Query("SELECT m FROM SystemMessage m WHERE m.receiver.id = :receiverId AND m.createdAt >= :startTime ORDER BY m.createdAt DESC")
    List<SystemMessage> findRecentMessagesByReceiverId(
        @Param("receiverId") Long receiverId, 
        @Param("startTime") LocalDateTime startTime);
    
    @Query("SELECT m FROM SystemMessage m WHERE m.receiver.campus.id = :campusId ORDER BY m.createdAt DESC")
    List<SystemMessage> findByCampusIdOrderByCreatedAtDesc(@Param("campusId") Long campusId);
    
    @Query("SELECT COUNT(m) FROM SystemMessage m WHERE m.receiver.campus.id = :campusId AND m.isRead = false")
    Long countUnreadMessagesByCampusId(@Param("campusId") Long campusId);
    
    void deleteByCreatedAtBefore(LocalDateTime beforeTime);
    
    @Query("SELECT m FROM SystemMessage m WHERE m.relatedId = :relatedId AND m.messageType = :messageType ORDER BY m.createdAt DESC")
    List<SystemMessage> findByRelatedIdAndMessageType(
        @Param("relatedId") Long relatedId, 
        @Param("messageType") MessageType messageType);
    
    // 通知服务需要的方法
    boolean existsByRelatedIdAndMessageType(Long relatedId, MessageType messageType);
} 
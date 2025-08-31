package com.pingpang.training.service;

import com.pingpang.training.entity.SystemMessage;
import com.pingpang.training.entity.User;
import com.pingpang.training.enums.MessageType;
import com.pingpang.training.repository.SystemMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class SystemMessageService {

    @Autowired
    private SystemMessageRepository systemMessageRepository;

    @Autowired
    private SystemLogService systemLogService;

    /**
     * 发送系统消息
     */
    public SystemMessage sendMessage(User sender, User receiver, MessageType messageType, 
                                   String title, String content, Long relatedId) {
        SystemMessage message = new SystemMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setMessageType(messageType);
        message.setTitle(title);
        message.setContent(content);
        message.setRelatedId(relatedId);
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());

        SystemMessage savedMessage = systemMessageRepository.save(message);

        // 记录系统日志
        systemLogService.log(sender, "SEND_MESSAGE", 
            "发送消息给 " + receiver.getRealName() + ": " + title);

        return savedMessage;
    }

    /**
     * 发送系统通知（无发送者）
     */
    public SystemMessage sendNotification(User receiver, MessageType messageType, 
                                        String title, String content, Long relatedId) {
        SystemMessage message = new SystemMessage();
        message.setSender(null); // 系统通知没有发送者
        message.setReceiver(receiver);
        message.setMessageType(messageType);
        message.setTitle(title);
        message.setContent(content);
        message.setRelatedId(relatedId);
        message.setIsRead(false);
        message.setCreatedAt(LocalDateTime.now());

        return systemMessageRepository.save(message);
    }

    /**
     * 批量发送消息
     */
    public void sendBatchMessage(User sender, List<User> receivers, MessageType messageType,
                                String title, String content, Long relatedId) {
        for (User receiver : receivers) {
            sendMessage(sender, receiver, messageType, title, content, relatedId);
        }
    }

    /**
     * 获取用户的消息列表
     */
    public List<SystemMessage> getUserMessages(Long userId) {
        return systemMessageRepository.findByReceiverIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取用户的未读消息
     */
    public List<SystemMessage> getUnreadMessages(Long userId) {
        return systemMessageRepository.findByReceiverIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * 获取用户的消息分页
     */
    public Page<SystemMessage> getUserMessages(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return systemMessageRepository.findByReceiverIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * 标记消息为已读
     */
    public SystemMessage markAsRead(Long messageId, Long userId) {
        SystemMessage message = systemMessageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("消息不存在"));

        if (!message.getReceiver().getId().equals(userId)) {
            throw new RuntimeException("无权限访问该消息");
        }

        message.setIsRead(true);
        return systemMessageRepository.save(message);
    }

    /**
     * 批量标记为已读
     */
    public void markMultipleAsRead(List<Long> messageIds, Long userId) {
        List<SystemMessage> messages = systemMessageRepository.findAllById(messageIds);
        for (SystemMessage message : messages) {
            if (message.getReceiver().getId().equals(userId)) {
                message.setIsRead(true);
                systemMessageRepository.save(message);
            }
        }
    }

    /**
     * 删除消息
     */
    public void deleteMessage(Long messageId, Long userId) {
        SystemMessage message = systemMessageRepository.findById(messageId)
            .orElseThrow(() -> new RuntimeException("消息不存在"));

        if (!message.getReceiver().getId().equals(userId)) {
            throw new RuntimeException("无权限删除该消息");
        }

        systemMessageRepository.delete(message);
    }

    /**
     * 获取未读消息数量
     */
    public long getUnreadCount(Long userId) {
        return systemMessageRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    /**
     * 发送教练审核相关消息
     */
    public void sendCoachApprovalMessage(User coach, User admin, boolean approved) {
        String title = approved ? "教练审核通过" : "教练审核未通过";
        String content = approved ? 
            "恭喜！您的教练入职申请已通过审核，现在可以正常使用系统功能。" :
            "很遗憾，您的教练入职申请未通过审核，如有疑问请联系校区管理员。";

        sendMessage(admin, coach, MessageType.COACH_APPROVAL, title, content, coach.getId());
    }

    /**
     * 发送学员换教练相关消息
     */
    public void sendCoachChangeRequestMessage(User student, User currentCoach, User newCoach, User admin) {
        // 发送给当前教练
        sendMessage(student, currentCoach, MessageType.COACH_CHANGE, 
            "学员申请更换教练", 
            "学员 " + student.getRealName() + " 申请更换教练，请确认是否同意。", 
            student.getId());

        // 发送给新教练
        sendMessage(student, newCoach, MessageType.COACH_CHANGE,
            "学员申请选择您为教练",
            "学员 " + student.getRealName() + " 申请选择您为新教练，请确认是否同意。",
            student.getId());

        // 发送给管理员
        sendMessage(student, admin, MessageType.COACH_CHANGE,
            "学员申请更换教练",
            "学员 " + student.getRealName() + " 申请从教练 " + currentCoach.getRealName() + 
            " 更换为教练 " + newCoach.getRealName() + "，请审核。",
            student.getId());
    }

    /**
     * 发送课程预约相关消息
     */
    public void sendBookingMessage(User student, User coach, String action, String details) {
        String title = "课程预约" + action;
        MessageType messageType = MessageType.BOOKING_NOTIFICATION;

        // 发送给学员
        sendMessage(coach, student, messageType, title, details, null);
        
        // 发送给教练
        sendMessage(student, coach, messageType, title, details, null);
    }

    /**
     * 发送上课提醒
     */
    public void sendClassReminder(User student, User coach, String classDetails) {
        String title = "上课提醒";
        String content = "您有一节课即将开始：" + classDetails;

        // 发送给学员和教练
        sendNotification(student, MessageType.CLASS_REMINDER, title, content, null);
        sendNotification(coach, MessageType.CLASS_REMINDER, title, content, null);
    }

    /**
     * 发送管理员操作通知
     */
    public void sendAdminOperationNotification(User admin, User targetUser, String operation, String details) {
        String title = "管理员操作通知";
        String content = "管理员 " + admin.getRealName() + " 对您执行了以下操作：" + operation + "\n详情：" + details;

        sendMessage(admin, targetUser, MessageType.ADMIN_NOTIFICATION, title, content, null);
    }
} 
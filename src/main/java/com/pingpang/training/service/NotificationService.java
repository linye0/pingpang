package com.pingpang.training.service;

import com.pingpang.training.entity.CourseBooking;
import com.pingpang.training.entity.SystemMessage;
import com.pingpang.training.entity.Student;
import com.pingpang.training.entity.Coach;
import com.pingpang.training.entity.User;
import com.pingpang.training.enums.MessageType;
import com.pingpang.training.enums.BookingStatus;
import com.pingpang.training.repository.CourseBookingRepository;
import com.pingpang.training.repository.SystemMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {
    
    @Autowired
    private CourseBookingRepository courseBookingRepository;
    
    @Autowired
    private SystemMessageRepository systemMessageRepository;
    
    /**
     * 课前1小时提醒
     * 每分钟检查一次
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    @Transactional
    public void sendPreClassReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourLater = now.plusHours(1);
        
        // 查找1小时后开始的已确认课程
        List<CourseBooking> upcomingBookings = courseBookingRepository
            .findByStartTimeBetweenAndStatus(now, oneHourLater, BookingStatus.CONFIRMED);
        
        for (CourseBooking booking : upcomingBookings) {
            // 检查是否已经发送过提醒
            boolean reminderSent = systemMessageRepository.existsByRelatedIdAndMessageType(
                booking.getId(), MessageType.COURSE_REMINDER);
            
            if (!reminderSent) {
                sendReminderToStudent(booking);
                sendReminderToCoach(booking);
            }
        }
    }
    
    /**
     * 发送提醒给学员
     */
    private void sendReminderToStudent(CourseBooking booking) {
        SystemMessage message = new SystemMessage();
        message.setReceiver(booking.getStudent());
        message.setTitle("课程提醒");
        message.setContent(String.format(
            "您在%s的课程即将开始（1小时后），教练：%s，球台：%s。请准时到达！",
            booking.getStartTime().toLocalTime().toString(),
            booking.getCoach().getRealName(),
            booking.getTableNumber()
        ));
        message.setMessageType(MessageType.COURSE_REMINDER);
        message.setRelatedId(booking.getId());
        message.setIsRead(false);
        
        systemMessageRepository.save(message);
    }
    
    /**
     * 发送提醒给教练
     */
    private void sendReminderToCoach(CourseBooking booking) {
        SystemMessage message = new SystemMessage();
        message.setReceiver(booking.getCoach());
        message.setTitle("课程提醒");
        message.setContent(String.format(
            "您在%s的课程即将开始（1小时后），学员：%s，球台：%s。请准时到达！",
            booking.getStartTime().toLocalTime().toString(),
            booking.getStudent().getRealName(),
            booking.getTableNumber()
        ));
        message.setMessageType(MessageType.COURSE_REMINDER);
        message.setRelatedId(booking.getId());
        message.setIsRead(false);
        
        systemMessageRepository.save(message);
    }
    
    /**
     * 发送课程取消通知
     */
    @Transactional
    public void sendCancellationNotification(CourseBooking booking, String reason) {
        // 通知学员
        SystemMessage studentMessage = new SystemMessage();
        studentMessage.setReceiver(booking.getStudent());
        studentMessage.setTitle("课程取消通知");
        studentMessage.setContent(String.format(
            "您预约的%s课程已被取消。教练：%s，原因：%s。费用已退回您的账户。",
            booking.getStartTime(),
            booking.getCoach().getRealName(),
            reason
        ));
        studentMessage.setMessageType(MessageType.BOOKING_CANCELLED);
        studentMessage.setRelatedId(booking.getId());
        studentMessage.setIsRead(false);
        
        // 通知教练
        SystemMessage coachMessage = new SystemMessage();
        coachMessage.setReceiver(booking.getCoach());
        coachMessage.setTitle("课程取消通知");
        coachMessage.setContent(String.format(
            "您的%s课程已被取消。学员：%s，原因：%s。",
            booking.getStartTime(),
            booking.getStudent().getRealName(),
            reason
        ));
        coachMessage.setMessageType(MessageType.BOOKING_CANCELLED);
        coachMessage.setRelatedId(booking.getId());
        coachMessage.setIsRead(false);
        
        systemMessageRepository.save(studentMessage);
        systemMessageRepository.save(coachMessage);
    }
    
    /**
     * 发送课程预约成功通知
     */
    @Transactional
    public void sendBookingConfirmationNotification(CourseBooking booking) {
        // 通知学员
        SystemMessage studentMessage = new SystemMessage();
        studentMessage.setReceiver(booking.getStudent());
        studentMessage.setTitle("课程预约成功");
        studentMessage.setContent(String.format(
            "您成功预约了%s的课程。教练：%s，球台：%s，费用：%.2f元。",
            booking.getStartTime(),
            booking.getCoach().getRealName(),
            booking.getTableNumber(),
            booking.getCost()
        ));
        studentMessage.setMessageType(MessageType.BOOKING_CONFIRMED);
        studentMessage.setRelatedId(booking.getId());
        studentMessage.setIsRead(false);
        
        // 通知教练
        SystemMessage coachMessage = new SystemMessage();
        coachMessage.setReceiver(booking.getCoach());
        coachMessage.setTitle("新课程预约");
        coachMessage.setContent(String.format(
            "学员%s预约了您%s的课程，球台：%s。请准时到达！",
            booking.getStudent().getRealName(),
            booking.getStartTime(),
            booking.getTableNumber()
        ));
        coachMessage.setMessageType(MessageType.BOOKING_CONFIRMED);
        coachMessage.setRelatedId(booking.getId());
        coachMessage.setIsRead(false);
        
        systemMessageRepository.save(studentMessage);
        systemMessageRepository.save(coachMessage);
    }
    

} 
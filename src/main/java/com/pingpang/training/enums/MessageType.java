package com.pingpang.training.enums;

public enum MessageType {
    SYSTEM("系统消息"),
    COACH_APPROVAL("教练审核"),
    BOOKING_APPROVAL("预约审核"),
    COURSE_REMINDER("课程提醒"),
    COACH_CHANGE("教练更换"),
    BOOKING_NOTIFICATION("课程预约通知"),
    BOOKING_CONFIRMED("预约确认"),
    BOOKING_CANCELLED("预约取消"),
    CLASS_REMINDER("上课提醒"),
    ADMIN_NOTIFICATION("管理员通知"),
    PAYMENT_NOTIFICATION("支付通知"),
    COMPETITION_NOTIFICATION("比赛通知"),
    EVALUATION_REQUEST("评价请求"),
    COACH_STUDENT_RELATION("师生关系"),
    SYSTEM_MAINTENANCE("系统维护"),
    URGENT("紧急通知");
    
    private final String description;
    
    MessageType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
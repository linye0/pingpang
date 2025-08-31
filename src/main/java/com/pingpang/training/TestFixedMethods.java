package com.pingpang.training;

import com.pingpang.training.dto.BookingRequest;
import com.pingpang.training.entity.Student;
import com.pingpang.training.entity.Coach;
import com.pingpang.training.entity.CourseEvaluation;
import com.pingpang.training.entity.CourseBooking;

/**
 * 测试类 - 验证所有修复的方法是否存在
 */
public class TestFixedMethods {
    
    public static void main(String[] args) {
        System.out.println("开始测试修复的方法...");
        
        // 测试 BookingRequest.getNotes()
        BookingRequest request = new BookingRequest();
        String notes = request.getNotes();
        System.out.println("✓ BookingRequest.getNotes() 存在");
        
        // 测试 Student 紧急联系人方法
        Student student = new Student();
        String contact = student.getEmergencyContact();
        String phone = student.getEmergencyPhone();
        System.out.println("✓ Student.getEmergencyContact() 存在");
        System.out.println("✓ Student.getEmergencyPhone() 存在");
        
        // 测试 Coach.getSpecialty()
        Coach coach = new Coach();
        String specialty = coach.getSpecialty();
        System.out.println("✓ Coach.getSpecialty() 存在");
        
        // 测试 CourseBooking notes方法
        CourseBooking booking = new CourseBooking();
        String bookingNotes = booking.getNotes();
        booking.setNotes("test");
        System.out.println("✓ CourseBooking.getNotes() 存在");
        System.out.println("✓ CourseBooking.setNotes() 存在");
        
        // 测试 CourseEvaluation.setStudentRating()
        CourseEvaluation evaluation = new CourseEvaluation();
        evaluation.setStudentRating(5);
        System.out.println("✓ CourseEvaluation.setStudentRating() 存在");
        
        System.out.println("所有方法测试通过！编译错误应该已经修复。");
    }
}
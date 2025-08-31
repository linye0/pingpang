package com.pingpang.training.controller;

import com.pingpang.training.entity.CourseBooking;
import com.pingpang.training.repository.CourseBookingRepository;
import com.pingpang.training.dto.ApiResponse;
import com.pingpang.training.service.BookingService;
import com.pingpang.training.security.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/course")
@CrossOrigin(origins = "*")
public class CourseController {

    @Autowired
    private CourseBookingRepository courseBookingRepository;
    
    @Autowired
    private BookingService bookingService;

    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<List<CourseBooking>> getMyBookings(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<CourseBooking> bookings = courseBookingRepository.findByStudentId(userDetails.getUser().getId());
            return ApiResponse.success(bookings);
        } catch (Exception e) {
            return ApiResponse.error("获取预约列表失败");
        }
    }
    
    @GetMapping("/available-tables")
    @PreAuthorize("hasRole('STUDENT') or hasRole('COACH') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getAvailableTables(@RequestParam String startTime,
                                            @RequestParam String endTime) {
        try {
            LocalDateTime start = LocalDateTime.parse(startTime);
            LocalDateTime end = LocalDateTime.parse(endTime);
            List<String> availableTables = bookingService.getAvailableTables(start, end);
            return ApiResponse.success(availableTables);
        } catch (Exception e) {
            return ApiResponse.error("获取可用球台失败");
        }
    }
    
    @GetMapping("/coach-schedule/{coachId}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('COACH') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getCoachSchedule(@PathVariable Long coachId,
                                          @RequestParam String startTime,
                                          @RequestParam String endTime) {
        try {
            LocalDateTime start = LocalDateTime.parse(startTime);
            LocalDateTime end = LocalDateTime.parse(endTime);
            List<CourseBooking> schedule = courseBookingRepository.findByCoachIdAndTimeRange(coachId, start, end);
            return ApiResponse.success(schedule);
        } catch (Exception e) {
            return ApiResponse.error("获取教练课表失败");
        }
    }
} 
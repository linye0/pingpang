package com.pingpang.training.controller;

import com.pingpang.training.dto.ApiResponse;
import com.pingpang.training.service.CampusService;
import com.pingpang.training.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*")
public class PublicController {

    @Autowired
    private CampusService campusService;

    @Autowired
    private UserService userService;

    @GetMapping("/campuses")
    public ApiResponse<?> getCampuses() {
        try {
            return ApiResponse.success(campusService.getAllActiveCampuses());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/student/register")
    public ApiResponse<?> registerStudent(@Valid @RequestBody com.pingpang.training.entity.Student student) {
        try {
            return ApiResponse.success("注册成功", userService.registerStudent(student));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PostMapping("/coach/register")
    public ApiResponse<?> registerCoach(@Valid @RequestBody com.pingpang.training.entity.Coach coach) {
        try {
            return ApiResponse.success("注册申请已提交，请等待审核", userService.registerCoach(coach));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
} 
package com.pingpang.training.controller;

import com.pingpang.training.dto.ApiResponse;
import com.pingpang.training.entity.SoftwareLicense;
import com.pingpang.training.entity.User;
import com.pingpang.training.enums.UserRole;
import com.pingpang.training.repository.SoftwareLicenseRepository;
import com.pingpang.training.repository.UserRepository;
import com.pingpang.training.security.UserDetailsImpl;
import com.pingpang.training.service.SystemLogService;
import com.pingpang.training.service.CampusService;
import com.pingpang.training.service.UserService;
import com.pingpang.training.entity.Campus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Optional;

@RestController
@RequestMapping("/api/super-admin")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    @Autowired
    private SoftwareLicenseRepository softwareLicenseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemLogService systemLogService;

    @Autowired
    private CampusService campusService;

    @Autowired
    private UserService userService;

    // 系统激活与许可证管理
    
    @GetMapping("/licenses")
    public ApiResponse<?> getAllLicenses() {
        try {
            List<SoftwareLicense> licenses = softwareLicenseRepository.findAll();
            return ApiResponse.success(licenses);
        } catch (Exception e) {
            return ApiResponse.error("获取许可证列表失败");
        }
    }

    @GetMapping("/licenses/active")
    public ApiResponse<?> getActiveLicenses() {
        try {
            List<SoftwareLicense> licenses = softwareLicenseRepository.findByActiveTrue();
            return ApiResponse.success(licenses);
        } catch (Exception e) {
            return ApiResponse.error("获取活跃许可证失败");
        }
    }

    @GetMapping("/licenses/expired")
    public ApiResponse<?> getExpiredLicenses() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<SoftwareLicense> licenses = softwareLicenseRepository.findExpiredLicenses(now);
            return ApiResponse.success(licenses);
        } catch (Exception e) {
            return ApiResponse.error("获取过期许可证失败");
        }
    }

    @GetMapping("/licenses/expiring")
    public ApiResponse<?> getExpiringLicenses() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime warningDate = now.plusDays(30); // 30天内到期
            List<SoftwareLicense> licenses = softwareLicenseRepository.findExpiringLicenses(now, warningDate);
            return ApiResponse.success(licenses);
        } catch (Exception e) {
            return ApiResponse.error("获取即将过期许可证失败");
        }
    }

    @PostMapping("/licenses")
    public ApiResponse<?> createLicense(@Valid @RequestBody SoftwareLicense license,
                                       @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // 检查组织名称是否已存在
            if (softwareLicenseRepository.existsByOrganizationName(license.getOrganizationName())) {
                return ApiResponse.error("组织名称已存在");
            }

            // 生成唯一的许可证密钥
            String licenseKey = generateLicenseKey();
            license.setLicenseKey(licenseKey);

            // 保存许可证
            SoftwareLicense savedLicense = softwareLicenseRepository.save(license);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "LICENSE_CREATE", 
                "创建许可证: " + license.getOrganizationName());

            return ApiResponse.success("许可证创建成功", savedLicense);
        } catch (Exception e) {
            return ApiResponse.error("创建许可证失败: " + e.getMessage());
        }
    }

    @PutMapping("/licenses/{id}")
    public ApiResponse<?> updateLicense(@PathVariable Long id, 
                                       @Valid @RequestBody SoftwareLicense license,
                                       @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            SoftwareLicense existingLicense = softwareLicenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("许可证不存在"));

            existingLicense.setOrganizationName(license.getOrganizationName());
            existingLicense.setDeviceId(license.getDeviceId());
            existingLicense.setStartDate(license.getStartDate());
            existingLicense.setEndDate(license.getEndDate());
            existingLicense.setAnnualFee(license.getAnnualFee());
            existingLicense.setActive(license.getActive());

            SoftwareLicense savedLicense = softwareLicenseRepository.save(existingLicense);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "LICENSE_UPDATE", 
                "更新许可证: " + license.getOrganizationName());

            return ApiResponse.success("许可证更新成功", savedLicense);
        } catch (Exception e) {
            return ApiResponse.error("更新许可证失败: " + e.getMessage());
        }
    }

    @PostMapping("/licenses/{id}/activate")
    public ApiResponse<?> activateLicense(@PathVariable Long id,
                                         @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            SoftwareLicense license = softwareLicenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("许可证不存在"));

            license.setActive(true);
            softwareLicenseRepository.save(license);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "LICENSE_ACTIVATE", 
                "激活许可证: " + license.getOrganizationName());

            return ApiResponse.success("许可证激活成功", license);
        } catch (Exception e) {
            return ApiResponse.error("激活许可证失败: " + e.getMessage());
        }
    }

    @PostMapping("/licenses/{id}/deactivate")
    public ApiResponse<?> deactivateLicense(@PathVariable Long id,
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            SoftwareLicense license = softwareLicenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("许可证不存在"));

            license.setActive(false);
            softwareLicenseRepository.save(license);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "LICENSE_DEACTIVATE", 
                "停用许可证: " + license.getOrganizationName());

            return ApiResponse.success("许可证停用成功", license);
        } catch (Exception e) {
            return ApiResponse.error("停用许可证失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/licenses/{id}")
    public ApiResponse<?> deleteLicense(@PathVariable Long id,
                                       @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            SoftwareLicense license = softwareLicenseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("许可证不存在"));

            softwareLicenseRepository.delete(license);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "LICENSE_DELETE", 
                "删除许可证: " + license.getOrganizationName());

            return ApiResponse.success("许可证删除成功", null);
        } catch (Exception e) {
            return ApiResponse.error("删除许可证失败: " + e.getMessage());
        }
    }

    // 校区管理员指定功能
    
    @PostMapping("/assign-campus-admin")
    public ApiResponse<?> assignCampusAdmin(@RequestParam Long userId, 
                                           @RequestParam Long campusId,
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            Campus campus = campusService.findById(campusId)
                .orElseThrow(() -> new RuntimeException("校区不存在"));
            
            // 检查用户当前角色
            if (user.getRole() == UserRole.SUPER_ADMIN) {
                return ApiResponse.error("超级管理员不能被指定为校区管理员");
            }
            
            // 检查该校区是否已有管理员
            if (campus.getAdmin() != null && !campus.getAdmin().getId().equals(userId)) {
                return ApiResponse.error("该校区已有管理员，请先解除当前管理员");
            }
            
            // 更新用户角色为校区管理员
            user.setRole(UserRole.CAMPUS_ADMIN);
            user.setCampus(campus);
            userRepository.save(user);
            
            // 更新校区管理员
            campus.setAdmin(user);
            campusService.save(campus);
            
            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "ASSIGN_CAMPUS_ADMIN", 
                "指定校区管理员: " + user.getRealName() + " -> " + campus.getName());
            
            return ApiResponse.success("校区管理员指定成功", user);
        } catch (Exception e) {
            return ApiResponse.error("指定校区管理员失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/unassign-campus-admin/{campusId}")
    public ApiResponse<?> unassignCampusAdmin(@PathVariable Long campusId,
                                             @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Campus campus = campusService.findById(campusId)
                .orElseThrow(() -> new RuntimeException("校区不存在"));
            
            if (campus.getAdmin() == null) {
                return ApiResponse.error("该校区没有指定管理员");
            }
            
            User admin = campus.getAdmin();
            
            // 如果是主校区，超级管理员不能解除自己的管理员身份
            if (campus.getIsMainCampus() && admin.getRole() == UserRole.SUPER_ADMIN) {
                return ApiResponse.error("不能解除主校区超级管理员身份");
            }
            
            // 将用户角色降级为普通用户（根据原来角色确定）
            if (admin.getRole() == UserRole.CAMPUS_ADMIN) {
                // 如果原来就是校区管理员，需要根据业务逻辑确定降级为什么角色
                // 这里假设降级为学员，实际应用中可能需要更复杂的逻辑
                admin.setRole(UserRole.STUDENT);
            }
            
            userRepository.save(admin);
            
            // 清除校区管理员
            campus.setAdmin(null);
            campusService.save(campus);
            
            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "UNASSIGN_CAMPUS_ADMIN", 
                "解除校区管理员: " + admin.getRealName() + " <- " + campus.getName());
            
            return ApiResponse.success("校区管理员解除成功", null);
        } catch (Exception e) {
            return ApiResponse.error("解除校区管理员失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/campus-admins")
    public ApiResponse<?> getAllCampusAdmins() {
        try {
            List<User> campusAdmins = userRepository.findByRole(UserRole.CAMPUS_ADMIN);
            return ApiResponse.success(campusAdmins);
        } catch (Exception e) {
            return ApiResponse.error("获取校区管理员列表失败");
        }
    }
    
    @GetMapping("/available-users-for-admin")
    public ApiResponse<?> getAvailableUsersForAdmin() {
        try {
            // 获取可以被指定为管理员的用户（排除超级管理员和已经是校区管理员的用户）
            List<User> availableUsers = userRepository.findByRoleInAndActiveTrue(
                Arrays.asList(UserRole.STUDENT, UserRole.COACH));
            return ApiResponse.success(availableUsers);
        } catch (Exception e) {
            return ApiResponse.error("获取可指定用户列表失败");
        }
    }
    
    @PostMapping("/set-main-campus/{campusId}")
    public ApiResponse<?> setMainCampus(@PathVariable Long campusId,
                                       @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            // 清除所有校区的主校区标记
            List<Campus> allCampuses = campusService.findAll();
            for (Campus campus : allCampuses) {
                campus.setIsMainCampus(false);
                campusService.save(campus);
            }
            
            // 设置新的主校区
            Campus newMainCampus = campusService.findById(campusId)
                .orElseThrow(() -> new RuntimeException("校区不存在"));
            
            newMainCampus.setIsMainCampus(true);
            
            // 如果主校区没有管理员，自动将超级管理员设为主校区管理员
            if (newMainCampus.getAdmin() == null) {
                User superAdmin = userDetails.getUser();
                newMainCampus.setAdmin(superAdmin);
                superAdmin.setCampus(newMainCampus);
                userRepository.save(superAdmin);
            }
            
            campusService.save(newMainCampus);
            
            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "SET_MAIN_CAMPUS", 
                "设置主校区: " + newMainCampus.getName());
            
            return ApiResponse.success("主校区设置成功", newMainCampus);
        } catch (Exception e) {
            return ApiResponse.error("设置主校区失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/transfer-main-campus")
    public ApiResponse<?> transferMainCampus(@RequestParam Long fromCampusId,
                                           @RequestParam Long toCampusId,
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            campusService.transferMainCampus(fromCampusId, toCampusId, userDetails.getUser());
            return ApiResponse.success("主校区转移成功", null);
        } catch (Exception e) {
            return ApiResponse.error("转移主校区失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/main-campus")
    public ApiResponse<?> getMainCampus() {
        try {
            Optional<Campus> mainCampus = campusService.getMainCampus();
            if (mainCampus.isPresent()) {
                return ApiResponse.success(mainCampus.get());
            } else {
                return ApiResponse.error("系统中没有主校区");
            }
        } catch (Exception e) {
            return ApiResponse.error("获取主校区信息失败");
        }
    }
    
    @PostMapping("/validate-main-campus")
    public ApiResponse<?> validateMainCampus() {
        try {
            campusService.validateMainCampusIntegrity();
            return ApiResponse.success("主校区完整性验证完成", null);
        } catch (Exception e) {
            return ApiResponse.error("验证主校区失败: " + e.getMessage());
        }
    }

    // 全局管理功能

    @GetMapping("/users/all")
    public ApiResponse<?> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            return ApiResponse.success(users);
        } catch (Exception e) {
            return ApiResponse.error("获取用户列表失败");
        }
    }

    @GetMapping("/users/statistics")
    public ApiResponse<?> getUserStatistics() {
        try {
            long totalUsers = userRepository.count();
            long superAdmins = userRepository.countByRole(UserRole.SUPER_ADMIN);
            long campusAdmins = userRepository.countByRole(UserRole.CAMPUS_ADMIN);
            long coaches = userRepository.countByRole(UserRole.COACH);
            long students = userRepository.countByRole(UserRole.STUDENT);

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalUsers", totalUsers);
            statistics.put("superAdmins", superAdmins);
            statistics.put("campusAdmins", campusAdmins);
            statistics.put("coaches", coaches);
            statistics.put("students", students);
            
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            return ApiResponse.error("获取用户统计失败");
        }
    }

    @PostMapping("/users/{id}/disable")
    public ApiResponse<?> disableUser(@PathVariable Long id,
                                     @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

            if (user.getId().equals(userDetails.getUser().getId())) {
                return ApiResponse.error("不能禁用自己的账户");
            }

            user.setActive(false);
            userRepository.save(user);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "USER_DISABLE", 
                "禁用用户: " + user.getUsername());

            return ApiResponse.success("用户禁用成功", null);
        } catch (Exception e) {
            return ApiResponse.error("禁用用户失败: " + e.getMessage());
        }
    }

    @PostMapping("/users/{id}/enable")
    public ApiResponse<?> enableUser(@PathVariable Long id,
                                    @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

            user.setActive(true);
            userRepository.save(user);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "USER_ENABLE", 
                "启用用户: " + user.getUsername());

            return ApiResponse.success("用户启用成功", null);
        } catch (Exception e) {
            return ApiResponse.error("启用用户失败: " + e.getMessage());
        }
    }

    @GetMapping("/system/info")
    public ApiResponse<?> getSystemInfo() {
        try {
            Map<String, Object> systemInfo = new HashMap<>();
            systemInfo.put("version", "1.0.0");
            systemInfo.put("currentTime", LocalDateTime.now());
            systemInfo.put("totalUsers", userRepository.count());
            systemInfo.put("activeLicenses", softwareLicenseRepository.findByActiveTrue().size());
            systemInfo.put("systemStatus", "运行正常");
            
            return ApiResponse.success(systemInfo);
        } catch (Exception e) {
            return ApiResponse.error("获取系统信息失败");
        }
    }

    // 私有方法

    private String generateLicenseKey() {
        return "PPS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
} 
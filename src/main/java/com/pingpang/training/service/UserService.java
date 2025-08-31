package com.pingpang.training.service;

import com.pingpang.training.dto.RegisterRequest;
import com.pingpang.training.entity.*;
import com.pingpang.training.enums.UserRole;
import com.pingpang.training.enums.ApprovalStatus;
import com.pingpang.training.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CoachRepository coachRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private CoachStudentRelationRepository relationRepository;

    @Autowired
    private SystemLogService systemLogService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SystemMessageService systemMessageService;

    public Student registerStudent(Student student) {
        // 验证用户名和手机号是否已存在
        if (userRepository.existsByUsername(student.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.existsByPhone(student.getPhone())) {
            throw new RuntimeException("手机号已被注册");
        }

        // 验证校区是否存在
        Campus campus = campusRepository.findById(student.getCampus().getId())
                .orElseThrow(() -> new RuntimeException("校区不存在"));

        student.setCampus(campus);
        student.setRole(UserRole.STUDENT);
        
        // 调试信息 - 记录密码处理过程
        System.out.println("DEBUG: 注册学员 - 用户名: " + student.getUsername());
        System.out.println("DEBUG: 注册学员 - 原始密码: " + student.getPassword());
        String encodedPassword = passwordEncoder.encode(student.getPassword());
        System.out.println("DEBUG: 注册学员 - 加密后密码: " + encodedPassword);
        student.setPassword(encodedPassword);

        Student savedStudent = studentRepository.save(student);
        System.out.println("DEBUG: 注册学员 - 保存成功，ID: " + savedStudent.getId());

        // 暂时禁用系统日志，避免外键约束问题
        // systemLogService.log(savedStudent, "学员注册", "学员 " + student.getRealName() + " 注册成功");

        return savedStudent;
    }

    public Coach registerCoach(Coach coach) {
        // 验证用户名和手机号是否已存在
        if (userRepository.existsByUsername(coach.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.existsByPhone(coach.getPhone())) {
            throw new RuntimeException("手机号已被注册");
        }

        // 验证校区是否存在
        Campus campus = campusRepository.findById(coach.getCampus().getId())
                .orElseThrow(() -> new RuntimeException("校区不存在"));

        coach.setCampus(campus);
        coach.setRole(UserRole.COACH);
        
        // 调试信息 - 记录密码处理过程
        System.out.println("DEBUG: 注册教练 - 用户名: " + coach.getUsername());
        System.out.println("DEBUG: 注册教练 - 原始密码: " + coach.getPassword());
        String encodedPassword = passwordEncoder.encode(coach.getPassword());
        System.out.println("DEBUG: 注册教练 - 加密后密码: " + encodedPassword);
        coach.setPassword(encodedPassword);
        coach.setApprovalStatus(ApprovalStatus.PENDING);

        Coach savedCoach = coachRepository.save(coach);
        System.out.println("DEBUG: 注册教练 - 保存成功，ID: " + savedCoach.getId());

        // 暂时禁用系统日志，避免外键约束问题
        // systemLogService.log(savedCoach, "教练注册", "教练 " + coach.getRealName() + " 提交入职申请");

        return savedCoach;
    }

    public List<Coach> getPendingCoaches(Long campusId) {
        if (campusId == null) {
            // SUPER_ADMIN 查看所有待审核教练
            return coachRepository.findByApprovalStatus(ApprovalStatus.PENDING);
        } else {
            // CAMPUS_ADMIN 查看指定校区的待审核教练
            return coachRepository.findByCampusIdAndApprovalStatus(campusId, ApprovalStatus.PENDING);
        }
    }

    public Coach approveCoach(Long coachId, User approver) {
        Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("教练不存在"));

        coach.setApprovalStatus(ApprovalStatus.APPROVED);
        Coach savedCoach = coachRepository.save(coach);

        // 发送审核通过消息
        systemMessageService.sendCoachApprovalMessage(coach, approver, true);

        // 记录系统日志
        systemLogService.log(approver, "教练审核", "审核通过教练 " + coach.getRealName());

        return savedCoach;
    }

    public Coach rejectCoach(Long coachId, User approver) {
        Coach coach = coachRepository.findById(coachId)
                .orElseThrow(() -> new RuntimeException("教练不存在"));

        coach.setApprovalStatus(ApprovalStatus.REJECTED);
        Coach savedCoach = coachRepository.save(coach);

        // 发送审核拒绝消息
        systemMessageService.sendCoachApprovalMessage(coach, approver, false);

        // 记录系统日志
        systemLogService.log(approver, "教练审核", "拒绝教练 " + coach.getRealName() + " 的入职申请");

        return savedCoach;
    }

    public User updateUserInfo(User user) {
        User existingUser = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        existingUser.setRealName(user.getRealName());
        existingUser.setGender(user.getGender());
        existingUser.setAge(user.getAge());
        existingUser.setEmail(user.getEmail());
        existingUser.setAvatar(user.getAvatar());

        User savedUser = userRepository.save(existingUser);

        // 记录系统日志
        systemLogService.log(savedUser, "更新信息", "更新个人信息");

        return savedUser;
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new RuntimeException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        User savedUser = userRepository.save(user);

        // 记录系统日志 - 使用独立事务避免影响主要操作
        try {
            systemLogService.log(savedUser, "修改密码", "修改登录密码");
        } catch (Exception logException) {
            // 记录日志失败不应影响主要操作
            System.err.println("System log failed but password change succeeded: " + logException.getMessage());
        }
    }

    public List<CoachStudentRelation> getStudentCoaches(Long studentId) {
        return relationRepository.findApprovedByStudentId(studentId);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    // 使用RegisterRequest注册学员
    public Student registerStudent(RegisterRequest request) {
        // 验证用户名和手机号是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("手机号已被注册");
        }

        // 验证校区是否存在
        Campus campus = campusRepository.findById(request.getCampusId())
                .orElseThrow(() -> new RuntimeException("校区不存在"));

        // 创建User基本信息
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setGender(request.getGender());
        user.setAge(request.getAge());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setCampus(campus);
        user.setRole(UserRole.STUDENT);
        
        User savedUser = userRepository.save(user);
        
        // 创建Student具体信息
        Student student = new Student();
        student.setId(savedUser.getId());
        student.setAccountBalance(BigDecimal.ZERO);
        
        Student savedStudent = studentRepository.save(student);

        // 记录系统日志
        systemLogService.log(savedUser, "学员注册", "学员 " + request.getRealName() + " 注册成功");

        return savedStudent;
    }
    
    // 使用RegisterRequest注册教练
    public Coach registerCoach(RegisterRequest request) {
        // 验证用户名和手机号是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("手机号已被注册");
        }

        // 验证校区是否存在
        Campus campus = campusRepository.findById(request.getCampusId())
                .orElseThrow(() -> new RuntimeException("校区不存在"));

        // 创建User基本信息
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setGender(request.getGender());
        user.setAge(request.getAge());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setCampus(campus);
        user.setRole(UserRole.COACH);
        user.setAvatar(request.getAvatar());
        
        User savedUser = userRepository.save(user);
        
        // 创建Coach具体信息
        Coach coach = new Coach();
        coach.setId(savedUser.getId());
        coach.setLevel(request.getLevel());
        coach.setAchievements(request.getAchievements());
        coach.setApprovalStatus(ApprovalStatus.PENDING);
        
        Coach savedCoach = coachRepository.save(coach);

        // 记录系统日志
        systemLogService.log(savedUser, "教练注册", "教练 " + request.getRealName() + " 提交入职申请");

        return savedCoach;
    }
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public List<User> getUsersByCampusId(Long campusId) {
        return userRepository.findByCampusId(campusId);
    }
    
    public User save(User user) {
        return userRepository.save(user);
    }
} 
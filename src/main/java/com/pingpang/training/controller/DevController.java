package com.pingpang.training.controller;

import com.pingpang.training.dto.ApiResponse;
import com.pingpang.training.entity.User;
import com.pingpang.training.entity.Campus;
import com.pingpang.training.repository.UserRepository;
import com.pingpang.training.repository.CampusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.Optional;
import com.pingpang.training.enums.UserRole;
import com.pingpang.training.enums.Gender;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import com.pingpang.training.entity.MonthlyCompetition;
import com.pingpang.training.repository.MonthlyCompetitionRepository;
import java.util.ArrayList;
import com.pingpang.training.entity.CourseBooking;
import com.pingpang.training.enums.BookingStatus;
import com.pingpang.training.entity.CourseEvaluation;
import com.pingpang.training.repository.CourseBookingRepository;
import com.pingpang.training.repository.CourseEvaluationRepository;
import com.pingpang.training.entity.Student;
import com.pingpang.training.entity.Coach;
import com.pingpang.training.repository.StudentRepository;
import com.pingpang.training.repository.CoachRepository;
import java.util.Random;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dev")
@CrossOrigin(origins = "*")
public class DevController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MonthlyCompetitionRepository monthlyCompetitionRepository;

    @Autowired
    private CourseBookingRepository courseBookingRepository;

    @Autowired
    private CourseEvaluationRepository courseEvaluationRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CoachRepository coachRepository;

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> userInfo = users.stream().map(user -> {
            Map<String, Object> userMap = new HashMap<>();
            userMap.put("id", user.getId());
            userMap.put("username", user.getUsername());
            userMap.put("realName", user.getRealName());
            userMap.put("role", user.getRole().toString());
            userMap.put("active", user.getActive());
            userMap.put("passwordHash", user.getPassword());
            return userMap;
        }).collect(Collectors.toList());
        
        return ApiResponse.success(userInfo);
    }

    @PostMapping("/reset-all-passwords")
    public ApiResponse<String> resetAllPasswords() {
        return doResetAllPasswords();
    }
    
    @GetMapping("/reset-all-passwords")
    public ApiResponse<String> resetAllPasswordsGet() {
        return doResetAllPasswords();
    }
    
    @Transactional
    private ApiResponse<String> doResetAllPasswords() {
        try {
            List<User> users = userRepository.findAll();
            String defaultPassword = "123456";
            int successCount = 0;
            int failCount = 0;
            StringBuilder errorDetails = new StringBuilder();
            
            for (User user : users) {
                try {
                    // 使用BCrypt编码器对密码进行加密
                    String encodedPassword = passwordEncoder.encode(defaultPassword);
                    user.setPassword(encodedPassword);
                    userRepository.save(user);
                    successCount++;
                } catch (Exception e) {
                    failCount++;
                    errorDetails.append("用户 ").append(user.getUsername()).append(" 重置失败: ").append(e.getMessage()).append("; ");
                }
            }
            
            String message = String.format("密码重置完成 - 成功: %d, 失败: %d", successCount, failCount);
            if (failCount > 0) {
                message += " 错误详情: " + errorDetails.toString();
            }
            
            return ApiResponse.success(message);
        } catch (Exception e) {
            return ApiResponse.error("重置密码失败: " + e.getMessage());
        }
    }

    @GetMapping("/reset-passwords-sql")
    public ApiResponse<String> resetPasswordsWithSQL() {
        try {
            String defaultPassword = "123456";
            String encodedPassword = passwordEncoder.encode(defaultPassword);
            
            // 使用原生SQL直接更新user表的password字段
            String sql = "UPDATE user SET password = ? WHERE 1=1";
            
            int updatedCount = entityManager.createNativeQuery(sql)
                    .setParameter(1, encodedPassword)
                    .executeUpdate();
            
            return ApiResponse.success("使用SQL成功重置 " + updatedCount + " 个用户的密码为: " + defaultPassword);
        } catch (Exception e) {
            return ApiResponse.error("SQL重置密码失败: " + e.getMessage());
        }
    }

    @PostMapping("/create-basic-users")
    public ApiResponse<String> createBasicUsers() {
        try {
            // 检查是否已有数据
            if (userRepository.count() > 0) {
                return ApiResponse.success("数据库中已有用户数据，无需重复创建");
            }

            // 创建基本校区
            Campus campus = new Campus();
            campus.setName("星辉乒乓球培训中心");
            campus.setAddress("北京市朝阳区建国路99号");
            campus.setContactPerson("张经理");
            campus.setContactPhone("13800138000");
            campus.setContactEmail("zhang@xinghui.com");
            campus.setActive(true);
            campus.setCreatedAt(LocalDateTime.now());
            campus.setUpdatedAt(LocalDateTime.now());
            campus = campusRepository.save(campus);

            // 创建超级管理员
            User superAdmin = new User();
            superAdmin.setUsername("super_admin");
            superAdmin.setPassword("admin123@");
            superAdmin.setRealName("系统超级管理员");
            superAdmin.setPhone("13800138888");
            superAdmin.setEmail("admin@system.com");

            superAdmin.setActive(true);
            superAdmin.setCampus(campus);
            superAdmin.setCreatedAt(LocalDateTime.now());
            superAdmin.setUpdatedAt(LocalDateTime.now());
            userRepository.save(superAdmin);

            return ApiResponse.success("成功创建基本用户数据！用户名: super_admin, 密码: admin123@");

        } catch (Exception e) {
            return ApiResponse.error("创建用户失败: " + e.getMessage());
        }
    }

    @PostMapping("/create-test-user")
    public ApiResponse<String> createTestUser() {
        try {
            // 创建一个简单的测试用户，密码是123456
            User testUser = new User();
            testUser.setUsername("testuser");
            testUser.setPassword(passwordEncoder.encode("123456"));  // BCrypt加密
            testUser.setRole(UserRole.SUPER_ADMIN);
            testUser.setRealName("测试用户");
            testUser.setPhone("13800000000");
            testUser.setEmail("test@test.com");
            testUser.setGender(Gender.MALE);
            testUser.setAge(30);
            testUser.setActive(true);
            testUser.setCreatedAt(LocalDateTime.now());
            testUser.setUpdatedAt(LocalDateTime.now());
            
            userRepository.save(testUser);
            
            return ApiResponse.success("测试用户创建成功！用户名: testuser, 密码: 123456");
        } catch (Exception e) {
            return ApiResponse.error("创建测试用户失败: " + e.getMessage());
        }
    }

    @GetMapping("/verify-password/{username}")
    public ApiResponse<String> verifyPassword(@PathVariable String username, @RequestParam String password) {
        try {
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                return ApiResponse.error("用户不存在: " + username);
            }
            
            User user = userOpt.get();
            boolean matches = passwordEncoder.matches(password, user.getPassword());
            
            String result = String.format(
                "用户: %s\n前端密码: %s\n数据库哈希: %s\n密码匹配: %s", 
                username, password, user.getPassword(), matches ? "✓ 是" : "✗ 否"
            );
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("验证密码失败: " + e.getMessage());
        }
    }

    @GetMapping("/test-password/{username}")
    public ApiResponse<Map<String, Object>> testPassword(@PathVariable String username, @RequestParam String password) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return ApiResponse.error("用户不存在");
        }
        
        boolean matches = passwordEncoder.matches(password, user.getPassword());
        
        Map<String, Object> result = new HashMap<>();
        result.put("username", user.getUsername());
        result.put("inputPassword", password);
        result.put("storedHash", user.getPassword());
        result.put("matches", matches);
        
        return ApiResponse.success(result);
    }

    @GetMapping("/generate-password-hash")
    public ApiResponse<String> generatePasswordHash() {
        try {
            String plainPassword = "123456";
            String hashedPassword = passwordEncoder.encode(plainPassword);
            
            String result = String.format(
                "明文密码: %s\nBCrypt哈希值: %s\n\n请将此哈希值更新到数据库的password字段中", 
                plainPassword, hashedPassword
            );
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("生成密码哈希失败: " + e.getMessage());
        }
    }

    @GetMapping("/fix-superadmin-password")
    public ApiResponse<String> fixSuperadminPassword() {
        try {
            String plainPassword = "123456";
            String hashedPassword = passwordEncoder.encode(plainPassword);
            
            // 直接更新superadmin的密码
            String sql = "UPDATE user SET password = ? WHERE username = 'superadmin'";
            
            int updatedCount = entityManager.createNativeQuery(sql)
                    .setParameter(1, hashedPassword)
                    .executeUpdate();
            
            if (updatedCount > 0) {
                return ApiResponse.success("成功更新superadmin密码！现在可以用用户名'superadmin'和密码'123456'登录了");
            } else {
                return ApiResponse.error("未找到superadmin用户");
            }
        } catch (Exception e) {
            return ApiResponse.error("更新superadmin密码失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/fix-all-user-passwords")
    @Transactional
    public ApiResponse<String> fixAllUserPasswords() {
        try {
            String plainPassword = "123456";
            String hashedPassword = passwordEncoder.encode(plainPassword);
            
            // 更新所有用户的密码为统一的BCrypt哈希值
            String sql = "UPDATE user SET password = ? WHERE username IN ('admin001', 'coach002', 'student001', 'superadmin')";
            
            int updatedCount = entityManager.createNativeQuery(sql)
                    .setParameter(1, hashedPassword)
                    .executeUpdate();
            
            return ApiResponse.success("成功更新了 " + updatedCount + " 个用户的密码！现在可以用密码'123456'登录这些用户了");
        } catch (Exception e) {
            return ApiResponse.error("更新用户密码失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/create-comprehensive-test-data")
    @Transactional
    public ApiResponse<String> createComprehensiveTestData() {
        try {
            // 1. 创建校区
            Campus campus = createTestCampus();
            
            // 2. 创建用户：学员、教练、管理员
            createTestUsers(campus);
            
            // 3. 创建师生关系
            createTestCoachStudentRelations();
            
            // 4. 创建课程预约
            createTestCourseBookings();
            
            // 5. 创建支付记录
            createTestPaymentRecords();
            
            // 6. 创建月度比赛
            createTestCompetitions(campus);
            
            // 7. 创建课程评价
            createTestEvaluations();
            
            return ApiResponse.success("综合测试数据创建成功！包括校区、用户、师生关系、课程预约、支付记录、比赛和评价数据");
        } catch (Exception e) {
            return ApiResponse.error("创建测试数据失败: " + e.getMessage());
        }
    }

    @PostMapping("/create-test-relations")
    @Transactional
    public ApiResponse<String> createTestRelations() {
        try {
            // 查找教练和学员用户
            List<User> coaches = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.COACH)
                .collect(java.util.stream.Collectors.toList());
                
            List<User> students = userRepository.findAll().stream()
                .filter(u -> u.getRole() == UserRole.STUDENT)
                .collect(java.util.stream.Collectors.toList());
            
            if (coaches.isEmpty() || students.isEmpty()) {
                return ApiResponse.error("需要至少有一个教练和一个学员用户");
            }
            
            int createdCount = 0;
            
            // 为每个教练创建几个学员申请关系
            for (User coach : coaches) {
                for (int i = 0; i < Math.min(students.size(), 3); i++) {
                    User student = students.get(i);
                    
                    // 检查关系是否已经存在
                    String checkSql = "SELECT COUNT(*) FROM coach_student_relations WHERE coach_id = ? AND student_id = ?";
                    Number count = (Number) entityManager.createNativeQuery(checkSql)
                        .setParameter(1, coach.getId())
                        .setParameter(2, student.getId())
                        .getSingleResult();
                    
                    if (count.intValue() == 0) {
                        // 创建师生关系 - 一半是PENDING，一半是APPROVED
                        String status = (i % 2 == 0) ? "PENDING" : "APPROVED";
                        String insertSql = "INSERT INTO coach_student_relations (coach_id, student_id, status, created_at, updated_at) VALUES (?, ?, ?, NOW(), NOW())";
                        
                        entityManager.createNativeQuery(insertSql)
                            .setParameter(1, coach.getId())
                            .setParameter(2, student.getId())
                            .setParameter(3, status)
                            .executeUpdate();
                        
                        createdCount++;
                    }
                }
            }
            
            return ApiResponse.success("成功创建了 " + createdCount + " 个师生关系！包括待审核和已通过的申请");
        } catch (Exception e) {
            return ApiResponse.error("创建测试师生关系失败: " + e.getMessage());
        }
    }

    // 私有方法 - 创建测试数据
    private Campus createTestCampus() {
        // 检查是否已存在测试校区
        List<Campus> existingCampuses = entityManager.createQuery("SELECT c FROM Campus c WHERE c.name LIKE '%测试%'", Campus.class).getResultList();
        if (!existingCampuses.isEmpty()) {
            return existingCampuses.get(0);
        }

        // 创建新的测试校区
        String sql = "INSERT INTO campus (name, address, contact_person, contact_phone, contact_email, active, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, true, NOW(), NOW())";
        
        entityManager.createNativeQuery(sql)
            .setParameter(1, "星辉乒乓球测试校区")
            .setParameter(2, "北京市朝阳区测试路123号")
            .setParameter(3, "测试负责人")
            .setParameter(4, "13800138000")
            .setParameter(5, "test@xinghui.com")
            .executeUpdate();

        // 返回刚创建的校区
        return entityManager.createQuery("SELECT c FROM Campus c WHERE c.name = ?1", Campus.class)
            .setParameter(1, "星辉乒乓球测试校区")
            .getSingleResult();
    }

    private void createTestUsers(Campus campus) {
        // 创建测试学员
        createTestStudent("student001", "张三", campus);
        createTestStudent("student002", "李四", campus);
        createTestStudent("student003", "王五", campus);

        // 创建测试教练
        createTestCoach("coach001", "赵教练", campus, "SENIOR");
        createTestCoach("coach002", "钱教练", campus, "INTERMEDIATE");  
        createTestCoach("coach003", "孙教练", campus, "JUNIOR");

        // 创建测试管理员
        createTestAdmin("admin001", "校区管理员", campus, "CAMPUS_ADMIN");
    }

    private void createTestStudent(String username, String realName, Campus campus) {
        try {
            String userSql = "INSERT INTO user (username, password, real_name, phone, email, gender, age, role, campus_id, active, created_at, updated_at) " +
                           "VALUES (?, ?, ?, ?, ?, 'MALE', 25, 'STUDENT', ?, true, NOW(), NOW())";
            
            entityManager.createNativeQuery(userSql)
                .setParameter(1, username)
                .setParameter(2, passwordEncoder.encode("123456"))
                .setParameter(3, realName)
                .setParameter(4, "1380000" + username.substring(username.length() - 4))
                .setParameter(5, username + "@test.com")
                .setParameter(6, campus.getId())
                .executeUpdate();

            // 获取插入的用户ID
            Number userId = (Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();

            // 创建学员记录
            String studentSql = "INSERT INTO student (id, account_balance, cancellation_count, last_cancellation_reset, emergency_contact, emergency_phone) " +
                              "VALUES (?, 500.00, 0, NULL, '紧急联系人', '13900000000')";
            
            entityManager.createNativeQuery(studentSql)
                .setParameter(1, userId.longValue())
                .executeUpdate();
        } catch (Exception e) {
            // 如果用户已存在，忽略错误
            System.out.println("用户 " + username + " 可能已存在，跳过创建");
        }
    }

    private void createTestCoach(String username, String realName, Campus campus, String level) {
        try {
            String userSql = "INSERT INTO user (username, password, real_name, phone, email, gender, age, role, campus_id, active, created_at, updated_at) " +
                           "VALUES (?, ?, ?, ?, ?, 'MALE', 30, 'COACH', ?, true, NOW(), NOW())";
            
            entityManager.createNativeQuery(userSql)
                .setParameter(1, username)
                .setParameter(2, passwordEncoder.encode("123456"))
                .setParameter(3, realName)
                .setParameter(4, "1380000" + username.substring(username.length() - 4))
                .setParameter(5, username + "@test.com")
                .setParameter(6, campus.getId())
                .executeUpdate();

            // 获取插入的用户ID
            Number userId = (Number) entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();

            // 创建教练记录
            String coachSql = "INSERT INTO coach (id, level, approval_status, achievements, specialty, cancellation_count, last_cancellation_reset) " +
                            "VALUES (?, ?, 'APPROVED', '测试教练成就', '测试专长', 0, NULL)";
            
            entityManager.createNativeQuery(coachSql)
                .setParameter(1, userId.longValue())
                .setParameter(2, level)
                .executeUpdate();
        } catch (Exception e) {
            System.out.println("教练 " + username + " 可能已存在，跳过创建");
        }
    }

    private void createTestAdmin(String username, String realName, Campus campus, String role) {
        try {
            String userSql = "INSERT INTO user (username, password, real_name, phone, email, gender, age, role, campus_id, active, created_at, updated_at) " +
                           "VALUES (?, ?, ?, ?, ?, 'MALE', 35, ?, ?, true, NOW(), NOW())";
            
            entityManager.createNativeQuery(userSql)
                .setParameter(1, username)
                .setParameter(2, passwordEncoder.encode("123456"))
                .setParameter(3, realName)
                .setParameter(4, "1380000" + username.substring(username.length() - 4))
                .setParameter(5, username + "@test.com")
                .setParameter(6, role)
                .setParameter(7, campus.getId())
                .executeUpdate();
        } catch (Exception e) {
            System.out.println("管理员 " + username + " 可能已存在，跳过创建");
        }
    }

    private void createTestCoachStudentRelations() {
        // 此方法已存在，重用现有逻辑
    }

    private void createTestCourseBookings() {
        try {
            // 创建一些测试课程预约
            String sql = "INSERT INTO course_booking (student_id, coach_id, start_time, end_time, table_number, cost, status, notes, created_at, updated_at) " +
                       "SELECT s.id, c.id, DATE_ADD(NOW(), INTERVAL 1 DAY) + INTERVAL FLOOR(RAND() * 8 + 9) HOUR, " +
                       "DATE_ADD(NOW(), INTERVAL 1 DAY) + INTERVAL FLOOR(RAND() * 8 + 10) HOUR, " +
                       "CONCAT('T00', FLOOR(RAND() * 10 + 1)), " +
                       "CASE c.level WHEN 'SENIOR' THEN 200.00 WHEN 'INTERMEDIATE' THEN 150.00 ELSE 80.00 END, " +
                       "'PENDING', '测试课程预约', NOW(), NOW() " +
                       "FROM student s " +
                       "CROSS JOIN coach c " +
                       "WHERE s.id IN (SELECT id FROM user WHERE username LIKE 'student%') " +
                       "AND c.id IN (SELECT id FROM user WHERE username LIKE 'coach%') " +
                       "LIMIT 5";

            entityManager.createNativeQuery(sql).executeUpdate();
        } catch (Exception e) {
            System.out.println("创建测试课程预约失败: " + e.getMessage());
        }
    }

    private void createTestPaymentRecords() {
        try {
            String sql = "INSERT INTO payment_record (student_id, amount, payment_method, transaction_no, description, created_at, updated_at) " +
                       "SELECT s.id, 500.00, 'OFFLINE', CONCAT('TEST', UNIX_TIMESTAMP(), FLOOR(RAND() * 1000)), '测试充值记录', NOW(), NOW() " +
                       "FROM student s " +
                       "WHERE s.id IN (SELECT id FROM user WHERE username LIKE 'student%')";

            entityManager.createNativeQuery(sql).executeUpdate();
        } catch (Exception e) {
            System.out.println("创建测试支付记录失败: " + e.getMessage());
        }
    }

    @PostMapping("/create-competitions")
    @Transactional
    public ApiResponse<String> createCompetitions() {
        try {
            // 删除现有的比赛数据
            entityManager.createNativeQuery("DELETE FROM competition_registration").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM monthly_competition").executeUpdate();
            
            // 获取校区信息
            List<Campus> campuses = campusRepository.findAll();
            if (campuses.isEmpty()) {
                return ApiResponse.error("需要先创建校区数据");
            }
            
            Campus campus = campuses.get(0); // 使用第一个校区
            
            // 创建当前月份的比赛
            String sql1 = "INSERT INTO monthly_competition (name, description, campus_id, competition_date, registration_start_date, registration_end_date, registration_fee, max_participants, registration_open, created_at, updated_at) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
            
            // 8月比赛
            entityManager.createNativeQuery(sql1)
                .setParameter(1, "2025年8月校区月赛")
                .setParameter(2, "面向所有学员的月度比赛，按年龄分组进行")
                .setParameter(3, campus.getId())
                .setParameter(4, "2025-08-31 14:00:00")
                .setParameter(5, "2025-08-01 00:00:00")
                .setParameter(6, "2025-08-30 23:59:59")
                .setParameter(7, 50.00)
                .setParameter(8, 32)
                .setParameter(9, true)
                .executeUpdate();
            
            // 9月比赛
            entityManager.createNativeQuery(sql1)
                .setParameter(1, "2025年9月校区月赛")
                .setParameter(2, "9月份月度比赛")
                .setParameter(3, campus.getId())
                .setParameter(4, "2025-09-15 14:00:00")
                .setParameter(5, "2025-09-01 00:00:00")
                .setParameter(6, "2025-09-12 23:59:59")
                .setParameter(7, 50.00)
                .setParameter(8, 32)
                .setParameter(9, true)
                .executeUpdate();
            
            // 10月比赛
            entityManager.createNativeQuery(sql1)
                .setParameter(1, "2025年10月校区月赛")
                .setParameter(2, "10月份月度比赛")
                .setParameter(3, campus.getId())
                .setParameter(4, "2025-10-15 14:00:00")
                .setParameter(5, "2025-10-01 00:00:00")
                .setParameter(6, "2025-10-12 23:59:59")
                .setParameter(7, 50.00)
                .setParameter(8, 32)
                .setParameter(9, true)
                .executeUpdate();
            
            return ApiResponse.success("成功创建比赛数据！共创建了3个比赛");
        } catch (Exception e) {
            return ApiResponse.error("创建比赛数据失败: " + e.getMessage());
        }
    }

    private void createTestCompetitions(Campus campus) {
        try {
            String sql = "INSERT INTO monthly_competition (name, description, campus_id, competition_date, registration_start_date, registration_end_date, registration_fee, max_participants, registration_open, created_at, updated_at) " +
                       "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";

            entityManager.createNativeQuery(sql)
                .setParameter(1, "测试月度比赛")
                .setParameter(2, "测试用的月度比赛")
                .setParameter(3, campus.getId())
                .setParameter(4, "2025-09-30 14:00:00")
                .setParameter(5, "2025-09-01 00:00:00")
                .setParameter(6, "2025-09-25 23:59:59")
                .setParameter(7, 30.00)
                .setParameter(8, 50)
                .setParameter(9, true)
                .executeUpdate();
        } catch (Exception e) {
            System.out.println("创建测试比赛失败: " + e.getMessage());
        }
    }

    private void createTestEvaluations() {
        try {
            // 这个方法暂时留空，因为评价记录需要先有完成的课程预约
            System.out.println("暂未创建测试评价记录");
        } catch (Exception e) {
            System.out.println("创建测试评价失败: " + e.getMessage());
        }
    }

    @GetMapping("/check-competitions")
    public ApiResponse<?> checkCompetitions() {
        try {
            Map<String, Object> result = new HashMap<>();
            
            // 检查所有比赛
            List<MonthlyCompetition> allCompetitions = monthlyCompetitionRepository.findAll();
            result.put("totalCompetitions", allCompetitions.size());
            
            // 按校区分组
            Map<String, Object> competitionsByCampus = new HashMap<>();
            for (MonthlyCompetition comp : allCompetitions) {
                String campusName = comp.getCampus().getName();
                List<Map<String, Object>> campusComps = (List<Map<String, Object>>) 
                    competitionsByCampus.getOrDefault(campusName, new ArrayList<>());
                
                Map<String, Object> compInfo = new HashMap<>();
                compInfo.put("id", comp.getId());
                compInfo.put("name", comp.getName());
                compInfo.put("competitionDate", comp.getCompetitionDate());
                compInfo.put("registrationOpen", comp.getRegistrationOpen());
                compInfo.put("registrationStartDate", comp.getRegistrationStartDate());
                compInfo.put("registrationEndDate", comp.getRegistrationEndDate());
                compInfo.put("registrationFee", comp.getRegistrationFee());
                compInfo.put("maxParticipants", comp.getMaxParticipants());
                
                campusComps.add(compInfo);
                competitionsByCampus.put(campusName, campusComps);
            }
            
            result.put("competitionsByCampus", competitionsByCampus);
            
            // 检查开放报名的比赛
            List<MonthlyCompetition> openCompetitions = monthlyCompetitionRepository
                .findUpcomingOpenCompetitions(LocalDateTime.now());
            result.put("openCompetitions", openCompetitions.size());
            
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("检查比赛数据失败: " + e.getMessage());
        }
    }

    @PostMapping("/create-evaluation-records")
    @Transactional
    public ApiResponse<String> createEvaluationRecords() {
        try {
            // 查找当前用户的已确认预约，创建一些已完成的课程并生成评价记录
            List<Object[]> bookingData = entityManager.createNativeQuery(
                "SELECT cb.id, cb.student_id, cb.coach_id, cb.start_time, cb.end_time " +
                "FROM course_booking cb " +
                "WHERE cb.status = 'CONFIRMED' " +
                "AND cb.start_time < NOW() " +
                "LIMIT 5"
            ).getResultList();
            
            if (bookingData.isEmpty()) {
                // 如果没有已确认的预约，创建一些测试预约
                createTestBookingsForEvaluation();
                // 重新查询
                bookingData = entityManager.createNativeQuery(
                    "SELECT cb.id, cb.student_id, cb.coach_id, cb.start_time, cb.end_time " +
                    "FROM course_booking cb " +
                    "WHERE cb.status = 'CONFIRMED' " +
                    "AND cb.start_time < NOW() " +
                    "LIMIT 5"
                ).getResultList();
            }
            
            int completedCount = 0;
            int evaluationCount = 0;
            
            for (Object[] row : bookingData) {
                Long bookingId = ((Number) row[0]).longValue();
                
                // 将预约状态更新为已完成
                entityManager.createNativeQuery(
                    "UPDATE course_booking SET status = 'COMPLETED' WHERE id = ?"
                ).setParameter(1, bookingId).executeUpdate();
                
                completedCount++;
                
                // 检查是否已有评价记录
                Number existingCount = (Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM course_evaluation WHERE booking_id = ?"
                ).setParameter(1, bookingId).getSingleResult();
                
                if (existingCount.intValue() == 0) {
                    // 创建评价记录
                    entityManager.createNativeQuery(
                        "INSERT INTO course_evaluation (booking_id, created_at, updated_at) VALUES (?, NOW(), NOW())"
                    ).setParameter(1, bookingId).executeUpdate();
                    
                    evaluationCount++;
                }
            }
            
            return ApiResponse.success(
                String.format("成功创建了 %d 个已完成课程和 %d 个待评价记录！" +
                            "现在可以在个人中心查看待评价课程了。", 
                            completedCount, evaluationCount)
            );
            
        } catch (Exception e) {
            return ApiResponse.error("创建评价记录失败: " + e.getMessage());
        }
    }
    
    private void createTestBookingsForEvaluation() {
        try {
            // 创建一些过去时间的课程预约用于测试评价功能
            String sql = "INSERT INTO course_booking (student_id, coach_id, start_time, end_time, table_number, cost, status, notes, created_at, updated_at) " +
                       "SELECT s.id, c.id, " +
                       "DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7 + 1) DAY) + INTERVAL FLOOR(RAND() * 6 + 9) HOUR, " +
                       "DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7 + 1) DAY) + INTERVAL FLOOR(RAND() * 6 + 10) HOUR, " +
                       "CONCAT('T00', FLOOR(RAND() * 10 + 1)), " +
                       "CASE c.level WHEN 'SENIOR' THEN 200.00 WHEN 'INTERMEDIATE' THEN 150.00 ELSE 80.00 END, " +
                       "'CONFIRMED', '测试已完成课程（用于评价）', NOW(), NOW() " +
                       "FROM student s " +
                       "CROSS JOIN coach c " +
                       "WHERE s.id IN (SELECT id FROM user WHERE username LIKE 'student%') " +
                       "AND c.id IN (SELECT id FROM user WHERE username LIKE 'coach%') " +
                       "LIMIT 3";

            entityManager.createNativeQuery(sql).executeUpdate();
        } catch (Exception e) {
            System.out.println("创建测试预约失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/create-coach-pending-evaluations")
    @Transactional
    public ApiResponse<String> createCoachPendingEvaluations() {
        try {
            System.out.println("=== 开始为教练创建待评价课程 ===");
            
            int bookingCount = 0;
            int evaluationCount = 0;
            
            // 1. 创建一些过去的已完成课程预约
            createCoachTestBookings();
            
            // 2. 查找所有确认状态的过去课程预约
            @SuppressWarnings("unchecked")
            List<Object[]> bookingData = entityManager.createNativeQuery(
                "SELECT cb.id, cb.coach_id, cb.student_id, s.real_name " +
                "FROM course_booking cb " +
                "JOIN student s ON cb.student_id = s.id " +
                "WHERE cb.status = 'CONFIRMED' " +
                "AND cb.end_time < NOW() " +
                "ORDER BY cb.end_time DESC " +
                "LIMIT 5"
            ).getResultList();
            
            System.out.println("找到 " + bookingData.size() + " 个过去的确认预约");
            
            for (Object[] row : bookingData) {
                Long bookingId = ((Number) row[0]).longValue();
                Long coachId = ((Number) row[1]).longValue();
                Long studentId = ((Number) row[2]).longValue();
                String studentName = (String) row[3];
                
                System.out.println(String.format("处理预约ID: %d, 教练ID: %d, 学员: %s", 
                    bookingId, coachId, studentName));
                
                // 将预约状态更新为已完成
                entityManager.createNativeQuery(
                    "UPDATE course_booking SET status = 'COMPLETED', updated_at = NOW() WHERE id = ?"
                ).setParameter(1, bookingId).executeUpdate();
                
                bookingCount++;
                
                // 检查是否已有评价记录
                Number existingCount = (Number) entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM course_evaluation WHERE booking_id = ?"
                ).setParameter(1, bookingId).getSingleResult();
                
                if (existingCount.intValue() == 0) {
                    // 创建评价记录，学员已评价，教练待评价
                    entityManager.createNativeQuery(
                        "INSERT INTO course_evaluation (booking_id, student_evaluation, student_rating, created_at, updated_at) " +
                        "VALUES (?, '学员评价：教练很专业，课程质量很好！', ?, NOW(), NOW())"
                    ).setParameter(1, bookingId)
                     .setParameter(2, (int)(Math.random() * 2) + 4) // 4-5分评价
                     .executeUpdate();
                    
                    evaluationCount++;
                    System.out.println("为预约 " + bookingId + " 创建了评价记录（教练待评价）");
                } else {
                    // 如果已有评价记录，但教练还没评价，则添加学员评价
                    entityManager.createNativeQuery(
                        "UPDATE course_evaluation SET " +
                        "student_evaluation = '学员评价：教练很专业，课程质量很好！', " +
                        "student_rating = ?, " +
                        "updated_at = NOW() " +
                        "WHERE booking_id = ? AND coach_evaluation IS NULL"
                    ).setParameter(1, (int)(Math.random() * 2) + 4) // 4-5分评价
                     .setParameter(2, bookingId)
                     .executeUpdate();
                    
                    System.out.println("更新了预约 " + bookingId + " 的评价记录");
                }
            }
            
            System.out.println("=== 教练待评价课程创建完成 ===");
            
            return ApiResponse.success(
                String.format("成功创建了 %d 个已完成课程和 %d 个教练待评价记录！" +
                            "教练现在可以在学员管理->课程评价中查看待评价课程了。", 
                            bookingCount, evaluationCount)
            );
            
        } catch (Exception e) {
            System.err.println("创建教练待评价记录失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("创建教练待评价记录失败: " + e.getMessage());
        }
    }
    
    private void createCoachTestBookings() {
        try {
            // 创建一些过去时间的课程预约，专门用于教练评价测试
            // 基于已存在的师生关系创建预约
            String sql = "INSERT INTO course_booking (student_id, coach_id, start_time, end_time, table_number, cost, status, remarks, created_at, updated_at) " +
                       "SELECT csr.student_id, csr.coach_id, " +
                       "DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 5 + 1) DAY) + INTERVAL FLOOR(RAND() * 6 + 9) HOUR, " +
                       "DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 5 + 1) DAY) + INTERVAL FLOOR(RAND() * 6 + 10) HOUR, " +
                       "CONCAT('T00', FLOOR(RAND() * 10 + 1)), " +
                       "CASE c.level WHEN 'SENIOR' THEN 200.00 WHEN 'INTERMEDIATE' THEN 150.00 ELSE 80.00 END, " +
                       "'CONFIRMED', '测试已完成课程（用于教练评价）', NOW(), NOW() " +
                       "FROM coach_student_relation csr " +
                       "JOIN coach c ON csr.coach_id = c.id " +
                       "WHERE csr.status = 'APPROVED' " +
                       "AND NOT EXISTS (SELECT 1 FROM course_booking cb WHERE cb.student_id = csr.student_id AND cb.coach_id = csr.coach_id AND cb.status = 'COMPLETED') " +
                       "LIMIT 6";

            int created = entityManager.createNativeQuery(sql).executeUpdate();
            System.out.println("基于师生关系创建了 " + created + " 个测试课程预约");
        } catch (Exception e) {
            System.out.println("创建教练测试预约失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/create-coach-student-relations")
    @Transactional
    public ApiResponse<String> createCoachStudentRelations() {
        try {
            System.out.println("=== 开始创建师生关系测试数据 ===");
            
            int relationCount = 0;
            
            // 查找所有教练和学员
            @SuppressWarnings("unchecked")
            List<Object[]> coachData = entityManager.createNativeQuery(
                "SELECT u.id, u.username, u.real_name FROM user u WHERE u.role = 'COACH' LIMIT 5"
            ).getResultList();
            
            @SuppressWarnings("unchecked")
            List<Object[]> studentData = entityManager.createNativeQuery(
                "SELECT u.id, u.username, u.real_name FROM user u WHERE u.role = 'STUDENT' LIMIT 5"
            ).getResultList();
            
            System.out.println("找到 " + coachData.size() + " 个教练");
            System.out.println("找到 " + studentData.size() + " 个学员");
            
            // 为每个教练分配2-3个学员
            for (Object[] coach : coachData) {
                Long coachId = ((Number) coach[0]).longValue();
                String coachUsername = (String) coach[1];
                String coachName = (String) coach[2];
                
                System.out.println("为教练 " + coachName + " (" + coachUsername + ") 创建师生关系");
                
                // 为每个教练分配前3个学员
                for (int i = 0; i < Math.min(3, studentData.size()); i++) {
                    Object[] student = studentData.get(i);
                    Long studentId = ((Number) student[0]).longValue();
                    String studentUsername = (String) student[1];
                    String studentName = (String) student[2];
                    
                    // 检查是否已存在关系
                    Number existingCount = (Number) entityManager.createNativeQuery(
                        "SELECT COUNT(*) FROM coach_student_relation WHERE coach_id = ? AND student_id = ?"
                    ).setParameter(1, coachId).setParameter(2, studentId).getSingleResult();
                    
                    if (existingCount.intValue() == 0) {
                        // 创建师生关系
                        entityManager.createNativeQuery(
                            "INSERT INTO coach_student_relation (coach_id, student_id, status, created_at, updated_at) " +
                            "VALUES (?, ?, 'APPROVED', NOW(), NOW())"
                        ).setParameter(1, coachId).setParameter(2, studentId).executeUpdate();
                        
                        relationCount++;
                        System.out.println("创建师生关系: " + coachName + " -> " + studentName);
                    } else {
                        System.out.println("师生关系已存在: " + coachName + " -> " + studentName);
                    }
                }
            }
            
            System.out.println("=== 师生关系创建完成 ===");
            
            return ApiResponse.success(
                String.format("成功创建了 %d 个师生关系！现在教练可以查看学员的课表和评价记录了。", relationCount)
            );
            
        } catch (Exception e) {
            System.err.println("创建师生关系失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("创建师生关系失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/create-coach-working-time-table")
    @Transactional
    public ApiResponse<String> createCoachWorkingTimeTable() {
        try {
            System.out.println("=== 开始创建教练工作时间表 ===");
            
            // 1. 创建表结构
            String createTableSQL = "CREATE TABLE IF NOT EXISTS coach_working_time (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "coach_id BIGINT NOT NULL," +
                "day_of_week INT NOT NULL COMMENT '星期几：1=周一, 2=周二, ..., 7=周日'," +
                "start_time TIME NOT NULL COMMENT '开始时间'," +
                "end_time TIME NOT NULL COMMENT '结束时间'," +
                "is_available BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否可预约'," +
                "specific_date DATE NULL COMMENT '特定日期设置（覆盖周重复设置）'," +
                "remarks VARCHAR(500) NULL COMMENT '备注（如：临时调整、休假等）'," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "FOREIGN KEY (coach_id) REFERENCES coach(id) ON DELETE CASCADE," +
                "INDEX idx_coach_day (coach_id, day_of_week)," +
                "INDEX idx_coach_date (coach_id, specific_date)," +
                "INDEX idx_available (is_available)" +
                ") COMMENT='教练工作时间安排表'";
            
            entityManager.createNativeQuery(createTableSQL).executeUpdate();
            System.out.println("教练工作时间表创建成功");
            
            // 2. 为现有教练创建默认工作时间安排
            int totalInserted = 0;
            
            // 获取所有教练
            @SuppressWarnings("unchecked")
            List<Object[]> coaches = entityManager.createNativeQuery(
                "SELECT id, real_name FROM coach"
            ).getResultList();
            
            System.out.println("找到 " + coaches.size() + " 个教练");
            
            for (Object[] coach : coaches) {
                Long coachId = ((Number) coach[0]).longValue();
                String coachName = (String) coach[1];
                
                System.out.println("为教练 " + coachName + " 创建默认工作时间");
                
                // 为每个教练创建标准工作时间：周一到周日，9:00-21:00，中午休息
                String[] timeSlots = {
                    "09:00:00", "10:00:00", "11:00:00",      // 上午
                    "14:00:00", "15:00:00", "16:00:00", "17:00:00",  // 下午
                    "19:00:00", "20:00:00"                    // 晚上
                };
                
                for (int dayOfWeek = 1; dayOfWeek <= 7; dayOfWeek++) {
                    for (String startTime : timeSlots) {
                        // 检查是否已存在
                        Number existingCount = (Number) entityManager.createNativeQuery(
                            "SELECT COUNT(*) FROM coach_working_time WHERE coach_id = ? AND day_of_week = ? AND start_time = ? AND specific_date IS NULL"
                        ).setParameter(1, coachId)
                         .setParameter(2, dayOfWeek)
                         .setParameter(3, startTime)
                         .getSingleResult();
                        
                        if (existingCount.intValue() == 0) {
                            // 计算结束时间（开始时间+1小时）
                            String endTime = entityManager.createNativeQuery(
                                "SELECT ADDTIME(?, '01:00:00')"
                            ).setParameter(1, startTime).getSingleResult().toString();
                            
                            // 插入工作时间记录
                            entityManager.createNativeQuery(
                                "INSERT INTO coach_working_time (coach_id, day_of_week, start_time, end_time, is_available, remarks, created_at, updated_at) " +
                                "VALUES (?, ?, ?, ?, TRUE, '系统默认工作时间', NOW(), NOW())"
                            ).setParameter(1, coachId)
                             .setParameter(2, dayOfWeek)
                             .setParameter(3, startTime)
                             .setParameter(4, endTime)
                             .executeUpdate();
                            
                            totalInserted++;
                        }
                    }
                }
            }
            
            System.out.println("=== 教练工作时间表创建完成 ===");
            
            return ApiResponse.success(
                String.format("教练工作时间表创建成功！为 %d 个教练创建了 %d 个工作时间段。", 
                    coaches.size(), totalInserted)
            );
            
        } catch (Exception e) {
            System.err.println("创建教练工作时间表失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("创建教练工作时间表失败: " + e.getMessage());
        }
    }

    // =============== 评价功能测试工具 ===============
    
    /**
     * 批量完成过期的课程（用于测试评价功能）
     */
    @PostMapping("/complete-past-bookings")
    public ApiResponse<?> completePastBookings() {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // 查找所有已过期但未完成的确认预约
            List<CourseBooking> pastBookings = courseBookingRepository.findAll().stream()
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED && 
                                 booking.getEndTime().isBefore(now))
                .collect(Collectors.toList());
            
            int completedCount = 0;
            int evaluationCount = 0;
            
            for (CourseBooking booking : pastBookings) {
                // 更新预约状态为已完成
                booking.setStatus(BookingStatus.COMPLETED);
                courseBookingRepository.save(booking);
                completedCount++;
                
                // 创建评价记录
                if (!courseEvaluationRepository.existsByBookingId(booking.getId())) {
                    CourseEvaluation evaluation = new CourseEvaluation();
                    evaluation.setBooking(booking);
                    courseEvaluationRepository.save(evaluation);
                    evaluationCount++;
                }
            }
            
            String message = String.format("批量完成成功：完成了 %d 个过期课程，创建了 %d 个评价记录", 
                completedCount, evaluationCount);
            
            Map<String, Object> result = new HashMap<>();
            result.put("completedBookings", completedCount);
            result.put("createdEvaluations", evaluationCount);
            result.put("pastBookings", pastBookings);
            return ApiResponse.success(message, result);
        } catch (Exception e) {
            System.err.println("批量完成过期课程失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("批量完成过期课程失败: " + e.getMessage());
        }
    }
    
    /**
     * 创建测试用的已完成课程（用于测试评价功能）
     */
    @PostMapping("/create-test-completed-bookings")
    public ApiResponse<?> createTestCompletedBookings(@RequestParam(defaultValue = "3") int count) {
        try {
            List<Student> students = studentRepository.findAll();
            List<Coach> coaches = coachRepository.findAll();
            
            if (students.isEmpty() || coaches.isEmpty()) {
                return ApiResponse.error("系统中没有学员或教练数据");
            }
            
            List<CourseBooking> testBookings = new ArrayList<>();
            Random random = new Random();
            
            for (int i = 0; i < count; i++) {
                Student student = students.get(random.nextInt(students.size()));
                Coach coach = coaches.get(random.nextInt(coaches.size()));
                
                CourseBooking booking = new CourseBooking();
                booking.setStudent(student);
                booking.setCoach(coach);
                booking.setTableNumber("测试台" + (i + 1));
                
                // 设置过去的时间
                LocalDateTime pastTime = LocalDateTime.now().minusDays(i + 1).withHour(10).withMinute(0);
                booking.setStartTime(pastTime);
                booking.setEndTime(pastTime.plusHours(1));
                
                booking.setStatus(BookingStatus.COMPLETED);
                booking.setCost(new BigDecimal("100.00"));
                booking.setRemarks("测试用已完成课程");
                
                booking = courseBookingRepository.save(booking);
                testBookings.add(booking);
                
                // 创建对应的评价记录
                CourseEvaluation evaluation = new CourseEvaluation();
                evaluation.setBooking(booking);
                courseEvaluationRepository.save(evaluation);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("createdBookings", testBookings.size());
            result.put("bookings", testBookings);
            return ApiResponse.success("测试数据创建成功", result);
        } catch (Exception e) {
            System.err.println("创建测试数据失败: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("创建测试数据失败: " + e.getMessage());
        }
    }
} 
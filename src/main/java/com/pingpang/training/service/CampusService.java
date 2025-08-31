package com.pingpang.training.service;

import com.pingpang.training.entity.Campus;
import com.pingpang.training.entity.User;
import com.pingpang.training.enums.UserRole;
import com.pingpang.training.repository.CampusRepository;
import com.pingpang.training.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CampusService {

    @Autowired
    private CampusRepository campusRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemLogService systemLogService;

    @Autowired
    private SystemMessageService systemMessageService;

    public List<Campus> getAllActiveCampuses() {
        return campusRepository.findByActiveTrue();
    }

    public Optional<Campus> getMainCampus() {
        return campusRepository.findByIsMainCampusTrue();
    }

    public Optional<Campus> findById(Long id) {
        return campusRepository.findById(id);
    }

    public Campus createCampus(Campus campus, User creator) {
        // 校验校区名称是否已存在
        if (campusRepository.existsByName(campus.getName())) {
            throw new RuntimeException("校区名称已存在");
        }

        campus.setActive(true);
        campus.setCreatedAt(LocalDateTime.now());
        campus.setUpdatedAt(LocalDateTime.now());

        // 如果是第一个校区，自动设为主校区
        if (campusRepository.count() == 0) {
            campus.setIsMainCampus(true);
            campus.setAdmin(creator); // 创建者成为主校区管理员
        }

        Campus savedCampus = campusRepository.save(campus);

        // 记录系统日志
        systemLogService.log(creator, "创建校区", "创建校区：" + campus.getName());

        return savedCampus;
    }

    public Campus updateCampus(Campus campus, User updater) {
        Campus existingCampus = campusRepository.findById(campus.getId())
                .orElseThrow(() -> new RuntimeException("校区不存在"));

        // 如果名称发生变化，检查新名称是否已存在
        if (!existingCampus.getName().equals(campus.getName()) && 
            campusRepository.existsByName(campus.getName())) {
            throw new RuntimeException("校区名称已存在");
        }

        // 保存原来的主校区状态
        boolean wasMainCampus = existingCampus.getIsMainCampus();

        existingCampus.setName(campus.getName());
        existingCampus.setAddress(campus.getAddress());
        existingCampus.setContactPerson(campus.getContactPerson());
        existingCampus.setContactPhone(campus.getContactPhone());
        existingCampus.setContactEmail(campus.getContactEmail());
        existingCampus.setUpdatedAt(LocalDateTime.now());

        // 如果要设置为主校区
        if (campus.getIsMainCampus() != null && campus.getIsMainCampus() && !wasMainCampus) {
            setAsMainCampus(existingCampus, updater);
        }

        Campus savedCampus = campusRepository.save(existingCampus);

        // 记录系统日志
        systemLogService.log(updater, "更新校区", "更新校区：" + campus.getName());

        return savedCampus;
    }

    public void deleteCampus(Long id, User deleter) {
        Campus campus = campusRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("校区不存在"));

        if (campus.getIsMainCampus()) {
            throw new RuntimeException("不能删除主校区，请先将主校区转移到其他校区");
        }

        // 检查校区是否有用户
        long userCount = campusRepository.countUsersByCampusId(id);
        if (userCount > 0) {
            throw new RuntimeException("该校区还有用户，请先转移用户到其他校区");
        }

        campus.setActive(false);
        campus.setUpdatedAt(LocalDateTime.now());
        campusRepository.save(campus);

        // 记录系统日志
        systemLogService.log(deleter, "删除校区", "删除校区：" + campus.getName());
    }

    /**
     * 设置主校区
     */
    public Campus setAsMainCampus(Long campusId, User operator) {
        Campus campus = campusRepository.findById(campusId)
                .orElseThrow(() -> new RuntimeException("校区不存在"));
        
        return setAsMainCampus(campus, operator);
    }

    /**
     * 设置主校区的内部方法
     */
    private Campus setAsMainCampus(Campus campus, User operator) {
        // 清除所有校区的主校区标记
        List<Campus> allCampuses = campusRepository.findAll();
        for (Campus c : allCampuses) {
            if (c.getIsMainCampus()) {
                c.setIsMainCampus(false);
                campusRepository.save(c);
                
                // 通知原主校区管理员
                if (c.getAdmin() != null) {
                    systemMessageService.sendMessage(null, c.getAdmin(), 
                        com.pingpang.training.enums.MessageType.ADMIN_NOTIFICATION,
                        "主校区变更通知", 
                        "校区 " + c.getName() + " 不再是主校区", null);
                }
            }
        }

        // 设置新的主校区
        campus.setIsMainCampus(true);
        
        // 如果新主校区没有管理员，检查是否有超级管理员可以担任
        if (campus.getAdmin() == null) {
            if (operator.getRole() == UserRole.SUPER_ADMIN) {
                campus.setAdmin(operator);
                operator.setCampus(campus);
                userRepository.save(operator);
                
                systemMessageService.sendMessage(null, operator, 
                    com.pingpang.training.enums.MessageType.ADMIN_NOTIFICATION,
                    "主校区管理员任命", 
                    "您已被任命为主校区 " + campus.getName() + " 的管理员", null);
            }
        }

        Campus savedCampus = campusRepository.save(campus);

        // 通知新主校区管理员
        if (campus.getAdmin() != null && !campus.getAdmin().getId().equals(operator.getId())) {
            systemMessageService.sendMessage(null, campus.getAdmin(), 
                com.pingpang.training.enums.MessageType.ADMIN_NOTIFICATION,
                "主校区设置通知", 
                "校区 " + campus.getName() + " 已被设置为主校区", null);
        }

        // 记录系统日志
        systemLogService.log(operator, "SET_MAIN_CAMPUS", 
            "设置主校区: " + campus.getName());

        return savedCampus;
    }

    /**
     * 转移主校区
     */
    public void transferMainCampus(Long fromCampusId, Long toCampusId, User operator) {
        Campus fromCampus = campusRepository.findById(fromCampusId)
                .orElseThrow(() -> new RuntimeException("源校区不存在"));
        Campus toCampus = campusRepository.findById(toCampusId)
                .orElseThrow(() -> new RuntimeException("目标校区不存在"));

        if (!fromCampus.getIsMainCampus()) {
            throw new RuntimeException("源校区不是主校区");
        }

        if (toCampus.getIsMainCampus()) {
            throw new RuntimeException("目标校区已经是主校区");
        }

        // 转移主校区标记
        fromCampus.setIsMainCampus(false);
        toCampus.setIsMainCampus(true);

        // 如果目标校区没有管理员，将当前操作员设为管理员
        if (toCampus.getAdmin() == null && operator.getRole() == UserRole.SUPER_ADMIN) {
            toCampus.setAdmin(operator);
            operator.setCampus(toCampus);
            userRepository.save(operator);
        }

        campusRepository.save(fromCampus);
        campusRepository.save(toCampus);

        // 发送通知
        if (fromCampus.getAdmin() != null) {
            systemMessageService.sendMessage(null, fromCampus.getAdmin(), 
                com.pingpang.training.enums.MessageType.ADMIN_NOTIFICATION,
                "主校区转移通知", 
                "主校区已从 " + fromCampus.getName() + " 转移到 " + toCampus.getName(), null);
        }

        if (toCampus.getAdmin() != null) {
            systemMessageService.sendMessage(null, toCampus.getAdmin(), 
                com.pingpang.training.enums.MessageType.ADMIN_NOTIFICATION,
                "主校区任命通知", 
                "您的校区 " + toCampus.getName() + " 已被设置为主校区", null);
        }

        // 记录系统日志
        systemLogService.log(operator, "TRANSFER_MAIN_CAMPUS", 
            "转移主校区：从 " + fromCampus.getName() + " 到 " + toCampus.getName());
    }

    /**
     * 获取主校区管理员
     */
    public User getMainCampusAdmin() {
        Optional<Campus> mainCampusOpt = getMainCampus();
        if (mainCampusOpt.isPresent() && mainCampusOpt.get().getAdmin() != null) {
            return mainCampusOpt.get().getAdmin();
        }
        
        // 如果主校区没有管理员，返回第一个超级管理员
        List<User> superAdmins = userRepository.findByRole(UserRole.SUPER_ADMIN);
        if (!superAdmins.isEmpty()) {
            return superAdmins.get(0);
        }
        
        throw new RuntimeException("系统中没有可用的管理员");
    }

    /**
     * 校验主校区完整性
     */
    public void validateMainCampusIntegrity() {
        Optional<Campus> mainCampusOpt = campusRepository.findByIsMainCampusTrue();
        
        if (!mainCampusOpt.isPresent()) {
            // 没有主校区，将第一个校区设为主校区
            List<Campus> allCampuses = campusRepository.findByActiveTrue();
            if (!allCampuses.isEmpty()) {
                Campus firstCampus = allCampuses.get(0);
                firstCampus.setIsMainCampus(true);
                
                // 如果没有管理员，设置第一个超级管理员
                if (firstCampus.getAdmin() == null) {
                    List<User> superAdmins = userRepository.findByRole(UserRole.SUPER_ADMIN);
                    if (!superAdmins.isEmpty()) {
                        firstCampus.setAdmin(superAdmins.get(0));
                    }
                }
                
                campusRepository.save(firstCampus);
            }
        } else {
            // 已经有主校区，无需处理
        }
    }

    public Campus save(Campus campus) {
        return campusRepository.save(campus);
    }

    public List<Campus> findAll() {
        return campusRepository.findAll();
    }

    public long count() {
        return campusRepository.count();
    }

    public boolean existsById(Long id) {
        return campusRepository.existsById(id);
    }

    public boolean existsByName(String name) {
        return campusRepository.existsByName(name);
    }
} 
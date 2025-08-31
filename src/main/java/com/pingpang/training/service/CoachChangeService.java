package com.pingpang.training.service;

import com.pingpang.training.entity.*;
import com.pingpang.training.enums.ApprovalStatus;
import com.pingpang.training.enums.MessageType;
import com.pingpang.training.enums.UserRole;
import com.pingpang.training.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class CoachChangeService {

    @Autowired
    private CoachChangeRequestRepository coachChangeRequestRepository;

    @Autowired
    private CoachStudentRelationRepository relationRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CoachRepository coachRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemMessageService systemMessageService;

    @Autowired
    private SystemLogService systemLogService;

    /**
     * 学员申请更换教练
     */
    public CoachChangeRequest submitCoachChangeRequest(Long studentId, Long newCoachId, String reason) {
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("学员不存在"));

        Coach newCoach = coachRepository.findById(newCoachId)
            .orElseThrow(() -> new RuntimeException("新教练不存在"));

        // 验证学员和新教练在同一校区
        if (!student.getCampus().getId().equals(newCoach.getCampus().getId())) {
            throw new RuntimeException("只能选择本校区的教练");
        }

        // 获取学员当前的教练关系
        List<CoachStudentRelation> currentRelations = relationRepository.findApprovedByStudentId(studentId);
        if (currentRelations.isEmpty()) {
            throw new RuntimeException("学员当前没有教练，无法申请更换");
        }

        // 检查是否已有待处理的申请
        List<CoachChangeRequest> pendingRequests = coachChangeRequestRepository.findPendingRequestsByStudentId(studentId);
        if (!pendingRequests.isEmpty()) {
            throw new RuntimeException("您有待处理的教练更换申请，请等待审核完成");
        }

        // 检查月度申请限制（假设一个月最多申请3次）
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        Long requestCount = coachChangeRequestRepository.countByStudentIdAndCreatedAtAfter(studentId, oneMonthAgo);
        if (requestCount >= 3) {
            throw new RuntimeException("本月申请次数已达上限（3次），请下月再试");
        }

        // 选择第一个当前教练（实际应用中可能需要让学员选择要更换哪个教练）
        Coach currentCoach = currentRelations.get(0).getCoach();

        // 验证不能选择当前已有的教练
        boolean isCurrentCoach = currentRelations.stream()
            .anyMatch(relation -> relation.getCoach().getId().equals(newCoachId));
        if (isCurrentCoach) {
            throw new RuntimeException("该教练已经是您的教练了");
        }

        // 获取校区管理员
        User campusAdmin = getCampusAdmin(student.getCampus().getId());

        // 创建更换申请
        CoachChangeRequest request = new CoachChangeRequest(student, currentCoach, newCoach, campusAdmin, reason);
        CoachChangeRequest savedRequest = coachChangeRequestRepository.save(request);

        // 发送消息通知三方
        systemMessageService.sendCoachChangeRequestMessage(student, currentCoach, newCoach, campusAdmin);

        // 记录系统日志
        systemLogService.log(student, "COACH_CHANGE_REQUEST", 
            "申请更换教练：从 " + currentCoach.getRealName() + " 到 " + newCoach.getRealName());

        return savedRequest;
    }

    /**
     * 当前教练审核
     */
    public CoachChangeRequest currentCoachApproval(Long requestId, Long coachId, boolean approved, String comment) {
        CoachChangeRequest request = coachChangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("申请不存在"));

        if (!request.getCurrentCoach().getId().equals(coachId)) {
            throw new RuntimeException("无权限审核该申请");
        }

        if (request.getCurrentCoachApproval() != ApprovalStatus.PENDING) {
            throw new RuntimeException("该申请已被审核过");
        }

        request.setCurrentCoachApproval(approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
        request.setCurrentCoachComment(comment);

        // 如果被拒绝，直接设置最终状态
        if (!approved) {
            request.setFinalStatus(ApprovalStatus.REJECTED);
            request.setProcessedAt(LocalDateTime.now());
        } else {
            // 检查是否所有审核都通过
            updateFinalStatus(request);
        }

        CoachChangeRequest savedRequest = coachChangeRequestRepository.save(request);

        // 发送结果通知
        String result = approved ? "同意" : "拒绝";
        systemMessageService.sendMessage(request.getCurrentCoach(), request.getStudent(), 
            MessageType.COACH_CHANGE, "教练更换申请审核结果", 
            "您的教练 " + request.getCurrentCoach().getRealName() + " " + result + " 了您的更换申请。" + 
            (comment != null ? "评论：" + comment : ""), requestId);

        // 记录系统日志
        systemLogService.log(request.getCurrentCoach(), "COACH_CHANGE_APPROVAL", 
            result + "学员 " + request.getStudent().getRealName() + " 的教练更换申请");

        return savedRequest;
    }

    /**
     * 新教练审核
     */
    public CoachChangeRequest newCoachApproval(Long requestId, Long coachId, boolean approved, String comment) {
        CoachChangeRequest request = coachChangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("申请不存在"));

        if (!request.getNewCoach().getId().equals(coachId)) {
            throw new RuntimeException("无权限审核该申请");
        }

        if (request.getNewCoachApproval() != ApprovalStatus.PENDING) {
            throw new RuntimeException("该申请已被审核过");
        }

        request.setNewCoachApproval(approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
        request.setNewCoachComment(comment);

        // 如果被拒绝，直接设置最终状态
        if (!approved) {
            request.setFinalStatus(ApprovalStatus.REJECTED);
            request.setProcessedAt(LocalDateTime.now());
        } else {
            // 检查是否所有审核都通过
            updateFinalStatus(request);
        }

        CoachChangeRequest savedRequest = coachChangeRequestRepository.save(request);

        // 发送结果通知
        String result = approved ? "同意" : "拒绝";
        systemMessageService.sendMessage(request.getNewCoach(), request.getStudent(),
            MessageType.COACH_CHANGE, "教练申请审核结果",
            "教练 " + request.getNewCoach().getRealName() + " " + result + " 了您选择Ta作为新教练的申请。" +
            (comment != null ? "评论：" + comment : ""), requestId);

        // 记录系统日志
        systemLogService.log(request.getNewCoach(), "COACH_CHANGE_APPROVAL",
            result + "学员 " + request.getStudent().getRealName() + " 的教练选择申请");

        return savedRequest;
    }

    /**
     * 管理员审核
     */
    public CoachChangeRequest adminApproval(Long requestId, Long adminId, boolean approved, String comment) {
        CoachChangeRequest request = coachChangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("申请不存在"));

        User admin = userRepository.findById(adminId)
            .orElseThrow(() -> new RuntimeException("管理员不存在"));

        // 权限检查
        if (admin.getRole() == UserRole.CAMPUS_ADMIN && 
            !admin.getCampus().getId().equals(request.getStudent().getCampus().getId())) {
            throw new RuntimeException("无权限审核其他校区的申请");
        }

        if (request.getAdminApproval() != ApprovalStatus.PENDING) {
            throw new RuntimeException("该申请已被审核过");
        }

        request.setAdminApproval(approved ? ApprovalStatus.APPROVED : ApprovalStatus.REJECTED);
        request.setAdminComment(comment);

        // 如果被拒绝，直接设置最终状态
        if (!approved) {
            request.setFinalStatus(ApprovalStatus.REJECTED);
            request.setProcessedAt(LocalDateTime.now());
        } else {
            // 检查是否所有审核都通过
            updateFinalStatus(request);
        }

        CoachChangeRequest savedRequest = coachChangeRequestRepository.save(request);

        // 如果最终通过，执行教练更换
        if (savedRequest.getFinalStatus() == ApprovalStatus.APPROVED) {
            executeCoachChange(savedRequest);
        }

        // 发送结果通知
        String result = approved ? "同意" : "拒绝";
        systemMessageService.sendMessage(admin, request.getStudent(),
            MessageType.COACH_CHANGE, "教练更换申请最终审核结果",
            "管理员 " + admin.getRealName() + " " + result + " 了您的教练更换申请。" +
            (savedRequest.getFinalStatus() == ApprovalStatus.APPROVED ? "教练更换已生效。" : "") +
            (comment != null ? "评论：" + comment : ""), requestId);

        // 记录系统日志
        systemLogService.log(admin, "COACH_CHANGE_ADMIN_APPROVAL",
            result + "学员 " + request.getStudent().getRealName() + " 的教练更换申请");

        return savedRequest;
    }

    /**
     * 更新最终状态
     */
    private void updateFinalStatus(CoachChangeRequest request) {
        if (request.isAllApproved()) {
            request.setFinalStatus(ApprovalStatus.APPROVED);
            request.setProcessedAt(LocalDateTime.now());
        } else if (request.isAnyRejected()) {
            request.setFinalStatus(ApprovalStatus.REJECTED);
            request.setProcessedAt(LocalDateTime.now());
        }
        // 否则保持PENDING状态
    }

    /**
     * 执行教练更换
     */
    private void executeCoachChange(CoachChangeRequest request) {
        // 找到并删除与当前教练的关系
        List<CoachStudentRelation> currentRelations = relationRepository
            .findByStudentIdAndCoachIdAndStatus(
                request.getStudent().getId(), 
                request.getCurrentCoach().getId(), 
                ApprovalStatus.APPROVED);

        for (CoachStudentRelation relation : currentRelations) {
            relationRepository.delete(relation);
        }

        // 创建与新教练的关系
        CoachStudentRelation newRelation = new CoachStudentRelation();
        newRelation.setStudent(request.getStudent());
        newRelation.setCoach(request.getNewCoach());
        newRelation.setStatus(ApprovalStatus.APPROVED);
        newRelation.setCreatedAt(LocalDateTime.now());
        relationRepository.save(newRelation);

        // 发送成功通知给所有相关人员
        systemMessageService.sendMessage(null, request.getStudent(), MessageType.COACH_CHANGE,
            "教练更换成功", "您已成功从教练 " + request.getCurrentCoach().getRealName() + 
            " 更换为教练 " + request.getNewCoach().getRealName(), request.getId());

        systemMessageService.sendMessage(null, request.getCurrentCoach(), MessageType.COACH_CHANGE,
            "学员更换教练通知", "学员 " + request.getStudent().getRealName() + 
            " 已更换教练，感谢您的指导", request.getId());

        systemMessageService.sendMessage(null, request.getNewCoach(), MessageType.COACH_CHANGE,
            "新学员分配通知", "学员 " + request.getStudent().getRealName() + 
            " 已成为您的学员，请多多指导", request.getId());
    }

    /**
     * 获取校区管理员
     */
    private User getCampusAdmin(Long campusId) {
        List<User> campusAdmins = userRepository.findByRoleAndCampusId(UserRole.CAMPUS_ADMIN, campusId);
        if (campusAdmins.isEmpty()) {
            // 如果没有校区管理员，查找超级管理员
            List<User> superAdmins = userRepository.findByRole(UserRole.SUPER_ADMIN);
            if (superAdmins.isEmpty()) {
                throw new RuntimeException("系统中没有可用的管理员");
            }
            return superAdmins.get(0);
        }
        return campusAdmins.get(0);
    }

    /**
     * 获取用户的待审核申请
     */
    public List<CoachChangeRequest> getPendingRequests(Long userId, UserRole userRole) {
        switch (userRole) {
            case STUDENT:
                return coachChangeRequestRepository.findPendingRequestsByStudentId(userId);
            case COACH:
                return coachChangeRequestRepository.findPendingRequestsByCoachId(userId);
            case CAMPUS_ADMIN:
            case SUPER_ADMIN:
                User admin = userRepository.findById(userId).orElse(null);
                if (admin != null && admin.getRole() == UserRole.CAMPUS_ADMIN) {
                    return coachChangeRequestRepository.findPendingRequestsByCampusId(admin.getCampus().getId());
                } else {
                    // 超级管理员可以查看所有待审核申请
                    return coachChangeRequestRepository.findByFinalStatusOrderByCreatedAtDesc(ApprovalStatus.PENDING);
                }
            default:
                return Collections.emptyList();
        }
    }

    /**
     * 获取申请详情
     */
    public CoachChangeRequest getRequestDetails(Long requestId, Long userId) {
        CoachChangeRequest request = coachChangeRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("申请不存在"));

        // 权限检查：只有相关人员才能查看详情
        if (!isRequestRelatedUser(request, userId)) {
            throw new RuntimeException("无权限查看该申请");
        }

        return request;
    }

    /**
     * 检查用户是否与申请相关
     */
    private boolean isRequestRelatedUser(CoachChangeRequest request, Long userId) {
        return request.getStudent().getId().equals(userId) ||
               request.getCurrentCoach().getId().equals(userId) ||
               request.getNewCoach().getId().equals(userId) ||
               request.getCampusAdmin().getId().equals(userId);
    }
} 
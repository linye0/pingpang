package com.pingpang.training.controller;

import com.pingpang.training.dto.ApiResponse;
import com.pingpang.training.dto.PaymentRequest;
import com.pingpang.training.entity.PaymentRecord;
import com.pingpang.training.entity.Student;
import com.pingpang.training.enums.PaymentMethod;
import com.pingpang.training.repository.PaymentRecordRepository;
import com.pingpang.training.repository.StudentRepository;
import com.pingpang.training.security.UserDetailsImpl;
import com.pingpang.training.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private SystemLogService systemLogService;

    // 学员功能
    @GetMapping("/balance")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getBalance(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Student student = studentRepository.findById(userDetails.getUser().getId()).orElse(null);
            if (student == null) {
                return ApiResponse.success(BigDecimal.ZERO);
            }
            return ApiResponse.success(student.getAccountBalance());
        } catch (Exception e) {
            return ApiResponse.error("获取余额失败");
        }
    }

    @GetMapping("/records")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getPaymentRecords(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            List<PaymentRecord> records = paymentRecordRepository.findByStudentIdOrderByCreatedAtDesc(
                userDetails.getUser().getId());
            return ApiResponse.success(records);
        } catch (Exception e) {
            return ApiResponse.error("获取缴费记录失败");
        }
    }

    @PostMapping("/recharge")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<?> recharge(@Valid @RequestBody PaymentRequest request,
                                  @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ApiResponse.error("充值金额必须大于0");
            }

            if (request.getAmount().compareTo(new BigDecimal("10000")) > 0) {
                return ApiResponse.error("单次充值金额不能超过10000元");
            }

            Student student = studentRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("学员信息不存在"));

            // 创建缴费记录
            PaymentRecord record = new PaymentRecord();
            record.setStudent(student);
            record.setAmount(request.getAmount());
            record.setPaymentMethod(request.getPaymentMethod());
            record.setTransactionNo(generateTransactionNo());
            record.setDescription(request.getDescription() != null ? request.getDescription() : "账户充值");

            paymentRecordRepository.save(record);

            // 更新学员余额
            student.setAccountBalance(student.getAccountBalance().add(request.getAmount()));
            studentRepository.save(student);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "ACCOUNT_RECHARGE", 
                "账户充值: ¥" + request.getAmount());

            return ApiResponse.success("充值成功", record);
        } catch (Exception e) {
            return ApiResponse.error("充值失败: " + e.getMessage());
        }
    }

    @PostMapping("/withdraw")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<?> withdraw(@RequestParam BigDecimal amount,
                                  @RequestParam(required = false) String reason,
                                  @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ApiResponse.error("提取金额必须大于0");
            }

            Student student = studentRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("学员信息不存在"));

            if (student.getAccountBalance().compareTo(amount) < 0) {
                return ApiResponse.error("账户余额不足");
            }

            // 创建提取记录
            PaymentRecord record = new PaymentRecord();
            record.setStudent(student);
            record.setAmount(amount.negate()); // 负数表示提取
            record.setPaymentMethod(PaymentMethod.BALANCE);
            record.setTransactionNo(generateTransactionNo());
            record.setDescription("余额提取" + (reason != null ? ": " + reason : ""));

            paymentRecordRepository.save(record);

            // 更新学员余额
            student.setAccountBalance(student.getAccountBalance().subtract(amount));
            studentRepository.save(student);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "ACCOUNT_WITHDRAW", 
                "余额提取: ¥" + amount);

            return ApiResponse.success("提取成功", record);
        } catch (Exception e) {
            return ApiResponse.error("提取失败: " + e.getMessage());
        }
    }

    // 管理员功能
    @GetMapping("/admin/campus-payments")
    @PreAuthorize("hasRole('CAMPUS_ADMIN') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getCampusPayments(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long campusId = userDetails.getUser().getCampus().getId();
            List<PaymentRecord> payments = paymentRecordRepository.findByCampusId(campusId);
            return ApiResponse.success(payments);
        } catch (Exception e) {
            return ApiResponse.error("获取校区缴费记录失败");
        }
    }

    @GetMapping("/admin/student/{studentId}/payments")
    @PreAuthorize("hasRole('CAMPUS_ADMIN') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getStudentPayments(@PathVariable Long studentId,
                                            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学员不存在"));

            // 权限检查：只能查看本校区学员
            if (!student.getCampus().getId().equals(userDetails.getUser().getCampus().getId()) &&
                !userDetails.getUser().getRole().name().equals("SUPER_ADMIN")) {
                return ApiResponse.error("无权限查看该学员的缴费记录");
            }

            List<PaymentRecord> records = paymentRecordRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
            return ApiResponse.success(records);
        } catch (Exception e) {
            return ApiResponse.error("获取学员缴费记录失败");
        }
    }

    @PostMapping("/admin/student/{studentId}/adjust-balance")
    @PreAuthorize("hasRole('CAMPUS_ADMIN') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> adjustStudentBalance(@PathVariable Long studentId,
                                              @RequestParam BigDecimal amount,
                                              @RequestParam String reason,
                                              @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("学员不存在"));

            // 权限检查
            if (!student.getCampus().getId().equals(userDetails.getUser().getCampus().getId()) &&
                !userDetails.getUser().getRole().name().equals("SUPER_ADMIN")) {
                return ApiResponse.error("无权限调整该学员余额");
            }

            // 检查调整后余额是否为负
            BigDecimal newBalance = student.getAccountBalance().add(amount);
            if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                return ApiResponse.error("调整后余额不能为负数");
            }

            // 创建调整记录
            PaymentRecord record = new PaymentRecord();
            record.setStudent(student);
            record.setAmount(amount);
            record.setPaymentMethod(PaymentMethod.ADMIN_ADJUST);
            record.setTransactionNo(generateTransactionNo());
            record.setDescription("管理员余额调整: " + reason);

            paymentRecordRepository.save(record);

            // 更新学员余额
            student.setAccountBalance(newBalance);
            studentRepository.save(student);

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "BALANCE_ADJUST", 
                String.format("调整学员 %s 余额: %s¥%s, 原因: %s", 
                    student.getRealName(), 
                    amount.compareTo(BigDecimal.ZERO) > 0 ? "+" : "", 
                    amount, reason));

            return ApiResponse.success("余额调整成功", record);
        } catch (Exception e) {
            return ApiResponse.error("余额调整失败: " + e.getMessage());
        }
    }

    @GetMapping("/admin/statistics")
    @PreAuthorize("hasRole('CAMPUS_ADMIN') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> getPaymentStatistics(@RequestParam(required = false) String startDate,
                                              @RequestParam(required = false) String endDate,
                                              @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            Long campusId = userDetails.getUser().getCampus().getId();
            
            LocalDateTime start = startDate != null ? 
                LocalDateTime.parse(startDate + "T00:00:00") : LocalDateTime.now().minusMonths(1);
            LocalDateTime end = endDate != null ? 
                LocalDateTime.parse(endDate + "T23:59:59") : LocalDateTime.now();

            List<PaymentRecord> records = paymentRecordRepository.findByCreatedAtBetween(start, end);
            
            // 筛选本校区的记录
            List<PaymentRecord> campusRecords = records.stream()
                .filter(r -> r.getStudent().getCampus().getId().equals(campusId))
                .collect(Collectors.toList());

            // 统计数据
            BigDecimal totalRecharge = campusRecords.stream()
                .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(PaymentRecord::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalExpense = campusRecords.stream()
                .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .map(r -> r.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            long rechargeCount = campusRecords.stream()
                .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) > 0)
                .count();

            long expenseCount = campusRecords.stream()
                .filter(r -> r.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .count();

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalRecharge", totalRecharge);
            statistics.put("totalExpense", totalExpense);
            statistics.put("netAmount", totalRecharge.subtract(totalExpense));
            statistics.put("rechargeCount", rechargeCount);
            statistics.put("expenseCount", expenseCount);
            statistics.put("totalTransactions", campusRecords.size());
            statistics.put("periodStart", start);
            statistics.put("periodEnd", end);
            
            return ApiResponse.success(statistics);
        } catch (Exception e) {
            return ApiResponse.error("获取缴费统计失败");
        }
    }

    // 模拟支付相关API
    @PostMapping("/generate-qr/{amount}")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    public ApiResponse<?> generateQRCode(@PathVariable BigDecimal amount,
                                        @RequestParam PaymentMethod method,
                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ApiResponse.error("充值金额必须大于0");
            }

            if (amount.compareTo(new BigDecimal("10000")) > 0) {
                return ApiResponse.error("单次充值金额不能超过10000元");
            }

            String orderNo = generateTransactionNo();
            
            // 生成模拟二维码URL
            String qrCodeUrl = generateMockQRCodeUrl(amount, method, orderNo);
            
            Map<String, Object> result = new HashMap<>();
            result.put("qrCodeUrl", qrCodeUrl);
            result.put("orderNo", orderNo);
            result.put("amount", amount);
            result.put("paymentMethod", method);
            result.put("expireTime", System.currentTimeMillis() + 15 * 60 * 1000); // 15分钟后过期

            return ApiResponse.success("二维码生成成功", result);
        } catch (Exception e) {
            return ApiResponse.error("生成二维码失败: " + e.getMessage());
        }
    }

    @PostMapping("/mock-payment-success")
    @PreAuthorize("hasRole('STUDENT') or hasRole('SUPER_ADMIN')")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApiResponse<?> mockPaymentSuccess(@RequestParam String orderNo,
                                           @RequestParam BigDecimal amount,
                                           @RequestParam PaymentMethod paymentMethod,
                                           @RequestParam(required = false) String description,
                                           @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            System.out.println(" 模拟支付成功开始 ");
            System.out.println("订单号: " + orderNo);
            System.out.println("金额: " + amount);
            System.out.println("支付方式: " + paymentMethod);
            System.out.println("用户ID: " + userDetails.getUser().getId());
            
            Student student = studentRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("学员信息不存在"));

            System.out.println("当前余额: " + student.getAccountBalance());

            // 创建支付记录
            PaymentRecord record = new PaymentRecord();
            record.setStudent(student);
            record.setAmount(amount);
            record.setPaymentMethod(paymentMethod);
            record.setTransactionNo(orderNo);
            record.setDescription(description != null ? description : "在线支付");

            System.out.println("保存支付记录");
            record = paymentRecordRepository.save(record);
            System.out.println("支付记录保存成功，ID: " + record.getId());

            // 更新学员余额
            BigDecimal newBalance = student.getAccountBalance().add(amount);
            student.setAccountBalance(newBalance);
            System.out.println("更新余额: " + student.getAccountBalance());
            
            student = studentRepository.save(student);
            System.out.println("学员信息保存成功");

            // 记录操作日志
            systemLogService.log(userDetails.getUser(), "MOCK_PAYMENT", 
                "模拟支付成功: ¥" + amount + " (" + paymentMethod + ")");

            System.out.println("支付成功完成 ");
            return ApiResponse.success("支付成功", record);
        } catch (Exception e) {
            System.err.println(" 支付失败 ");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            return ApiResponse.error("支付失败: " + e.getMessage());
        }
    }

    // 私有方法
    private String generateTransactionNo() {
        return "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateMockQRCodeUrl(BigDecimal amount, PaymentMethod method, String orderNo) {
        // 生成一个模拟的二维码图片URL
        String baseUrl = "https://api.qrserver.com/v1/create-qr-code/";
        String qrContent = String.format("MOCK_PAYMENT|%s|%s|%s", method.name(), amount, orderNo);
        try {
            return baseUrl + "?size=200x200&data=" + java.net.URLEncoder.encode(qrContent, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return baseUrl + "?size=200x200&data=" + qrContent;
        }
    }
} 
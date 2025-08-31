package com.pingpang.training.service;

import com.pingpang.training.dto.PaymentRequest;
import com.pingpang.training.entity.CourseBooking;
import com.pingpang.training.entity.PaymentRecord;
import com.pingpang.training.entity.Student;
import com.pingpang.training.enums.PaymentMethod;
import com.pingpang.training.repository.CourseBookingRepository;
import com.pingpang.training.repository.PaymentRecordRepository;
import com.pingpang.training.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private CourseBookingRepository courseBookingRepository;

    // 处理充值
    public PaymentRecord processRecharge(PaymentRequest request, Student student) {
        // 验证金额
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("充值金额必须大于0");
        }
        
        if (request.getAmount().compareTo(BigDecimal.valueOf(10000)) > 0) {
            throw new RuntimeException("单次充值金额不能超过10000元");
        }
        
        // 模拟支付处理
        String transactionNo = generateTransactionNo(request.getPaymentMethod());
        
        // 根据支付方式处理
        boolean paymentSuccess = processPayment(request, transactionNo);
        
        if (!paymentSuccess) {
            throw new RuntimeException("支付失败，请重试");
        }
        
        // 更新学员余额
        student.setAccountBalance(student.getAccountBalance().add(request.getAmount()));
        studentRepository.save(student);
        
        // 创建支付记录
        PaymentRecord payment = new PaymentRecord();
        payment.setStudent(student);
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionNo(transactionNo);
        payment.setDescription("账户充值");
        
        return paymentRecordRepository.save(payment);
    }

    // 处理退款
    public PaymentRecord processRefund(Student student, BigDecimal amount, String description) {
        // 更新学员余额
        student.setAccountBalance(student.getAccountBalance().add(amount));
        studentRepository.save(student);
        
        // 创建退款记录
        PaymentRecord refund = new PaymentRecord();
        refund.setStudent(student);
        refund.setAmount(amount);
        refund.setPaymentMethod(PaymentMethod.ACCOUNT_REFUND);
        refund.setTransactionNo("REFUND-" + System.currentTimeMillis());
        refund.setDescription(description);
        
        return paymentRecordRepository.save(refund);
    }

    // 扣除费用
    public PaymentRecord deductAmount(Student student, BigDecimal amount, String description) {
        // 检查余额
        if (student.getAccountBalance().compareTo(amount) < 0) {
            throw new RuntimeException("账户余额不足");
        }
        
        // 扣除费用
        student.setAccountBalance(student.getAccountBalance().subtract(amount));
        studentRepository.save(student);
        
        // 创建扣费记录
        PaymentRecord deduction = new PaymentRecord();
        deduction.setStudent(student);
        deduction.setAmount(amount.negate()); // 负数表示扣费
        deduction.setPaymentMethod(PaymentMethod.ACCOUNT_DEDUCT);
        deduction.setTransactionNo("DEDUCT-" + System.currentTimeMillis());
        deduction.setDescription(description);
        
        return paymentRecordRepository.save(deduction);
    }

    // 生成交易号
    private String generateTransactionNo(PaymentMethod paymentMethod) {
        String prefix;
        switch (paymentMethod) {
            case WECHAT:
                prefix = "WX";
                break;
            case ALIPAY:
                prefix = "AL";
                break;
            case OFFLINE:
                prefix = "OF";
                break;
            default:
                prefix = "SYS";
        }
        
        return prefix + LocalDateTime.now().toString().replaceAll("[^0-9]", "").substring(0, 14);
    }

    // 模拟支付处理
    private boolean processPayment(PaymentRequest request, String transactionNo) {
        // 这里应该调用实际的支付接口（微信、支付宝等）
        // 现在模拟支付成功
        
        switch (request.getPaymentMethod()) {
            case WECHAT:
                return simulateWechatPayment(request, transactionNo);
            case ALIPAY:
                return simulateAlipayPayment(request, transactionNo);
            case OFFLINE:
                return true; // 线下支付直接成功
            default:
                return false;
        }
    }

    private boolean simulateWechatPayment(PaymentRequest request, String transactionNo) {
        // 模拟微信支付
        try {
            Thread.sleep(1000); // 模拟网络延迟
            return Math.random() > 0.1; // 90%成功率
        } catch (InterruptedException e) {
            return false;
        }
    }

    private boolean simulateAlipayPayment(PaymentRequest request, String transactionNo) {
        // 模拟支付宝支付
        try {
            Thread.sleep(800); // 模拟网络延迟
            return Math.random() > 0.05; // 95%成功率
        } catch (InterruptedException e) {
            return false;
        }
    }

    // 生成支付二维码（模拟）
    public String generatePaymentQRCode(PaymentRequest request) {
        String qrData = String.format("payment://%s?amount=%s&merchant=pingpang&timestamp=%d",
            request.getPaymentMethod().toString().toLowerCase(),
            request.getAmount().toString(),
            System.currentTimeMillis());
        
        // 这里应该调用实际的二维码生成服务
        return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg==";
    }

    // 验证预约时间是否可用
    public boolean isTimeSlotAvailable(Long coachId, String tableNumber, 
                                      LocalDateTime startTime, LocalDateTime endTime) {
        // 检查教练时间冲突
        List<CourseBooking> coachConflicts = courseBookingRepository
            .findConflictingBookings(coachId, startTime, endTime);
        
        // 检查球台时间冲突
        List<CourseBooking> tableConflicts = courseBookingRepository
            .findConflictingBookingsWithTable(tableNumber, startTime, endTime);
        
        return coachConflicts.isEmpty() && tableConflicts.isEmpty();
    }

    // 自动分配球台
    public String autoAssignTable(LocalDateTime startTime, LocalDateTime endTime) {
        List<String> availableTables = getAvailableTables(startTime, endTime);
        if (availableTables.isEmpty()) {
            throw new RuntimeException("该时间段没有可用球台");
        }
        return availableTables.get(0);
    }
    
    // 获取可用球台
    private List<String> getAvailableTables(LocalDateTime startTime, LocalDateTime endTime) {
        List<String> allTables = Arrays.asList("T001", "T002", "T003", "T004", "T005");
        List<String> occupiedTables = courseBookingRepository.findOccupiedTables(startTime, endTime);
        return allTables.stream()
            .filter(table -> !occupiedTables.contains(table))
            .collect(java.util.stream.Collectors.toList());
    }
}
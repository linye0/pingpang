package com.pingpang.training.dto;

import com.pingpang.training.enums.PaymentMethod;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class PaymentRequest {
    
    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0")
    private BigDecimal amount;
    
    @NotNull(message = "支付方式不能为空")
    private PaymentMethod paymentMethod;
    
    private String transactionNo; // 第三方支付交易号
    
    private String description;
    
    public PaymentRequest() {}
    
    public PaymentRequest(BigDecimal amount, PaymentMethod paymentMethod) {
        this.amount = amount;
        this.paymentMethod = paymentMethod;
    }
    
    // Getters and Setters
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    
    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
    
    public String getTransactionNo() { return transactionNo; }
    public void setTransactionNo(String transactionNo) { this.transactionNo = transactionNo; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
} 
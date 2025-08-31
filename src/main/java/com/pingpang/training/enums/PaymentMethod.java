package com.pingpang.training.enums;

public enum PaymentMethod {
    WECHAT("微信支付"),
    ALIPAY("支付宝"),
    OFFLINE("线下支付"),
    BALANCE("余额支付"),
    ADMIN_ADJUST("管理员调整"),
    ACCOUNT_DEDUCT("账户扣费"),
    ACCOUNT_REFUND("账户退款");
    
    private final String description;
    
    PaymentMethod(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
} 
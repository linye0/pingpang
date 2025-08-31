package com.pingpang.training.utils;

import java.util.regex.Pattern;

public class ValidationUtils {
    
    // 手机号验证
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    
    // 邮箱验证
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    
    // 身份证号验证（简化版）
    private static final Pattern ID_CARD_PATTERN = Pattern.compile(
        "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$");
    
    // 密码强度验证（8-16位，包含字母、数字和特殊字符）
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,16}$");
    
    // 用户名验证（3-20位字母数字下划线）
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,20}$");
    
    // 中文姓名验证
    private static final Pattern CHINESE_NAME_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5]{2,10}$");
    
    /**
     * 验证手机号
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }
    
    /**
     * 验证邮箱
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * 验证身份证号
     */
    public static boolean isValidIdCard(String idCard) {
        return idCard != null && ID_CARD_PATTERN.matcher(idCard).matches();
    }
    
    /**
     * 验证密码强度
     */
    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }
    
    /**
     * 验证用户名
     */
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }
    
    /**
     * 验证中文姓名
     */
    public static boolean isValidChineseName(String name) {
        return name != null && CHINESE_NAME_PATTERN.matcher(name).matches();
    }
    
    /**
     * 验证年龄
     */
    public static boolean isValidAge(Integer age) {
        return age != null && age >= 3 && age <= 100;
    }
    
    /**
     * 验证字符串长度
     */
    public static boolean isValidLength(String str, int minLength, int maxLength) {
        if (str == null) return false;
        int length = str.trim().length();
        return length >= minLength && length <= maxLength;
    }
    
    /**
     * 验证非空字符串
     */
    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }
    
    /**
     * 获取手机号验证失败消息
     */
    public static String getPhoneValidationMessage() {
        return "手机号格式不正确，请输入11位数字，以1开头";
    }
    
    /**
     * 获取邮箱验证失败消息
     */
    public static String getEmailValidationMessage() {
        return "邮箱格式不正确，请输入有效的邮箱地址";
    }
    
    /**
     * 获取身份证验证失败消息
     */
    public static String getIdCardValidationMessage() {
        return "身份证号格式不正确，请输入18位有效身份证号";
    }
    
    /**
     * 获取密码验证失败消息
     */
    public static String getPasswordValidationMessage() {
        return "密码格式不正确，必须为8-16位，包含字母、数字和特殊字符(@$!%*?&)";
    }
    
    /**
     * 获取用户名验证失败消息
     */
    public static String getUsernameValidationMessage() {
        return "用户名格式不正确，必须为3-20位字母、数字或下划线";
    }
    
    /**
     * 获取中文姓名验证失败消息
     */
    public static String getChineseNameValidationMessage() {
        return "姓名格式不正确，请输入2-10位中文字符";
    }
    
    /**
     * 获取年龄验证失败消息
     */
    public static String getAgeValidationMessage() {
        return "年龄范围不正确，请输入3-100之间的年龄";
    }
    
    /**
     * 验证并返回错误消息
     */
    public static class ValidationResult {
        private boolean valid;
        private String message;
        
        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }
    
    /**
     * 综合验证用户信息
     */
    public static ValidationResult validateUserInfo(String username, String password, 
                                                   String realName, String phone, String email) {
        if (!isValidUsername(username)) {
            return ValidationResult.error(getUsernameValidationMessage());
        }
        
        if (!isValidPassword(password)) {
            return ValidationResult.error(getPasswordValidationMessage());
        }
        
        if (!isValidChineseName(realName)) {
            return ValidationResult.error(getChineseNameValidationMessage());
        }
        
        if (!isValidPhone(phone)) {
            return ValidationResult.error(getPhoneValidationMessage());
        }
        
        if (email != null && !email.trim().isEmpty() && !isValidEmail(email)) {
            return ValidationResult.error(getEmailValidationMessage());
        }
        
        return ValidationResult.success();
    }
    
    /**
     * 验证教练更新信息
     */
    public static ValidationResult validateCoachUpdateInfo(String realName, String phone, 
                                                          String email, Integer age) {
        if (realName != null && !isValidChineseName(realName)) {
            return ValidationResult.error(getChineseNameValidationMessage());
        }
        
        if (phone != null && !isValidPhone(phone)) {
            return ValidationResult.error(getPhoneValidationMessage());
        }
        
        if (email != null && !email.trim().isEmpty() && !isValidEmail(email)) {
            return ValidationResult.error(getEmailValidationMessage());
        }
        
        if (age != null && !isValidAge(age)) {
            return ValidationResult.error(getAgeValidationMessage());
        }
        
        return ValidationResult.success();
    }
} 
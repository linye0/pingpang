package com.pingpang.training.entity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "software_licenses")
public class SoftwareLicense {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "机构名称不能为空")
    @Column(nullable = false, unique = true)
    private String organizationName;
    
    @NotBlank(message = "授权密钥不能为空")
    @Column(nullable = false, unique = true)
    private String licenseKey;
    
    @NotBlank(message = "设备标识不能为空")
    @Column(nullable = false, unique = true)
    private String deviceId;
    
    @NotNull(message = "开始时间不能为空")
    @Column(nullable = false)
    private LocalDateTime startDate;
    
    @NotNull(message = "结束时间不能为空")
    @Column(nullable = false)
    private LocalDateTime endDate;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal annualFee = new BigDecimal("500");
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Constructors
    public SoftwareLicense() {}
    
    public SoftwareLicense(String organizationName, String licenseKey, String deviceId, LocalDateTime startDate, LocalDateTime endDate) {
        this.organizationName = organizationName;
        this.licenseKey = licenseKey;
        this.deviceId = deviceId;
        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
    
    public String getLicenseKey() { return licenseKey; }
    public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    
    public BigDecimal getAnnualFee() { return annualFee; }
    public void setAnnualFee(BigDecimal annualFee) { this.annualFee = annualFee; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
} 
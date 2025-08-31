package com.pingpang.training.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "student")
@PrimaryKeyJoinColumn(name = "id")
public class Student extends User {
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal accountBalance = BigDecimal.ZERO;
    
    @Column(nullable = false)
    private Integer cancellationCount = 0;
    
    @Column(nullable = false)
    private LocalDateTime lastCancellationReset;
    
    @JsonIgnore
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CoachStudentRelation> coachRelations;
    
    @JsonIgnore
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CourseBooking> courseBookings;
    
    @JsonIgnore
    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<PaymentRecord> paymentRecords;
    
    public Student() {
        super();
        this.lastCancellationReset = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
    }
    
    // Getters and Setters
    public BigDecimal getAccountBalance() { return accountBalance; }
    public void setAccountBalance(BigDecimal accountBalance) { this.accountBalance = accountBalance; }
    
    public Integer getCancellationCount() { return cancellationCount; }
    public void setCancellationCount(Integer cancellationCount) { this.cancellationCount = cancellationCount; }
    
    public LocalDateTime getLastCancellationReset() { return lastCancellationReset; }
    public void setLastCancellationReset(LocalDateTime lastCancellationReset) { this.lastCancellationReset = lastCancellationReset; }
    
    public List<CoachStudentRelation> getCoachRelations() { return coachRelations; }
    public void setCoachRelations(List<CoachStudentRelation> coachRelations) { this.coachRelations = coachRelations; }
    
    public List<CourseBooking> getCourseBookings() { return courseBookings; }
    public void setCourseBookings(List<CourseBooking> courseBookings) { this.courseBookings = courseBookings; }
    
    public List<PaymentRecord> getPaymentRecords() { return paymentRecords; }
    public void setPaymentRecords(List<PaymentRecord> paymentRecords) { this.paymentRecords = paymentRecords; }
    
    // 兼容性字段 - 紧急联系人信息
    @Column(length = 100)
    private String emergencyContact;
    
    @Column(length = 20)
    private String emergencyPhone;
    
    public String getEmergencyContact() { return emergencyContact; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    
    public String getEmergencyPhone() { return emergencyPhone; }
    public void setEmergencyPhone(String emergencyPhone) { this.emergencyPhone = emergencyPhone; }
} 
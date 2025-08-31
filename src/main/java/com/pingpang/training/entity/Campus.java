package com.pingpang.training.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import javax.persistence.*;
import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "campus")
public class Campus {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "校区名称不能为空")
    @Size(max = 50, message = "校区名称长度不能超过50个字符")
    @Column(nullable = false)
    private String name;
    
    @NotBlank(message = "校区地址不能为空")
    @Size(max = 200, message = "地址长度不能超过200个字符")
    @Column(nullable = false)
    private String address;
    
    @NotBlank(message = "联系人不能为空")
    @Size(max = 20, message = "联系人姓名长度不能超过20个字符")
    @Column(nullable = false)
    private String contactPerson;
    
    @NotBlank(message = "联系电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系电话格式不正确")
    @Column(nullable = false)
    private String contactPhone;
    
    @Email(message = "邮箱格式不正确")
    private String contactEmail;
    
    @Column(nullable = false)
    private Boolean isMainCampus = false;
    
    @JsonIgnore
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;
    
    @Column(nullable = false)
    private Boolean active = true;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @JsonIgnore
    @OneToMany(mappedBy = "campus", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<User> users;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    

    public Campus() {}
    
    public Campus(String name, String address, String contactPerson, String contactPhone) {
        this.name = name;
        this.address = address;
        this.contactPerson = contactPerson;
        this.contactPhone = contactPhone;
    }
    

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    
    public Boolean getIsMainCampus() { return isMainCampus; }
    public void setIsMainCampus(Boolean isMainCampus) { this.isMainCampus = isMainCampus; }
    
    public User getAdmin() { return admin; }
    public void setAdmin(User admin) { this.admin = admin; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }
} 
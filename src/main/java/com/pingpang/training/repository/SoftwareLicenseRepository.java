package com.pingpang.training.repository;

import com.pingpang.training.entity.SoftwareLicense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SoftwareLicenseRepository extends JpaRepository<SoftwareLicense, Long> {
    
    Optional<SoftwareLicense> findByLicenseKey(String licenseKey);
    
    Optional<SoftwareLicense> findByDeviceId(String deviceId);
    
    Optional<SoftwareLicense> findByOrganizationName(String organizationName);
    
    List<SoftwareLicense> findByActiveTrue();
    
    List<SoftwareLicense> findByActiveFalse();
    
    @Query("SELECT l FROM SoftwareLicense l WHERE l.endDate < :currentDate")
    List<SoftwareLicense> findExpiredLicenses(@Param("currentDate") LocalDateTime currentDate);
    
    @Query("SELECT l FROM SoftwareLicense l WHERE l.endDate BETWEEN :currentDate AND :warningDate")
    List<SoftwareLicense> findExpiringLicenses(@Param("currentDate") LocalDateTime currentDate, 
                                              @Param("warningDate") LocalDateTime warningDate);
    
    boolean existsByLicenseKey(String licenseKey);
    
    boolean existsByDeviceId(String deviceId);
    
    boolean existsByOrganizationName(String organizationName);
} 
package com.pingpang.training.repository;

import com.pingpang.training.entity.MonthlyCompetition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MonthlyCompetitionRepository extends JpaRepository<MonthlyCompetition, Long> {
    
    List<MonthlyCompetition> findByCampusId(Long campusId);
    
    List<MonthlyCompetition> findByCampusIdAndRegistrationOpenTrue(Long campusId);
    
    @Query("SELECT c FROM MonthlyCompetition c WHERE c.campus.id = :campusId AND c.competitionDate >= :currentDate ORDER BY c.competitionDate ASC")
    List<MonthlyCompetition> findUpcomingCompetitionsByCampus(@Param("campusId") Long campusId, 
                                                             @Param("currentDate") LocalDateTime currentDate);
    
    @Query("SELECT c FROM MonthlyCompetition c WHERE c.competitionDate >= :currentDate AND c.registrationOpen = true ORDER BY c.competitionDate ASC")
    List<MonthlyCompetition> findUpcomingOpenCompetitions(@Param("currentDate") LocalDateTime currentDate);
    
    @Query("SELECT c FROM MonthlyCompetition c WHERE c.competitionDate BETWEEN :startDate AND :endDate")
    List<MonthlyCompetition> findCompetitionsByDateRange(@Param("startDate") LocalDateTime startDate, 
                                                        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(r) FROM CompetitionRegistration r WHERE r.competition.id = :competitionId")
    Long countRegistrationsByCompetitionId(@Param("competitionId") Long competitionId);
    
    // 校区统计方法
    Long countByCampusId(Long campusId);
} 
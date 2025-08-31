package com.pingpang.training.repository;

import com.pingpang.training.entity.CompetitionRegistration;
import com.pingpang.training.enums.CompetitionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompetitionRegistrationRepository extends JpaRepository<CompetitionRegistration, Long> {
    
    List<CompetitionRegistration> findByStudentId(Long studentId);
    
    List<CompetitionRegistration> findByCompetitionId(Long competitionId);
    
    List<CompetitionRegistration> findByCompetitionIdAndCompetitionGroup(Long competitionId, CompetitionGroup group);
    
    Optional<CompetitionRegistration> findByCompetitionIdAndStudentId(Long competitionId, Long studentId);
    
    boolean existsByCompetitionIdAndStudentId(Long competitionId, Long studentId);
    
    @Query("SELECT r FROM CompetitionRegistration r WHERE r.student.id = :studentId AND r.competition.campus.id = :campusId")
    List<CompetitionRegistration> findByStudentIdAndCampusId(@Param("studentId") Long studentId, 
                                                           @Param("campusId") Long campusId);
    
    @Query("SELECT COUNT(r) FROM CompetitionRegistration r WHERE r.competition.id = :competitionId")
    Long countByCompetitionId(@Param("competitionId") Long competitionId);
    
    @Query("SELECT COUNT(r) FROM CompetitionRegistration r WHERE r.competition.id = :competitionId AND r.competitionGroup = :group")
    Long countByCompetitionIdAndGroup(@Param("competitionId") Long competitionId, 
                                    @Param("group") CompetitionGroup group);
    
    // 学员统计方法
    Long countByStudentId(Long studentId);
} 
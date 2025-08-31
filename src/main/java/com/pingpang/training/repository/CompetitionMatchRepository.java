package com.pingpang.training.repository;

import com.pingpang.training.entity.CompetitionMatch;
import com.pingpang.training.enums.CompetitionGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompetitionMatchRepository extends JpaRepository<CompetitionMatch, Long> {
    
    List<CompetitionMatch> findByCompetitionId(Long competitionId);
    
    List<CompetitionMatch> findByCompetitionIdAndCompetitionGroup(Long competitionId, CompetitionGroup group);
    
    List<CompetitionMatch> findByCompetitionIdAndCompetitionGroupOrderByRoundNumberAscMatchNumberAsc(
        Long competitionId, CompetitionGroup group);
    
    @Query("SELECT m FROM CompetitionMatch m WHERE m.competition.id = :competitionId " +
           "AND (m.player1.id = :studentId OR m.player2.id = :studentId)")
    List<CompetitionMatch> findByCompetitionIdAndStudentId(@Param("competitionId") Long competitionId, 
                                                          @Param("studentId") Long studentId);
    
    List<CompetitionMatch> findByCompetitionIdAndRoundNumber(Long competitionId, Integer roundNumber);
    
    List<CompetitionMatch> findByCompetitionIdAndCompetitionGroupAndRoundNumber(
        Long competitionId, CompetitionGroup group, Integer roundNumber);
    
    @Query("SELECT DISTINCT m.roundNumber FROM CompetitionMatch m WHERE m.competition.id = :competitionId " +
           "AND m.competitionGroup = :group ORDER BY m.roundNumber")
    List<Integer> findRoundNumbersByCompetitionIdAndGroup(@Param("competitionId") Long competitionId, 
                                                         @Param("group") CompetitionGroup group);
    
    Long countByCompetitionIdAndCompetitionGroup(Long competitionId, CompetitionGroup group);
    
    void deleteByCompetitionId(Long competitionId);
    
    void deleteByCompetitionIdAndCompetitionGroup(Long competitionId, CompetitionGroup group);
} 
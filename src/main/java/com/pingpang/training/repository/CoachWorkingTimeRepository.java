package com.pingpang.training.repository;

import com.pingpang.training.entity.CoachWorkingTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CoachWorkingTimeRepository extends JpaRepository<CoachWorkingTime, Long> {
    
    // 查找教练的周工作时间安排
    List<CoachWorkingTime> findByCoachIdAndSpecificDateIsNull(Long coachId);
    
    // 查找教练在特定日期的工作时间安排
    List<CoachWorkingTime> findByCoachIdAndSpecificDate(Long coachId, LocalDate specificDate);
    
    // 查找教练在特定星期几的工作时间安排
    List<CoachWorkingTime> findByCoachIdAndDayOfWeekAndSpecificDateIsNull(Long coachId, Integer dayOfWeek);
    
    // 查找教练在某个日期范围内的所有工作时间安排
    @Query("SELECT cwt FROM CoachWorkingTime cwt WHERE cwt.coach.id = :coachId AND " +
           "((cwt.specificDate IS NULL AND cwt.dayOfWeek = :dayOfWeek) OR " +
           "(cwt.specificDate = :specificDate))")
    List<CoachWorkingTime> findCoachWorkingTimeForDate(@Param("coachId") Long coachId, 
                                                      @Param("dayOfWeek") Integer dayOfWeek,
                                                      @Param("specificDate") LocalDate specificDate);
    
    // 查找教练可用的工作时间
    @Query("SELECT cwt FROM CoachWorkingTime cwt WHERE cwt.coach.id = :coachId AND cwt.isAvailable = true")
    List<CoachWorkingTime> findAvailableWorkingTimes(@Param("coachId") Long coachId);
    
    // 删除教练在特定星期几的工作时间安排
    void deleteByCoachIdAndDayOfWeekAndSpecificDateIsNull(Long coachId, Integer dayOfWeek);
    
    // 删除教练在特定日期的工作时间安排
    void deleteByCoachIdAndSpecificDate(Long coachId, LocalDate specificDate);
} 
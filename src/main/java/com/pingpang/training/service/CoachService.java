package com.pingpang.training.service;

import com.pingpang.training.entity.Coach;
import com.pingpang.training.enums.ApprovalStatus;
import com.pingpang.training.enums.Gender;
import com.pingpang.training.repository.CoachRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CoachService {

    @Autowired
    private CoachRepository coachRepository;

    public List<Coach> getApprovedCoaches(Long campusId) {
        return coachRepository.findByCampusIdAndApprovalStatus(campusId, ApprovalStatus.APPROVED);
    }

    public List<Coach> searchCoaches(Long campusId, String name, Gender gender, Integer age) {
        String genderStr = gender != null ? gender.name() : null;
        return coachRepository.searchCoaches(campusId, name, genderStr, age);
    }

    public Optional<Coach> findById(Long id) {
        return coachRepository.findById(id);
    }

    public List<Coach> getCoachesByCampus(Long campusId) {
        return coachRepository.findByCampusId(campusId);
    }

    public Coach save(Coach coach) {
        return coachRepository.save(coach);
    }

    public void deleteById(Long id) {
        coachRepository.deleteById(id);
    }

    public List<Coach> findAll() {
        return coachRepository.findAll();
    }

    public long count() {
        return coachRepository.count();
    }

    public boolean existsById(Long id) {
        return coachRepository.existsById(id);
    }
} 
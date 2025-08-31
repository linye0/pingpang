package com.pingpang.training.service;

import com.pingpang.training.entity.MonthlyCompetition;
import com.pingpang.training.entity.CompetitionRegistration;
import com.pingpang.training.entity.CompetitionMatch;
import com.pingpang.training.entity.Student;
import com.pingpang.training.entity.PaymentRecord;
import com.pingpang.training.enums.CompetitionGroup;
import com.pingpang.training.enums.PaymentMethod;
import com.pingpang.training.repository.CompetitionRegistrationRepository;
import com.pingpang.training.repository.MonthlyCompetitionRepository;
import com.pingpang.training.repository.CompetitionMatchRepository;
import com.pingpang.training.repository.StudentRepository;
import com.pingpang.training.repository.PaymentRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CompetitionService {
    
    @Autowired
    private CompetitionRegistrationRepository competitionRegistrationRepository;
    
    @Autowired
    private MonthlyCompetitionRepository monthlyCompetitionRepository;
    
    @Autowired
    private CompetitionMatchRepository competitionMatchRepository;
    
    @Autowired
    private StudentRepository studentRepository;
    
    @Autowired
    private PaymentRecordRepository paymentRecordRepository;
    
    /**
     * 学员报名比赛（包含费用扣除）
     */
    @Transactional
    public CompetitionRegistration registerForCompetition(Long competitionId, Long studentId, CompetitionGroup group) {
        // 检查是否已经报名
        if (competitionRegistrationRepository.existsByCompetitionIdAndStudentId(competitionId, studentId)) {
            throw new RuntimeException("已经报名该比赛");
        }
        
        MonthlyCompetition competition = monthlyCompetitionRepository.findById(competitionId)
            .orElseThrow(() -> new RuntimeException("比赛不存在"));
        
        if (!competition.getRegistrationOpen()) {
            throw new RuntimeException("比赛报名已关闭");
        }
        
        Student student = studentRepository.findById(studentId)
            .orElseThrow(() -> new RuntimeException("学员信息不存在"));
        
        // 检查余额
        if (student.getAccountBalance().compareTo(competition.getRegistrationFee()) < 0) {
            throw new RuntimeException("账户余额不足，请先充值");
        }
        
        // 创建报名记录
        CompetitionRegistration registration = new CompetitionRegistration();
        registration.setCompetition(competition);
        registration.setStudent(student);
        registration.setCompetitionGroup(group);
        
        registration = competitionRegistrationRepository.save(registration);
        
        // 扣除报名费
        student.setAccountBalance(student.getAccountBalance().subtract(competition.getRegistrationFee()));
        studentRepository.save(student);
        
        // 创建扣费记录
        PaymentRecord record = new PaymentRecord();
        record.setStudent(student);
        record.setAmount(competition.getRegistrationFee().negate());
        record.setPaymentMethod(PaymentMethod.BALANCE);
        record.setTransactionNo("COMP-" + System.currentTimeMillis());
        record.setDescription("比赛报名费: " + competition.getName());
        
        paymentRecordRepository.save(record);
        
        return registration;
    }
    
    /**
     * 取消比赛报名（退还费用）
     */
    @Transactional
    public void cancelCompetitionRegistration(Long registrationId, Long studentId) {
        CompetitionRegistration registration = competitionRegistrationRepository.findById(registrationId)
            .orElseThrow(() -> new RuntimeException("报名记录不存在"));
        
        if (!registration.getStudent().getId().equals(studentId)) {
            throw new RuntimeException("无权取消此报名");
        }
        
        // 退还报名费
        Student student = registration.getStudent();
        MonthlyCompetition competition = registration.getCompetition();
        
        student.setAccountBalance(student.getAccountBalance().add(competition.getRegistrationFee()));
        studentRepository.save(student);
        
        // 创建退费记录
        PaymentRecord record = new PaymentRecord();
        record.setStudent(student);
        record.setAmount(competition.getRegistrationFee());
        record.setPaymentMethod(PaymentMethod.BALANCE);
        record.setTransactionNo("REFUND-" + System.currentTimeMillis());
        record.setDescription("比赛报名费退款: " + competition.getName());
        
        paymentRecordRepository.save(record);
        
        // 删除报名记录
        competitionRegistrationRepository.delete(registration);
        
        // 如果已经生成了赛程，需要重新生成
        if (competition.getScheduleGenerated()) {
            generateAndSaveCompetitionSchedule(competition.getId());
        }
    }
    
    /**
     * 生成并保存比赛赛程到数据库
     */
    @Transactional
    public void generateAndSaveCompetitionSchedule(Long competitionId) {
        MonthlyCompetition competition = monthlyCompetitionRepository.findById(competitionId)
            .orElseThrow(() -> new RuntimeException("比赛不存在"));
        
        // 清除旧的比赛安排
        competitionMatchRepository.deleteByCompetitionId(competitionId);
        
        // 按组别获取报名学员
        Map<CompetitionGroup, List<CompetitionRegistration>> groupRegistrations = 
            competitionRegistrationRepository.findByCompetitionId(competitionId)
                .stream()
                .collect(Collectors.groupingBy(CompetitionRegistration::getCompetitionGroup));
        
        // 为每个组别生成赛程
        for (Map.Entry<CompetitionGroup, List<CompetitionRegistration>> entry : groupRegistrations.entrySet()) {
            CompetitionGroup group = entry.getKey();
            List<CompetitionRegistration> registrations = entry.getValue();
            
            if (registrations.size() <= 6) {
                // 全循环赛程
                generateAndSaveRoundRobinMatches(competition, group, registrations);
            } else {
                // 分组+淘汰赛程
                generateAndSaveGroupKnockoutMatches(competition, group, registrations);
            }
        }
        
        // 标记赛程已生成
        competition.setScheduleGenerated(true);
        monthlyCompetitionRepository.save(competition);
    }
    
    /**
     * 生成并保存全循环赛程（6人或以下）
     */
    private void generateAndSaveRoundRobinMatches(MonthlyCompetition competition, CompetitionGroup group, 
                                                 List<CompetitionRegistration> registrations) {
        int playerCount = registrations.size();
        if (playerCount < 2) {
            return; // 人数不足，无法安排比赛
        }
        
        // 为选手编号并随机排序
        List<Student> players = registrations.stream()
            .map(CompetitionRegistration::getStudent)
            .collect(Collectors.toList());
        Collections.shuffle(players);
        
        // 如果是奇数，需要添加轮空
        boolean hasGhost = playerCount % 2 == 1;
        if (hasGhost) {
            playerCount++; // 虚拟增加一个选手用于轮空计算
        }
        
        int totalRounds = playerCount - 1;
        int matchNumber = 1;
        
        // 根据图1和图2的规则生成对战
        for (int round = 0; round < totalRounds; round++) {
            List<CompetitionMatch> roundMatches = new ArrayList<>();
            
            // 生成这一轮的对战
            List<int[]> roundPairs = generateRoundPairs(players.size(), round, hasGhost);
            
            for (int[] pair : roundPairs) {
                CompetitionMatch match = new CompetitionMatch();
                match.setCompetition(competition);
                match.setCompetitionGroup(group);
                match.setRoundNumber(round + 1);
                match.setMatchNumber(matchNumber++);
                match.setMatchType("全循环");
                
                if (pair[1] == -1) { // 轮空
                    match.setPlayer1(players.get(pair[0]));
                    match.setPlayer2(null);
                    match.setIsBye(true);
                    match.setTableNumber("轮空");
                } else {
                    match.setPlayer1(players.get(pair[0]));
                    match.setPlayer2(players.get(pair[1]));
                    match.setIsBye(false);
                    match.setTableNumber("T" + String.format("%03d", roundMatches.size() + 1));
                }
                
                roundMatches.add(match);
            }
            
            competitionMatchRepository.saveAll(roundMatches);
        }
    }
    
    /**
     * 根据需求文档图1和图2的轮次规则生成对战配对
     */
    private List<int[]> generateRoundPairs(int playerCount, int round, boolean hasGhost) {
        List<int[]> pairs = new ArrayList<>();
        
        if (playerCount == 0) return pairs;
        
        // 如果是奇数人数，需要轮空处理
        if (hasGhost) {
            return generateOddPlayerRoundPairs(playerCount, round);
        } else {
            return generateEvenPlayerRoundPairs(playerCount, round);
        }
    }
    
    /**
     * 偶数人数的轮次配对（基于需求文档图1）
     */
    private List<int[]> generateEvenPlayerRoundPairs(int playerCount, int round) {
        List<int[]> pairs = new ArrayList<>();
        
        // 1号选手固定，其他选手轮转
        for (int i = 0; i < playerCount / 2; i++) {
            int player1, player2;
            
            if (i == 0) {
                // 第一场：1号 vs 轮转的对手
                player1 = 0; // 选手1（索引0）
                player2 = (playerCount - 1 - round + playerCount) % playerCount;
                if (player2 == 0) player2 = playerCount - 1;
            } else {
                // 其他场次：轮转规则
                player1 = i;
                player2 = playerCount - i - round - 1;
                while (player2 <= 0) player2 += playerCount;
                if (player2 >= playerCount) player2 = player2 % playerCount;
                
                // 避免和第一场重复
                if (player1 == 0 || player2 == 0) {
                    continue;
                }
            }
            
            if (player1 != player2 && player1 >= 0 && player1 < playerCount && 
                player2 >= 0 && player2 < playerCount) {
                pairs.add(new int[]{player1, player2});
            }
        }
        
        return pairs;
    }
    
    /**
     * 奇数人数的轮次配对（基于需求文档图2）
     */
    private List<int[]> generateOddPlayerRoundPairs(int playerCount, int round) {
        List<int[]> pairs = new ArrayList<>();
        
        // 使用圆桌算法处理奇数人数轮空
        List<Integer> players = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            players.add(i);
        }
        
        // 固定第一个选手，其他选手轮转
        int fixedPlayer = 0;
        List<Integer> rotatingPlayers = new ArrayList<>();
        for (int i = 1; i < playerCount; i++) {
            rotatingPlayers.add(i);
        }
        
        // 轮转
        if (round > 0 && !rotatingPlayers.isEmpty()) {
            Collections.rotate(rotatingPlayers, round);
        }
        
        // 重新组合选手列表
        List<Integer> currentRoundPlayers = new ArrayList<>();
        currentRoundPlayers.add(fixedPlayer);
        currentRoundPlayers.addAll(rotatingPlayers);
        
        // 生成配对，最后一个选手轮空
        for (int i = 0; i < playerCount / 2; i++) {
            int player1 = currentRoundPlayers.get(i);
            int player2 = currentRoundPlayers.get(playerCount - 1 - i);
            
            if (player1 != player2) {
                pairs.add(new int[]{player1, player2});
            }
        }
        
        // 轮空的选手
        if (playerCount % 2 == 1) {
            int byePlayer = currentRoundPlayers.get(playerCount / 2);
            pairs.add(new int[]{byePlayer, -1});
        }
        
        return pairs;
    }
    
    /**
     * 生成并保存分组+淘汰赛程（6人以上）
     */
    private void generateAndSaveGroupKnockoutMatches(MonthlyCompetition competition, CompetitionGroup group, 
                                                    List<CompetitionRegistration> registrations) {
        int playerCount = registrations.size();
        int groupSize = 4; // 每小组4人
        int groupCount = (int) Math.ceil((double) playerCount / groupSize);
        
        List<Student> shuffledPlayers = registrations.stream()
            .map(CompetitionRegistration::getStudent)
            .collect(Collectors.toList());
        Collections.shuffle(shuffledPlayers);
        
        int matchNumber = 1;
        
        // 分组阶段
        for (int i = 0; i < groupCount; i++) {
            int startIndex = i * groupSize;
            int endIndex = Math.min(startIndex + groupSize, playerCount);
            List<Student> groupPlayers = shuffledPlayers.subList(startIndex, endIndex);
            
            String groupName = "第" + (i + 1) + "小组";
            
            // 为每个小组生成全循环比赛
            generateGroupStageMatches(competition, group, groupPlayers, groupName, matchNumber);
            matchNumber += calculateRoundRobinMatches(groupPlayers.size());
        }
        
        // 生成淘汰赛阶段（小组前两名交叉淘汰）
        generateKnockoutStage(competition, group, groupCount, matchNumber);
    }
    
    /**
     * 生成淘汰赛阶段
     */
    private void generateKnockoutStage(MonthlyCompetition competition, CompetitionGroup group, 
                                      int groupCount, int startMatchNumber) {
        // 为简化起见，生成淘汰赛的框架
        // 实际比赛时需要根据小组赛结果确定参赛选手
        
        int qualifyingPlayers = groupCount * 2; // 每组前两名
        int matchNumber = startMatchNumber;
        
        // 计算淘汰赛轮数
        int knockoutRounds = (int) Math.ceil(Math.log(qualifyingPlayers) / Math.log(2));
        
        for (int round = 1; round <= knockoutRounds; round++) {
            int playersInRound = qualifyingPlayers / (int) Math.pow(2, round - 1);
            int matchesInRound = playersInRound / 2;
            
            String roundName = getRoundName(round, knockoutRounds);
            
            for (int matchInRound = 1; matchInRound <= matchesInRound; matchInRound++) {
                CompetitionMatch match = new CompetitionMatch();
                match.setCompetition(competition);
                match.setCompetitionGroup(group);
                match.setRoundNumber(round + 100); // 区别于小组赛轮次
                match.setMatchNumber(matchNumber++);
                match.setMatchType("淘汰赛");
                match.setGroupName(roundName);
                match.setIsBye(false);
                match.setTableNumber("T" + String.format("%03d", matchInRound));
                
                // 淘汰赛的具体对阵需要在小组赛结束后确定
                // 这里先创建框架，player1和player2为null
                match.setPlayer1(null);
                match.setPlayer2(null);
                match.setMatchStatus("PENDING_GROUP_STAGE");
                
                competitionMatchRepository.save(match);
            }
        }
    }
    
    /**
     * 获取淘汰赛轮次名称
     */
    private String getRoundName(int round, int totalRounds) {
        if (round == totalRounds) {
            return "决赛";
        } else if (round == totalRounds - 1) {
            return "半决赛";
        } else if (round == totalRounds - 2) {
            return "四分之一决赛";
        } else {
            return "第" + round + "轮淘汰赛";
        }
    }
    
    /**
     * 为小组生成全循环比赛
     */
    private void generateGroupStageMatches(MonthlyCompetition competition, CompetitionGroup group, 
                                          List<Student> players, String groupName, int startMatchNumber) {
        int playerCount = players.size();
        if (playerCount < 2) return;
        
        boolean hasGhost = playerCount % 2 == 1;
        int adjustedPlayerCount = hasGhost ? playerCount + 1 : playerCount;
        int totalRounds = adjustedPlayerCount - 1;
        int matchNumber = startMatchNumber;
        
        for (int round = 0; round < totalRounds; round++) {
            List<int[]> roundPairs = generateRoundPairs(playerCount, round, hasGhost);
            
            for (int[] pair : roundPairs) {
                CompetitionMatch match = new CompetitionMatch();
                match.setCompetition(competition);
                match.setCompetitionGroup(group);
                match.setRoundNumber(round + 1);
                match.setMatchNumber(matchNumber++);
                match.setMatchType("小组赛");
                match.setGroupName(groupName);
                
                if (pair[1] == -1) { // 轮空
                    match.setPlayer1(players.get(pair[0]));
                    match.setPlayer2(null);
                    match.setIsBye(true);
                    match.setTableNumber("轮空");
                } else {
                    match.setPlayer1(players.get(pair[0]));
                    match.setPlayer2(players.get(pair[1]));
                    match.setIsBye(false);
                    match.setTableNumber("T" + String.format("%03d", (matchNumber - startMatchNumber) + 1));
                }
                
                competitionMatchRepository.save(match);
            }
        }
    }
    
    /**
     * 计算全循环比赛的场次数
     */
    private int calculateRoundRobinMatches(int playerCount) {
        if (playerCount < 2) return 0;
        if (playerCount % 2 == 0) {
            return playerCount * (playerCount - 1) / 2;
        } else {
            return playerCount * (playerCount - 1) / 2;
        }
    }
    
    /**
     * 获取比赛赛程
     */
    public Map<String, Object> getCompetitionSchedule(Long competitionId) {
        MonthlyCompetition competition = monthlyCompetitionRepository.findById(competitionId)
            .orElseThrow(() -> new RuntimeException("比赛不存在"));
        
        // 如果赛程还未生成，先生成
        if (!competition.getScheduleGenerated()) {
            generateAndSaveCompetitionSchedule(competitionId);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("competition", competition);
        
        // 获取所有比赛安排
        List<CompetitionMatch> allMatches = competitionMatchRepository.findByCompetitionId(competitionId);
        
        // 按组别分组
        Map<CompetitionGroup, List<CompetitionMatch>> matchesByGroup = allMatches.stream()
            .collect(Collectors.groupingBy(CompetitionMatch::getCompetitionGroup));
        
        Map<String, Object> schedule = new HashMap<>();
        for (Map.Entry<CompetitionGroup, List<CompetitionMatch>> entry : matchesByGroup.entrySet()) {
            CompetitionGroup group = entry.getKey();
            List<CompetitionMatch> matches = entry.getValue();
            
            // 按轮次分组
            Map<Integer, List<CompetitionMatch>> matchesByRound = matches.stream()
                .collect(Collectors.groupingBy(CompetitionMatch::getRoundNumber));
            
            Map<String, Object> groupSchedule = new HashMap<>();
            groupSchedule.put("groupName", group.getDescription());
            groupSchedule.put("totalMatches", matches.size());
            groupSchedule.put("rounds", matchesByRound);
            
            schedule.put(group.name(), groupSchedule);
        }
        
        result.put("schedule", schedule);
        return result;
    }
    
    /**
     * 获取学员的比赛安排
     */
    public List<CompetitionMatch> getStudentMatches(Long competitionId, Long studentId) {
        return competitionMatchRepository.findByCompetitionIdAndStudentId(competitionId, studentId);
    }
    
    /**
     * 生成比赛赛程（原有方法，保持兼容性）
     */
    public Map<String, Object> generateCompetitionSchedule(Long competitionId) {
        return getCompetitionSchedule(competitionId);
    }
} 
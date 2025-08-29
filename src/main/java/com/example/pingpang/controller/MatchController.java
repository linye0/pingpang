package com.example.pingpang.controller;

import com.example.pingpang.dto.ApiResponse;
import com.example.pingpang.entity.Match;
import com.example.pingpang.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MatchController {
    
    private final MatchService matchService;
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    public ResponseEntity<ApiResponse<Match>> createMatch(
            @RequestParam Long player1Id,
            @RequestParam Long player2Id,
            @RequestParam Match.MatchType matchType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime matchTime,
            @RequestParam(required = false) String venue) {
        try {
            Match match = matchService.createMatch(player1Id, player2Id, matchType, matchTime, venue);
            return ResponseEntity.ok(ApiResponse.success("比赛创建成功", match));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Match>> getMatch(@PathVariable Long id) {
        Optional<Match> match = matchService.getMatchById(id);
        if (match.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(match.get()));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("比赛不存在"));
        }
    }
    
    @PutMapping("/{id}/start")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    public ResponseEntity<ApiResponse<Match>> startMatch(@PathVariable Long id) {
        try {
            Match match = matchService.startMatch(id);
            return ResponseEntity.ok(ApiResponse.success("比赛已开始", match));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    public ResponseEntity<ApiResponse<Match>> completeMatch(
            @PathVariable Long id,
            @RequestParam Integer player1Score,
            @RequestParam Integer player2Score,
            @RequestParam(required = false) String remarks) {
        try {
            Match match = matchService.completeMatch(id, player1Score, player2Score, remarks);
            return ResponseEntity.ok(ApiResponse.success("比赛已完成", match));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    public ResponseEntity<ApiResponse<Match>> cancelMatch(@PathVariable Long id, @RequestParam String reason) {
        try {
            Match match = matchService.cancelMatch(id, reason);
            return ResponseEntity.ok(ApiResponse.success("比赛已取消", match));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<Page<Match>>> getUserMatches(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Match> matches = matchService.getUserMatches(userId, pageable);
            return ResponseEntity.ok(ApiResponse.success(matches));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<Match>>> getTodayMatches() {
        try {
            List<Match> matches = matchService.getTodayMatches();
            return ResponseEntity.ok(ApiResponse.success(matches));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<Match>>> getUpcomingMatches() {
        try {
            List<Match> matches = matchService.getUpcomingMatches();
            return ResponseEntity.ok(ApiResponse.success(matches));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<Match>>> getMatchesByStatus(@PathVariable Match.MatchStatus status) {
        try {
            List<Match> matches = matchService.getMatchesByStatus(status);
            return ResponseEntity.ok(ApiResponse.success(matches));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<Page<Match>>> getAllMatches(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Match.MatchStatus status,
            @RequestParam(required = false) Match.MatchType matchType) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Match> matches = matchService.getAllMatchesWithFilter(pageable, status, matchType);
            return ResponseEntity.ok(ApiResponse.success(matches));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
} 
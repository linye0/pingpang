package com.example.pingpang.controller;

import com.example.pingpang.dto.ApiResponse;
import com.example.pingpang.entity.Tournament;
import com.example.pingpang.entity.User;
import com.example.pingpang.service.TournamentService;
import com.example.pingpang.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TournamentController {
    
    private final TournamentService tournamentService;
    private final UserService userService;
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    public ResponseEntity<ApiResponse<Tournament>> createTournament(@Valid @RequestBody Tournament tournament) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Optional<User> currentUser = userService.findByUsername(auth.getName());
            
            if (!currentUser.isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户未登录"));
            }
            
            Tournament createdTournament = tournamentService.createTournament(tournament, currentUser.get().getId());
            return ResponseEntity.ok(ApiResponse.success("锦标赛创建成功", createdTournament));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<Tournament>>> getAllTournaments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            if (page == 0 && size == 10) {
                // 如果是默认参数，返回所有锦标赛
                List<Tournament> tournaments = tournamentService.getAllTournaments();
                return ResponseEntity.ok(ApiResponse.success(tournaments));
            } else {
                // 分页查询
                Pageable pageable = PageRequest.of(page, size);
                Page<Tournament> tournamentPage = tournamentService.getAllTournaments(pageable);
                return ResponseEntity.ok(ApiResponse.success(tournamentPage.getContent()));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Tournament>> getTournament(@PathVariable Long id) {
        try {
            Optional<Tournament> tournament = tournamentService.getTournamentById(id);
            if (tournament.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(tournament.get()));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("锦标赛不存在"));
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PostMapping("/{id}/register")
    public ResponseEntity<ApiResponse<Tournament>> registerForTournament(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Optional<User> currentUser = userService.findByUsername(auth.getName());
            
            if (!currentUser.isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户未登录"));
            }
            
            Tournament tournament = tournamentService.registerForTournament(id, currentUser.get().getId());
            return ResponseEntity.ok(ApiResponse.success("报名成功", tournament));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}/register")
    public ResponseEntity<ApiResponse<Tournament>> unregisterFromTournament(@PathVariable Long id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Optional<User> currentUser = userService.findByUsername(auth.getName());
            
            if (!currentUser.isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("用户未登录"));
            }
            
            Tournament tournament = tournamentService.unregisterFromTournament(id, currentUser.get().getId());
            return ResponseEntity.ok(ApiResponse.success("取消报名成功", tournament));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/start")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    public ResponseEntity<ApiResponse<Tournament>> startTournament(@PathVariable Long id) {
        try {
            Tournament tournament = tournamentService.startTournament(id);
            return ResponseEntity.ok(ApiResponse.success("锦标赛已开始", tournament));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    public ResponseEntity<ApiResponse<Tournament>> completeTournament(
            @PathVariable Long id,
            @RequestParam(required = false) Long championId,
            @RequestParam(required = false) Long runnerUpId) {
        try {
            Tournament tournament = tournamentService.completeTournament(id, championId, runnerUpId);
            return ResponseEntity.ok(ApiResponse.success("锦标赛已完成", tournament));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COACH')")
    public ResponseEntity<ApiResponse<Tournament>> cancelTournament(@PathVariable Long id) {
        try {
            Tournament tournament = tournamentService.cancelTournament(id);
            return ResponseEntity.ok(ApiResponse.success("锦标赛已取消", tournament));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<Tournament>>> getTournamentsByStatus(@PathVariable Tournament.TournamentStatus status) {
        try {
            List<Tournament> tournaments = tournamentService.getTournamentsByStatus(status);
            return ResponseEntity.ok(ApiResponse.success(tournaments));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<Tournament>>> getUpcomingTournaments() {
        try {
            List<Tournament> tournaments = tournamentService.getUpcomingTournaments();
            return ResponseEntity.ok(ApiResponse.success(tournaments));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<Tournament>>> getUserTournaments(@PathVariable Long userId) {
        try {
            List<Tournament> tournaments = tournamentService.getUserTournaments(userId);
            return ResponseEntity.ok(ApiResponse.success(tournaments));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
} 
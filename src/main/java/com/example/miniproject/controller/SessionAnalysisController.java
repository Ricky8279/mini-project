package com.example.miniproject.controller;

import com.example.miniproject.model.SessionEvent;
import com.example.miniproject.model.SessionAnalytics;
import com.example.miniproject.service.EventProcessingService;
import com.example.miniproject.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionAnalysisController {

    private final EventProcessingService eventProcessingService;
    private final StorageService storageService;

    @PostMapping("/events")
    @PreAuthorize("hasRole('USER')")
    public Mono<ResponseEntity<Void>> publishEvent(@RequestBody SessionEvent event) {
        return Mono.fromRunnable(() -> eventProcessingService.publishEvent(event))
                .thenReturn(ResponseEntity.ok().build());
    }

    @GetMapping("/{sessionId}/events")
    @PreAuthorize("hasRole('USER')")
    public Mono<ResponseEntity<List<SessionEvent>>> getSessionEvents(
            @PathVariable String sessionId,
            @RequestParam(required = false) String eventType) {

        return Mono.fromCallable(() -> {
            List<SessionEvent> events = storageService.getRecentEvents(sessionId);
            if (eventType != null) {
                events = events.stream()
                        .filter(e -> e.getEventType().equals(eventType))
                        .collect(Collectors.toList());
            }
            return ResponseEntity.ok(events);
        });
    }

    @GetMapping("/{sessionId}/analytics")
    @PreAuthorize("hasRole('USER')")
    public Mono<ResponseEntity<SessionAnalytics>> getSessionAnalytics(
            @PathVariable String sessionId) {

        return Mono.fromCallable(() -> {
            List<SessionEvent> events = storageService.getRecentEvents(sessionId);
            SessionAnalytics analytics = calculateAnalytics(events);
            return ResponseEntity.ok(analytics);
        });
    }

    @GetMapping("/admin/all-analytics")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<ResponseEntity<List<SessionAnalytics>>> getAllSessionsAnalytics() {
        // 实现管理员级别的分析功能
        return Mono.just(ResponseEntity.ok(new ArrayList<>())); // 使用兼容性更好的空ArrayList
    }

    private SessionAnalytics calculateAnalytics(List<SessionEvent> events) {
        SessionAnalytics analytics = new SessionAnalytics();

        // Calculate average startup time
        OptionalDouble avgStartupTime = events.stream()
                .filter(e -> e.getMetrics().getStartupTimeMs() != null)
                .mapToLong(e -> e.getMetrics().getStartupTimeMs())
                .average();
        analytics.setAverageStartupTimeMs(avgStartupTime.orElse(0.0));

        // Calculate total buffering time
        long totalBuffering = events.stream()
                .filter(e -> e.getMetrics().getBufferingDurationMs() != null)
                .mapToLong(e -> e.getMetrics().getBufferingDurationMs())
                .sum();
        analytics.setTotalBufferingMs(totalBuffering);

        // Calculate average bitrate
        OptionalDouble avgBitrate = events.stream()
                .filter(e -> e.getMetrics().getBitrate() != null)
                .mapToInt(e -> e.getMetrics().getBitrate())
                .average();
        analytics.setAverageBitrateKbps(avgBitrate.orElse(0.0));

        // Count errors
        long errorCount = events.stream()
                .mapToInt(e -> e.getMetrics().getErrorCount())
                .sum();
        analytics.setTotalErrors(errorCount);

        return analytics;
    }
}
package com.azhi.controller;

import com.azhi.pojo.Result;
import com.azhi.service.VisitorStatsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/visitor-stats")
@RequiredArgsConstructor
public class VisitorStatsController {

    private final VisitorStatsService visitorStatsService;

    @PostMapping("/record")
    public Result<Map<String, Long>> recordVisitor(HttpServletRequest request) {
        visitorStatsService.recordVisitor(getClientIp(request));
        return Result.success(Map.of("totalVisitors", visitorStatsService.getTotalVisitors()));
    }

    @GetMapping("/total")
    public Result<Map<String, Long>> getTotalVisitors() {
        return Result.success(Map.of("totalVisitors", visitorStatsService.getTotalVisitors()));
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }
}

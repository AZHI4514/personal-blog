package com.example.blog.agent.controller;

import com.example.blog.agent.service.RoomMemoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/room-agent")
public class RoomAgentController {

    private final RoomMemoryService roomMemoryService;

    public RoomAgentController(RoomMemoryService roomMemoryService) {
        this.roomMemoryService = roomMemoryService;
    }

    @GetMapping("/world")
    public ResponseEntity<?> world(
        @RequestParam(required = false) Double lat,
        @RequestParam(required = false) Double lon,
        @RequestParam(required = false) String timezone
    ) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int month = now.getMonthValue();
        String season = month >= 3 && month <= 5 ? "spring" : month >= 6 && month <= 8 ? "summer" : month >= 9 && month <= 11 ? "autumn" : "winter";
        String timePhase = hour >= 5 && hour < 8 ? "dawn" : hour >= 8 && hour < 17 ? "day" : hour >= 17 && hour < 20 ? "dusk" : "night";

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("weather", "clear");
        data.put("weatherCode", 0);
        data.put("temperature", 26);
        data.put("windSpeed", 3);
        data.put("timePhase", timePhase);
        data.put("season", season);
        data.put("city", "Room");
        data.put("locationSource", lat != null && lon != null ? "browser-geolocation" : "fallback");
        data.put("updatedAt", java.time.OffsetDateTime.now().toString());

        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    @GetMapping("/memory")
    public ResponseEntity<?> listMemory(
        @RequestHeader(value = "X-User-Id", defaultValue = "guest") String userId,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String type,
        @RequestParam(defaultValue = "20") int limit
    ) {
        Object data = (q != null && !q.isBlank())
            ? roomMemoryService.searchMemories(userId, q, limit)
            : roomMemoryService.listMemories(userId, type, limit);
        return ResponseEntity.ok(Map.of("success", true, "data", data));
    }

    @PostMapping("/memory")
    public ResponseEntity<?> saveMemory(
        @RequestHeader(value = "X-User-Id", defaultValue = "guest") String userId,
        @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.ok(Map.of("success", true, "data", roomMemoryService.recordMemory(userId, body)));
    }

    @PatchMapping("/memory/{id}")
    public ResponseEntity<?> updateMemory(
        @RequestHeader(value = "X-User-Id", defaultValue = "guest") String userId,
        @PathVariable String id,
        @RequestBody Map<String, Object> body
    ) {
        return ResponseEntity.ok(Map.of("success", true, "data", roomMemoryService.updateMemory(userId, id, body)));
    }

    @DeleteMapping("/memory/{id}")
    public ResponseEntity<?> deleteMemory(
        @RequestHeader(value = "X-User-Id", defaultValue = "guest") String userId,
        @PathVariable String id
    ) {
        return ResponseEntity.ok(Map.of("success", true, "data", Map.of("count", roomMemoryService.deleteMemory(userId, id))));
    }

    @PostMapping("/mcp/call")
    public ResponseEntity<?> callMcp(@RequestBody Map<String, Object> body) {
        String name = String.valueOf(body.getOrDefault("name", ""));
        if (!"understand_image".equals(name) && !"web_search".equals(name)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "tool not allowed"));
        }
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", Map.of("text", "template mcp result for " + name, "raw", body)
        ));
    }
}


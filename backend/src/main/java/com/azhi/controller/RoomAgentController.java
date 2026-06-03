package com.azhi.controller;

import com.azhi.pojo.Result;
import com.azhi.service.RoomAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/room-agent")
@RequiredArgsConstructor
public class RoomAgentController {

    private final RoomAgentService roomAgentService;

    @GetMapping("/world")
    public Result<Map<String, Object>> world(
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false) String timezone
    ) {
        return Result.success(roomAgentService.buildWorld(lat, lon, timezone));
    }

    @GetMapping("/config")
    public Result<Map<String, Object>> getConfig() {
        return Result.success(roomAgentService.getConfigView());
    }

    @PutMapping("/config")
    public Result<Map<String, Object>> saveConfig(
            @RequestHeader(value = "X-Admin-User", defaultValue = "") String adminUser,
            @RequestBody Map<String, Object> body
    ) {
        return Result.success(roomAgentService.saveConfig(adminUser, body));
    }

    @GetMapping("/memory")
    public Result<?> listMemory(
            @RequestHeader(value = "X-User-Id", defaultValue = "guest") String userId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "20") int limit
    ) {
        Object data = (q != null && !q.isBlank())
                ? roomAgentService.searchMemories(userId, q, limit)
                : roomAgentService.listMemories(userId, type, limit);
        return Result.success(data);
    }

    @PostMapping("/memory")
    public Result<?> saveMemory(
            @RequestHeader(value = "X-User-Id", defaultValue = "guest") String userId,
            @RequestBody Map<String, Object> body
    ) {
        return Result.success(roomAgentService.recordMemory(userId, body));
    }

    @PatchMapping("/memory/{id}")
    public Result<?> updateMemory(
            @RequestHeader(value = "X-User-Id", defaultValue = "guest") String userId,
            @PathVariable String id,
            @RequestBody Map<String, Object> body
    ) {
        return Result.success(roomAgentService.updateMemory(userId, id, body));
    }

    @DeleteMapping("/memory/{id}")
    public Result<Map<String, Integer>> deleteMemory(
            @RequestHeader(value = "X-User-Id", defaultValue = "guest") String userId,
            @PathVariable String id
    ) {
        return Result.success(Map.of("count", roomAgentService.deleteMemory(userId, id)));
    }

    @DeleteMapping("/memory")
    public Result<Map<String, Integer>> deleteAllMemory(
            @RequestHeader(value = "X-User-Id", defaultValue = "guest") String userId
    ) {
        return Result.success(Map.of("count", roomAgentService.deleteAllMemories(userId)));
    }

    @PostMapping("/mcp/call")
    public Result<?> callMcp(@RequestBody Map<String, Object> body) {
        return Result.success(roomAgentService.callAllowedTool(body));
    }

    @PostMapping("/chat")
    public Result<?> chat(@RequestBody Map<String, Object> body) {
        return Result.success(roomAgentService.chat(body));
    }
}

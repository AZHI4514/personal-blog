package com.example.blog.agent.service;

import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RoomMemoryService {

    private static final int VECTOR_SIZE = 96;
    private final Map<String, List<Map<String, Object>>> store = new ConcurrentHashMap<>();

    public Object recordMemory(String userId, Map<String, Object> payload) {
        Map<String, Object> candidate = buildMemoryCandidate(payload);
        if (candidate == null) return null;
        userBucket(userId).add(0, candidate);
        return Map.of("action", "created", "memory", candidate);
    }

    public List<Map<String, Object>> listMemories(String userId, String type, int limit) {
        return userBucket(userId).stream()
            .filter(item -> type == null || type.isBlank() || type.equals(item.get("type")))
            .limit(Math.max(1, Math.min(limit, 200)))
            .toList();
    }

    public List<Map<String, Object>> searchMemories(String userId, String query, int limit) {
        List<Double> vector = createEmbedding(query);
        return userBucket(userId).stream()
            .map(item -> {
                Map<String, Object> copy = new LinkedHashMap<>(item);
                @SuppressWarnings("unchecked")
                List<Double> embedding = (List<Double>) copy.get("embedding");
                double score = similarity(vector, embedding) + ((Number) copy.getOrDefault("importance", 0.0)).doubleValue() * 0.2;
                copy.put("score", score);
                return copy;
            })
            .sorted((a, b) -> Double.compare(((Number) b.get("score")).doubleValue(), ((Number) a.get("score")).doubleValue()))
            .limit(Math.max(1, Math.min(limit, 20)))
            .toList();
    }

    public Map<String, Object> updateMemory(String userId, String id, Map<String, Object> payload) {
        List<Map<String, Object>> bucket = userBucket(userId);
        for (int i = 0; i < bucket.size(); i++) {
            Map<String, Object> current = bucket.get(i);
            if (Objects.equals(current.get("id"), id)) {
                Map<String, Object> next = new LinkedHashMap<>(current);
                next.put("type", String.valueOf(payload.getOrDefault("type", current.get("type"))));
                next.put("summary", String.valueOf(payload.getOrDefault("summary", current.get("summary"))));
                next.put("content", String.valueOf(payload.getOrDefault("content", current.get("content"))));
                next.put("updatedAt", OffsetDateTime.now().toString());
                next.put("embedding", createEmbedding(next.get("summary") + "\n" + next.get("content")));
                bucket.set(i, next);
                return next;
            }
        }
        return null;
    }

    public int deleteMemory(String userId, String id) {
        List<Map<String, Object>> bucket = userBucket(userId);
        int before = bucket.size();
        bucket.removeIf(item -> Objects.equals(item.get("id"), id));
        return before - bucket.size();
    }

    private List<Map<String, Object>> userBucket(String userId) {
        return store.computeIfAbsent(userId, key -> new ArrayList<>());
    }

    private Map<String, Object> buildMemoryCandidate(Map<String, Object> payload) {
        String content = String.valueOf(payload.getOrDefault(
            "content",
            "用户：" + payload.getOrDefault("userMessage", "") + "\n八千代：" + payload.getOrDefault("assistantReply", "")
        )).trim();
        if (content.length() < 12) return null;

        String summary = String.valueOf(payload.getOrDefault("summary", content));
        String type = inferMemoryType(content);
        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("id", UUID.randomUUID().toString());
        memory.put("type", type);
        memory.put("summary", summary.length() > 500 ? summary.substring(0, 500) : summary);
        memory.put("content", content.length() > 4000 ? content.substring(0, 4000) : content);
        memory.put("importance", estimateImportance(content));
        memory.put("embedding", createEmbedding(summary + "\n" + content));
        memory.put("createdAt", OffsetDateTime.now().toString());
        memory.put("updatedAt", OffsetDateTime.now().toString());
        return memory;
    }

    private String inferMemoryType(String text) {
        if (text.matches(".*(我叫|叫我|我的名字|我是).*")) return "profile";
        if (text.matches(".*(喜欢|偏好|希望|不要|风格|主题).*")) return "preference";
        if (text.matches(".*(项目|网站|功能|计划|开发|部署).*")) return "project";
        if (text.matches(".*(上次|刚才|昨天|今天|继续|完成|报错).*")) return "episodic";
        return "conversation";
    }

    private double estimateImportance(String text) {
        double score = 0.42;
        if (text.matches(".*(记住|以后|喜欢|偏好|名字|重要|不要).*")) score += 0.28;
        if (text.length() > 120) score += 0.08;
        return Math.min(1.0, score);
    }

    private List<Double> createEmbedding(String text) {
        double[] vector = new double[VECTOR_SIZE];
        for (String token : tokenize(text)) {
          int hash = hashString(token);
          int slot = Math.floorMod(hash, VECTOR_SIZE);
          vector[slot] += (hash & 1) == 1 ? 1 : -1;
        }
        double norm = 0;
        for (double value : vector) norm += value * value;
        norm = Math.sqrt(norm);
        if (norm == 0) norm = 1;
        List<Double> output = new ArrayList<>(VECTOR_SIZE);
        for (double value : vector) output.add(Math.round((value / norm) * 1_000_000d) / 1_000_000d);
        return output;
    }

    private double similarity(List<Double> left, List<Double> right) {
        if (left == null || right == null) return 0;
        double score = 0;
        for (int i = 0; i < Math.min(left.size(), right.size()); i++) score += left.get(i) * right.get(i);
        return score;
    }

    private List<String> tokenize(String text) {
        String source = String.valueOf(text).toLowerCase(Locale.ROOT);
        List<String> words = new ArrayList<>();
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("[a-z0-9_]+|[\\u4e00-\\u9fff]").matcher(source);
        while (matcher.find()) words.add(matcher.group());
        List<String> grams = new ArrayList<>();
        for (int i = 0; i < words.size() - 1; i++) grams.add(words.get(i) + words.get(i + 1));
        words.addAll(grams);
        return words;
    }

    private int hashString(String value) {
        int hash = 0x811c9dc5;
        for (int i = 0; i < value.length(); i++) {
            hash ^= value.charAt(i);
            hash *= 16777619;
        }
        return hash;
    }
}

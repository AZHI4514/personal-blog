package com.azhi.service.roomagent;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class RoomAgentWorldService {

    public Map<String, Object> buildWorld(Double lat, Double lon, String timezone) {
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
        data.put("city", "Game Room");
        data.put("locationSource", lat != null && lon != null ? "browser-geolocation" : "fallback");
        data.put("timezone", timezone == null || timezone.isBlank() ? "Asia/Shanghai" : timezone);
        data.put("updatedAt", OffsetDateTime.now().toString());
        return data;
    }
}

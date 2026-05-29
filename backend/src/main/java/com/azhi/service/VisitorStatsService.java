package com.azhi.service;

public interface VisitorStatsService {
    void recordVisitor(String ip);

    Long getTotalVisitors();
}

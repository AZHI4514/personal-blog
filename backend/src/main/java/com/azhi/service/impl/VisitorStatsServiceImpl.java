package com.azhi.service.impl;

import com.azhi.mapper.VisitorStatMapper;
import com.azhi.service.VisitorStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VisitorStatsServiceImpl implements VisitorStatsService {

    private final VisitorStatMapper visitorStatMapper;

    @Override
    @Transactional
    public void recordVisitor(String ip) {
        visitorStatMapper.recordVisitor(ip);
    }

    @Override
    public Long getTotalVisitors() {
        Long total = visitorStatMapper.getTotalVisitors();
        return total == null ? 0L : total;
    }
}

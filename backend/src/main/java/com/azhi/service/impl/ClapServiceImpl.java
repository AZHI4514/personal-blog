package com.azhi.service.impl;

import com.azhi.mapper.ClapMapper;
import com.azhi.service.ClapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClapServiceImpl implements ClapService {
    private final ClapMapper clapMapper;

    @Override
    @Transactional
    public void clap() {
        clapMapper.incrementClap();
    }
}

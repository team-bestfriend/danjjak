package com.bestfriend.danjjak.health.service;

import com.bestfriend.danjjak.health.mapper.HealthMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HealthService {

    private final HealthMapper healthMapper;

    public HealthService(HealthMapper healthMapper) {
        this.healthMapper = healthMapper;
    }

    @Transactional(readOnly = true)
    public boolean isDatabaseReady() {
        return healthMapper.selectOne() == 1;
    }
}


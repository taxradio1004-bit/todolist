package com.portfolio.todo.service;

import org.springframework.stereotype.Service;

import com.portfolio.todo.mapper.TestMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TestService {

    private final TestMapper testMapper;

    public String getServiceStatusMessage() {
        return testMapper.getServiceStatusMessage();
    }
}

package com.portfolio.todo.service;

import com.portfolio.todo.dto.CreateTodoRequest;
import org.springframework.stereotype.Service;

import com.portfolio.todo.mapper.TestMapper;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;


@Service
@RequiredArgsConstructor
public class TestService {

    private final TestMapper testMapper;

    public String getServiceStatusMessage() {
        return testMapper.getServiceStatusMessage();
    }
    public List<HashMap<String, Object>> getTodo() {
        return testMapper.getTodo();
    }

    public String postTodo(CreateTodoRequest request) {
        return testMapper.postTodo(request);
    }
}

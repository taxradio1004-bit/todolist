package com.portfolio.todo.mapper;

import com.portfolio.todo.dto.CreateTodoRequest;
import org.apache.ibatis.annotations.Mapper;

import java.util.HashMap;
import java.util.List;

@Mapper
public interface TestMapper {
    String getServiceStatusMessage();

    List<HashMap<String, Object>> getTodo();

    String postTodo(CreateTodoRequest request);
}

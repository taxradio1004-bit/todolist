package com.portfolio.todo.mapper;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestMapper {
    String getServiceStatusMessage();
}

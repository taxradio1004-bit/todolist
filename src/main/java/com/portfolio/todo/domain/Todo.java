package com.portfolio.todo.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.*;
import org.apache.ibatis.type.Alias;

@Alias("Todo")
@Data
public class Todo {
    private UUID id;
    private String title;
    private String description;
    private boolean completed;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

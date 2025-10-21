package com.portfolio.todo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "todo Object")
public class CreateTodoRequest {
    private String title;
    private String description;
    private Boolean completed;
}

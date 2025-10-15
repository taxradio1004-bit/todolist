package com.portfolio.todo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/tasks")
    public String tasks() {
        return "Todo API is running!";
    }
}

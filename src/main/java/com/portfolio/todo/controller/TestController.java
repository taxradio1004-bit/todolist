package com.portfolio.todo.controller;
@RestController
public class TestController {

        @GetMapping("/tasks")
        public String tasks() {
            return "Todo API is running!";
        }
    }
}

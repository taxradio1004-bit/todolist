package com.portfolio.todo.controller;

import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.todo.service.TestService;

import java.util.HashMap;
import java.util.List;

@Tag(
        name = "Test",
        description = "Todo API 서비스가 정상적으로 동작하는지 확인할 수 있는 간단한 엔드포인트를 제공합니다."
)
@RestController
@RequiredArgsConstructor
public class TestController {
    private final TestService testService;

    @Operation(
            summary = "서비스 헬스 체크",
            description = "Todo API가 정상적으로 동작 중인지 확인할 수 있는 간단한 메시지를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "서비스가 정상적으로 기동되어 요청에 응답했습니다.",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    @GetMapping("/tasks")
    public String tasks() {
        return testService.getServiceStatusMessage();
    }
    @Operation(
            summary = "DB 헬스 체크",
            description = "Todo API가 정상적으로 동작 중인지 확인할 수 있는 간단한 메시지를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "서비스가 정상적으로 기동되어 요청에 응답했습니다.",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    @GetMapping("/todo")
    public List<HashMap<String, Object>> getTodo() {
        return testService.getTodo();
    }
    @Operation(
            summary = "DB 입력",
            description = "Todo API가 정상적으로 동작 중인지 확인할 수 있는 간단한 메시지를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "서비스가 정상적으로 기동되어 요청에 응답했습니다.",
                    content = @Content(schema = @Schema(implementation = String.class))
            )
    })
    @PostMapping("/todo")
    public String postTodo() {
        return testService.postTodo();
    }
}

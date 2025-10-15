# HandlerExceptionResolver & AOP 정리

Spring MVC는 예외를 처리하기 위해 `HandlerExceptionResolver` 체인과 AOP 기반 횡단 관심사를 함께 활용할 수 있습니다. Todo API 프로젝트를 예로 들며 정리합니다.

## HandlerExceptionResolver 흐름

1. 컨트롤러 진입 전에 발생한 예외는 `Filter`, `Interceptor` 단계에서 바로 처리되거나 전파됩니다.
2. 컨트롤러/서비스에서 예외가 발생하면 `DispatcherServlet`이 `HandlerExceptionResolver` 구현체들을 순서대로 실행합니다.
3. 가장 먼저 등록된 `ExceptionHandlerExceptionResolver`가 `@ControllerAdvice` / `@ExceptionHandler` 메서드를 탐색해 처리합니다.
4. 처리하지 못한 예외는 `ResponseStatusExceptionResolver`, `DefaultHandlerExceptionResolver` 등이 순차적으로 시도합니다.
5. 모든 Resolver가 처리하지 못하면 WAS(Tomcat)까지 전파되어 기본 에러 페이지가 반환됩니다.

### 주요 Resolver

- **ExceptionHandlerExceptionResolver**  
  `@RestControllerAdvice`(또는 `@ControllerAdvice`)와 `@ExceptionHandler` 메서드를 실행합니다.

- **ResponseStatusExceptionResolver**  
  `@ResponseStatus`가 붙은 예외를 HTTP 상태 코드로 변환합니다.

- **DefaultHandlerExceptionResolver**  
  스프링이 제공하는 표준 예외(`HttpRequestMethodNotSupportedException` 등)를 적절한 상태 코드로 매핑합니다.

## 실무 구성 예시

```java
@RestControllerAdvice
public class GlobalExceptionAdvice {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        return ResponseEntity
                .status(ex.status())
                .body(ApiError.of(ex.code(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnknown(Exception ex) {
        return ResponseEntity
                .internalServerError()
                .body(ApiError.of("INTERNAL_ERROR", "알 수 없는 오류가 발생했습니다."));
    }
}
```

```java
public record ApiError(String code, String message) {
    public static ApiError of(String code, String message) {
        return new ApiError(code, message);
    }
}
```

## HandlerExceptionResolver 커스터마이징

- `HandlerExceptionResolver` 인터페이스를 직접 구현하거나 `AbstractHandlerExceptionResolver`를 상속받아 우선순위를 조정할 수 있습니다.
- `Ordered.HIGHEST_PRECEDENCE`로 등록하면 기존 Resolver보다 먼저 실행되어 특정 예외에 대한 응답 규칙을 강제할 수 있습니다.
- 예: 감사 로깅, 모니터링 시스템 연동, 다국어 메시지 변환 등.

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class MonitoringExceptionResolver extends AbstractHandlerExceptionResolver {

    private final MonitoringClient monitoringClient;

    public MonitoringExceptionResolver(MonitoringClient monitoringClient) {
        this.monitoringClient = monitoringClient;
    }

    @Override
    protected ModelAndView doResolveException(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        monitoringClient.send(ex, request.getRequestURI());
        return null; // 다음 Resolver에게 위임
    }
}
```

## AOP와의 연계

AOP(Aspect Oriented Programming)는 공통 관심사를 모듈화하여 Service/Repository 등에서 반복되는 코드를 줄입니다. 예외 처리와 함께 사용하면 다음과 같은 패턴을 구현할 수 있습니다.

### 1. 트랜잭션 처리
`@Transactional` 자체가 AOP 기반으로, 메서드 실행 전후로 프록시가 트랜잭션을 열고 닫습니다. 예외 발생 시 롤백 여부를 결정합니다.

### 2. 로깅/모니터링 어드바이스
```java
@Aspect
@Component
public class LoggingAspect {

    @Around("execution(* com.example.todo.service..*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return joinPoint.proceed();
        } catch (Exception ex) {
            // 예외를 다시 던져 HandlerExceptionResolver가 처리하도록 위임
            throw ex;
        } finally {
            long elapsed = System.currentTimeMillis() - start;
            log.info("[{}] {} ms", joinPoint.getSignature(), elapsed);
        }
    }
}
```

### 3. 사용자 정의 예외 변환
`@Around` 또는 `@AfterThrowing` 어드바이스에서 도메인 예외로 변환한 뒤 던지면, `@RestControllerAdvice`가 일관된 응답을 만들 수 있습니다.

```java
@Aspect
@Component
public class ExceptionMappingAspect {

    @AfterThrowing(pointcut = "execution(* com.example.todo.repository..*(..))", throwing = "ex")
    public void translate(Exception ex) {
        if (ex instanceof DataIntegrityViolationException violation) {
            throw new BusinessException("TODO_DUPLICATE", "할 일이 중복되었습니다.", HttpStatus.CONFLICT, violation);
        }
    }
}
```

## 정리

- `HandlerExceptionResolver`는 Spring MVC 레벨에서 예외를 HTTP 응답으로 바꿔주는 파이프라인입니다.
- `@RestControllerAdvice`를 활용하면 대부분의 예외 처리를 선언적으로 구현할 수 있습니다.
- AOP는 로깅, 모니터링, 예외 변환, 트랜잭션 제어 등 횡단 관심사를 분리하여 서비스 코드의 가독성을 높여줍니다.
- 두 기능을 조합하면 예외 흐름을 중앙집중식으로 관리하면서도, 각 레이어의 책임을 깔끔하게 유지할 수 있습니다.

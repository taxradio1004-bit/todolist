# Spring Boot 데이터 흐름도

Todo API를 예로 들어, 클라이언트 요청이 내장 Tomcat부터 컨트롤러·서비스·데이터베이스까지 어떤 흐름을 거쳐 처리되는지 정리했습니다. Servlet Filter와 Spring MVC Interceptor 단계도 포함되어 있습니다.

```text
Client (웹/모바일)
      |
      | HTTPS 요청 (POST /api/v1/todos)
      v
Embedded Tomcat (Servlet Container)
      |
      | --> Servlet Filter Chain
      |     - 공통 로깅/트레이싱 필터
      |     - Spring Security Filter Chain (옵션)
      |     - CORS, GZip 등 추가 필터
      v
Spring DispatcherServlet
      |
      | --> HandlerInterceptor (preHandle)
      |       - 인증 이후 추가 검증, 멱등성 체크, 요청 로깅 등
      v
Controller (@RestController, TodoController)
      |
      | Bean Validation, DTO ↔ Domain 변환
      v
Service (@Service, TodoService)
      |
      | 비즈니스 로직, 트랜잭션(@Transactional)
      v
Repository (Spring Data JPA / MyBatis)
      |
      | SQL 실행
      v
Database (PostgreSQL)
      |
      | 결과 반환
      v
Repository
      |
      v
Service
      |
      | 응답 DTO 생성, 이벤트 발행(Optional)
      v
Controller
      |
      <---- HandlerInterceptor (postHandle / afterCompletion)
                      |
                      v
            DispatcherServlet → Filter Chain → Tomcat
                      |
                      | HTTP Response(JSON)
                      v
              Client (웹/모바일)
```

## 단계별 설명

- **네트워크 & 컨테이너**
  - 클라이언트 요청은 CDN/프록시를 거쳐 내장 Tomcat의 커넥터(기본 8080 포트)로 전달됩니다.
  - Tomcat이 Servlet 스펙에 따라 요청을 `HttpServletRequest` 객체로 만들어 Filter 체인에 위임합니다.

- **Servlet Filter Chain**
  - `javax.servlet.Filter` 구현체가 체인 형태로 구성되어 공통 전·후처리를 담당합니다.
  - Spring Security, CORS, 로깅, 트레이싱, 멀티 테넌시 필터 등이 이 계층에 위치합니다.

- **DispatcherServlet & HandlerInterceptor**
  - `DispatcherServlet`이 Spring MVC 진입점입니다.
  - 요청 매핑 정보를 찾기 전/후로 `HandlerInterceptor`의 `preHandle`과 `postHandle/afterCompletion`이 실행되어 인증/인가, 요청 로깅, 응답 가공 등을 수행합니다.

- **Controller → Service**
  - 컨트롤러는 DTO를 받고 `@Valid` 검증을 수행한 뒤 서비스 계층을 호출합니다.
  - 서비스는 트랜잭션 경계를 생성하고 도메인 규칙을 적용합니다.

- **Service → Repository → Database**
  - Spring Data JPA 사용 시 메서드 이름 기반 쿼리 또는 `@Query`, QueryDSL을 통해 SQL을 실행합니다.
  - MyBatis라면 매퍼 인터페이스와 XML 매퍼를 통해 SQL을 실행합니다.
  - PostgreSQL이 실제 데이터 읽기/쓰기 작업을 수행합니다.

- **응답 경로**
  - 서비스가 엔터티를 DTO로 변환하고 컨트롤러가 반환합니다.
  - `HandlerInterceptor`의 `afterCompletion`이 마지막 후처리를 수행한 뒤 `DispatcherServlet`이 응답을 `HttpServletResponse`에 작성합니다.
  - 응답은 필터 체인을 역순으로 거쳐 Tomcat → 프록시 → 클라이언트로 전달됩니다.

## 추가 고려 사항

- **비동기 처리**: `@Async`, `WebFlux`, 메시지 큐(Kafka/RabbitMQ) 등을 사용하면 Service 이후 흐름이 비동기 파이프라인으로 확장됩니다.
- **예외 처리**: `@RestControllerAdvice`와 `HandlerExceptionResolver`가 예외를 가로채 공통 에러 응답을 생성합니다.
- **모니터링**: Filter/Interceptor 단계에서 Trace ID를 주입해 APM, 로그, Metrics와 연계하면 진단이 쉬워집니다.

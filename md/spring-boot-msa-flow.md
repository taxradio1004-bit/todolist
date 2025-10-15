# Spring Cloud MSA 요청 흐름도

Spring Cloud 기반 마이크로서비스 아키텍처에서 API Gateway·Discovery·Config Server를 포함한 요청 흐름을 ASCII 다이어그램으로 정리했습니다.

```text
Client (웹/모바일)
      |
      | HTTPS 요청 (api.example.com/orders)
      v
API Gateway (Spring Cloud Gateway)
      |
      | 라우팅/필터 적용 (인증, Rate Limit, 로깅)
      v
Service Discovery (Eureka/Consul)
      |
      | 대상 서비스 인스턴스 조회
      v
Order Service (Spring Boot Microservice)
      |
      | --> Config Client (Spring Cloud Config)
      |       - Config Server에서 환경 프로퍼티 fetch
      |
      | 내장 Tomcat
      |   └─ Security Filter Chain (JWT 등)
      |   └─ Servlet Filter (로깅, CORS)
      |   └─ DispatcherServlet
      |         └─ HandlerInterceptor
      |               └─ Controller → Service → Repository → Database
      |
      | 필요 시 다른 서비스 호출 (Feign/WebClient)
      v
Inventory Service / Payment Service …
      |
      | 역시 Discovery를 통해 호출, Config Server에서 설정 주입
      v
Database / 외부 시스템

응답 흐름: 하위 서비스 → Order Service → API Gateway → Client
```

## 단계별 설명

- **API Gateway**
  - 클라이언트 모든 요청이 Gateway를 통과합니다.
  - 인증/JWT 검증, Rate Limiting, 공통 헤더 추가, 트레이싱 ID 삽입 등을 필터로 처리합니다.
  - 라우팅 규칙에 따라 적절한 마이크로서비스로 전달합니다.

- **Service Discovery**
  - Gateway와 각 마이크로서비스는 Eureka/Consul 같은 Discovery 서버와 통신하여 인스턴스 목록을 가져옵니다.
  - 라이브 인스턴스를 동적으로 찾기 때문에 스케일 인/아웃 시 설정 변경 없이도 라우팅이 유지됩니다.

- **Config Server**
  - 모든 마이크로서비스는 부팅 시 `bootstrap.properties` 또는 `bootstrap.yml`을 통해 Config Server에 등록된 환경 값을 가져옵니다.
  - 설정 변경이 있을 경우 `/actuator/refresh` 또는 Spring Cloud Bus를 사용해 동적으로 갱신할 수 있습니다.

- **개별 서비스 (예: Order Service)**
  - Gateway에서 전달받은 요청을 내장 Tomcat이 처리하고, 보안/로깅 필터와 HandlerInterceptor를 거친 후 컨트롤러가 비즈니스 로직을 수행합니다.
  - 다른 서비스 호출이 필요하면 `OpenFeign` 또는 `WebClient`를 사용하며, 이때도 Discovery를 통해 대상 서비스의 위치를 조회합니다.
  - 각각의 서비스는 자체 데이터베이스나 외부 시스템과 연동합니다.

- **응답 경로**
  - 서비스에서 생성한 응답이 Gateway로 되돌아오면, Gateway가 필요한 후처리(헤더 추가, 변환 등)를 거친 뒤 클라이언트에 반환합니다.

## 추가 고려 사항

- **Circuit Breaker & Resilience**
  - `Resilience4j` 또는 Spring Cloud CircuitBreaker를 사용해 서비스 간 호출 안정성을 확보합니다.

- **Observability**
  - Sleuth/Zipkin, Micrometer/Prometheus 등을 Gateway 및 각 서비스에 적용하여 분산 추적과 모니터링을 구현합니다.

- **Security**
  - 중앙 인증 서버(OAuth2, Keycloak 등)와 연동하여 Gateway 또는 각 서비스에서 토큰 검증을 수행합니다.


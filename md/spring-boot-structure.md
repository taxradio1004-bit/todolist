# Spring Boot 프로젝트 구조 안내

다음은 간단한 Todo 애플리케이션을 기준으로 한 Spring Boot 프로젝트 구조 예시입니다. 레이어별 역할과 주요 파일을 정리했습니다.

```text
src
└── main
    ├── java
    │   └── com.example.todo
    │       ├── TodoApplication.java        # 애플리케이션 시작점 (Spring Boot 메인 클래스)
    │       ├── config                      # 공통 설정(@Configuration) 모음
    │       ├── domain
    │       │   ├── entity                  # JPA 엔터티 정의
    │       │   ├── repository              # JPA/MyBatis 레포지토리 인터페이스
    │       │   └── service                 # 트랜잭션·도메인 로직
    │       ├── web
    │       │   ├── controller              # REST 컨트롤러, 요청/응답 DTO
    │       │   └── advice                  # 예외 처리(@RestControllerAdvice)
    │       └── support                     # 공용 유틸, 공통 컴포넌트
    └── resources
        ├── application.properties          # 환경 설정(DB, JPA, 포트 등)
        ├── static                          # 정적 리소스(css, js)
        ├── templates                       # Thymeleaf 등 서버 템플릿
        └── mapper                          # MyBatis XML 매퍼 (옵션)
```

## 주요 레이어 설명

- **Application** (`TodoApplication.java`)
  - `@SpringBootApplication`이 선언된 엔트리 포인트.
  - 컴포넌트 스캔 베이스 패키지를 결정하여 하위 패키지 Bean을 자동 등록.

- **Config**
  - 데이터소스, 시큐리티, 메시지 컨버터 같은 전역 설정을 `@Configuration` 클래스로 관리.
  - 환경별 설정은 `application-{profile}.properties`와 함께 사용.

- **Domain**
  - `entity`: `@Entity`로 데이터베이스 테이블과 매핑되는 객체.
  - `repository`: Spring Data JPA 인터페이스(`CrudRepository`, `JpaRepository`) 또는 MyBatis 매퍼.
  - `service`: `@Service` 클래스에서 트랜잭션 경계를 정의하고 비즈니스 규칙을 구현.

- **Web**
  - `controller`: `@RestController`로 HTTP 요청을 받고 DTO 변환 및 서비스 호출을 담당.
  - `advice`: 전역 예외 처리, API 응답 표준화.

- **Support**
  - 공통 예외, 로깅, 유틸리티, AOP 등 횡단 관심사를 위치.

- **Resources**
  - `application.properties`: 데이터베이스 연결, JPA 옵션, 포트 등 환경 설정.
  - `mapper`: MyBatis XML 파일 저장 위치.
  - `static`/`templates`: 프론트 자원 또는 서버 템플릿이 필요할 때 사용.

## 확장 팁

- 환경 분리: `application-dev.properties`, `application-prod.properties` 등 프로필을 나눠 관리합니다.
- 테스트 구조: `src/test/java` 아래 동일한 패키지 구조로 `@SpringBootTest`, `@WebMvcTest` 등을 구성합니다.
- 모듈화: 도메인이 커지면 Gradle 멀티모듈로 `api`, `core`, `batch` 등을 분리해 유지보수성을 높일 수 있습니다.

이 구조를 기반으로 팀 규칙에 맞춰 패키지 네이밍이나 레이어를 조정하면 대부분의 Spring Boot 프로젝트를 효율적으로 관리할 수 있습니다.

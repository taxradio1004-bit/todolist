# Spring Boot Start

- **개념**
  - Spring vs Spring Boot 차이
  - 자동 구성, 의존성 관리, 내장 서버 특징
  - 이번 세션 학습 목표 공유
- **프로젝트 생성**
  - Spring Initializr 실습(Gradle, Java 17, Spring Web, Spring Data JPA, PostgreSQL Driver)
  - 생성된 프로젝트 구조와 핵심 파일 설명
- **REST 컨트롤러 만들기**
  - `@RestController`, `@GetMapping`으로 간단한 API 구현
  - `application.properties` 기본 설정
  - 내장 Tomcat 구동 및 브라우저/Postman 테스트
  - **샘플 코드**
    ```java
    package com.example.demo.web;

    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RestController;

    @RestController
    public class HelloController {

        @GetMapping("/tasks")
        public String tasks() {
            return "Todo API is running!";
        }
    }
    ```
    ```properties
    # src/main/resources/application.properties
    server.port=8080
    ```
- **데이터 계층**
  - PostgreSQL 연결 환경 구성(Docker 컨테이너 또는 로컬 인스턴스)
  - `CrudRepository`를 활용한 엔터티 저장/조회 실습
  - **샘플 코드**
    ```java
    package com.example.demo.domain;

    import jakarta.persistence.Entity;
    import jakarta.persistence.GeneratedValue;
    import jakarta.persistence.GenerationType;
    import jakarta.persistence.Id;

    @Entity
    public class Todo {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        private String title;

        private boolean completed;

        protected Todo() {
        }

        public Todo(String title, boolean completed) {
            this.title = title;
            this.completed = completed;
        }

        public Long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public boolean isCompleted() {
            return completed;
        }
    }
    ```
    ```java
    package com.example.demo.domain;

    import org.springframework.data.repository.CrudRepository;

    public interface TodoRepository extends CrudRepository<Todo, Long> {
    }
    ```
    ```java
    package com.example.demo.web;

    import com.example.demo.domain.Todo;
    import com.example.demo.domain.TodoRepository;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RestController;

    @RestController
    public class TodoController {

        private final TodoRepository repository;

        public TodoController(TodoRepository repository) {
            this.repository = repository;
        }

        @GetMapping("/todos/sample")
        public Todo sample() {
            Todo saved = repository.save(new Todo("스프링 부트 공부하기", false));
            return repository.findById(saved.getId()).orElseThrow();
        }
    }
    ```
    ```properties
    # src/main/resources/application.properties (PostgreSQL 설정 추가)
    spring.datasource.url=jdbc:postgresql://localhost:5432/todo_db
    spring.datasource.username=todo_user
    spring.datasource.password=todo_pass
    spring.jpa.hibernate.ddl-auto=update
    spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
    ```
- **보충: MyBatis 예시**
  - `spring-boot-starter-jdbc`, `mybatis-spring-boot-starter` 추가
  - `src/main/resources/mapper/TodoMapper.xml`과 매퍼 인터페이스 구성
  - **샘플 코드**
    ```properties
    # src/main/resources/application.properties (MyBatis 설정 추가)
    mybatis.mapper-locations=classpath:mapper/*.xml
    mybatis.type-aliases-package=com.example.demo.domain
    ```
    ```java
    package com.example.demo.domain;

    public record TodoDto(Long id, String title, boolean completed) {}
    ```
    ```java
    package com.example.demo.domain;

    import org.apache.ibatis.annotations.Mapper;
    import org.apache.ibatis.annotations.Param;

    @Mapper
    public interface TodoMapper {

        void insert(@Param("title") String title, @Param("completed") boolean completed);

        TodoDto findLatest();
    }
    ```
    ```xml
    <!-- src/main/resources/mapper/TodoMapper.xml -->
    <?xml version="1.0" encoding="UTF-8" ?>
    <!DOCTYPE mapper
            PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
            "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
    <mapper namespace="com.example.demo.domain.TodoMapper">
        <insert id="insert">
            INSERT INTO todo(title, completed) VALUES (#{title}, #{completed})
        </insert>

        <select id="findLatest" resultType="com.example.demo.domain.TodoDto">
            SELECT id, title, completed
            FROM todo
            ORDER BY id DESC
            LIMIT 1
        </select>
    </mapper>
    ```
    ```java
    package com.example.demo.web;

    import com.example.demo.domain.TodoDto;
    import com.example.demo.domain.TodoMapper;
    import org.springframework.web.bind.annotation.GetMapping;
    import org.springframework.web.bind.annotation.RestController;

    @RestController
    public class TodoMyBatisController {

        private final TodoMapper mapper;

        public TodoMyBatisController(TodoMapper mapper) {
            this.mapper = mapper;
        }

        @GetMapping("/todos/mybatis")
        public TodoDto latestTodo() {
            mapper.insert("MyBatis로 만든 할 일", false);
            return mapper.findLatest();
        }
    }
    ```
- **정리**
  - 컨트롤러, Data, Entity
  - myBatis, JPA 차이.
  

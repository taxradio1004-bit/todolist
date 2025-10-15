## CRUD / RESTful / DDL / DML 한눈에 보기

| CRUD 단계 | DDL (스키마 정의) | DML (데이터 조작) | RESTful (HTTP 메서드) |
| --- | --- | --- | --- |
| C (Create) | `CREATE TABLE todo (...)`, `ALTER TABLE ... ADD COLUMN` | `INSERT INTO todo(title, completed) VALUES (...)` | `POST /todos` |
| R (Read) | `SHOW CREATE TABLE todo`, `DESCRIBE todo` | `SELECT * FROM todo WHERE id = ?` | `GET /todos/{id}` |
| U (Update) | `ALTER TABLE todo ALTER COLUMN completed TYPE BOOLEAN` | `UPDATE todo SET completed = true WHERE id = ?` | `PUT /todos/{id}`, `PATCH /todos/{id}` |
| D (Delete) | `DROP TABLE todo`, `ALTER TABLE ... DROP COLUMN` | `DELETE FROM todo WHERE id = ?` | `DELETE /todos/{id}` |

## DB 기초 개념 정리

- `Schema`: 데이터베이스 내 객체(테이블, 뷰 등)를 논리적으로 묶는 컨테이너이자 설계도.
- `Table`: 같은 종류의 데이터를 열(Column)과 행(Row) 구조로 저장하는 실체.
- `Column`: 테이블에서 하나의 속성을 나타내는 열로, 데이터 타입과 제약조건을 가진다.
- `Row`: 각 컬럼에 대응되는 실제 값들이 모여 있는 한 줄의 레코드로, 한 개체나 사건을 표현한다.
- `Data`: 컬럼 구조에 저장된 값들의 집합으로, 비즈니스 정보를 담는다.

## RESTful에서 CRUD별 특징

| 구분 | 대표 HTTP 메서드 | URI 패턴 예시 | 주요 상태 코드 | 핵심 포인트 |
| --- | --- | --- | --- | --- |
| Create | `POST` | `/todos` | `201 Created`, `400 Bad Request` | 새 자원 생성, 응답에 Location 헤더 포함 |
| Read | `GET` | `/todos`, `/todos/{id}` | `200 OK`, `404 Not Found` | 서버 상태 변화 없음, 캐싱 가능 |
| Update | `PUT`, `PATCH` | `/todos/{id}` | `200 OK`, `204 No Content`, `409 Conflict` | 전체 vs 부분 업데이트, 멱등성 고려 |
| Delete | `DELETE` | `/todos/{id}` | `200 OK`, `204 No Content`, `404 Not Found` | 멱등적 동작, 응답 본문 선택적 |

### Create (C) - POST

- **목적**: 아직 존재하지 않는 자원을 생성. 서버가 새 ID를 할당하거나 클라이언트가 지정한 ID를 검증.
- **요청 본문**: JSON 등으로 필수 필드 포함. 유효성 검증 실패 시 `400 Bad Request`.
- **응답 규약**: 성공 시 `201 Created`와 함께 `Location` 헤더에 새 자원 URI 전달, 본문에는 생성된 리소스나 식별자 반환.
- **부가 고려사항**:
  - 같은 요청이 반복되면 중복 데이터가 생길 수 있으므로 클라이언트는 중복 방지 토큰(Idempotency-Key) 등을 활용.
  - 트랜잭션 처리 후 이벤트 발행이 필요하다면 비동기 처리나 Outbox 패턴 고려.

### Read (R) - GET

- **목적**: 자원 조회. 컬렉션(`/todos`)과 단건(`/todos/{id}`) 모두 지원.
- **캐싱 전략**: GET은 안전하고 멱등적이므로 `ETag`, `Last-Modified`, `Cache-Control`을 활용해 트래픽 절감.
- **필터링/페이지네이션**: 쿼리 파라미터(`?page=1&size=10`, `?completed=true`)로 조건 제어. 응답에 메타데이터(총 개수, 다음 페이지 링크) 포함.
- **에러 처리**: 존재하지 않는 ID 요청은 `404 Not Found`, 권한 부족 시 `403 Forbidden` 반환.
- **한계**: 대량 데이터는 스트리밍이나 Cursor 기반 페이지네이션 고려 필요.

### Update (U) - PUT / PATCH

- **PUT (전체 갱신)**:
  - 요청 본문이 리소스의 전체 상태를 대표. 빠진 필드는 기본값이나 null로 덮어쓰는 것이 원칙.
  - 동일 요청을 여러 번 보내도 상태가 변하지 않아 멱등성을 보장.
- **PATCH (부분 갱신)**:
  - 변경하고 싶은 필드만 전달. JSON Merge Patch, JSON Patch(RFC 6902) 같은 형식 사용 가능.
  - 구현 단순성 vs 표준 준수 사이에서 프로젝트 정책 결정 필요.
- **공통 고려사항**:
  - 동시 수정 충돌 방지를 위해 `If-Match` 헤더(ETag 기반)나 버전 필드 사용 후 `409 Conflict` 처리.
  - 성공 응답: 갱신된 리소스를 반환하거나 `204 No Content`로 본문 없이 처리.

### Delete (D) - DELETE

- **목적**: 자원 제거. 같은 요청을 반복해도 결과가 동일해야 하므로 멱등성을 지켜야 함.
- **응답 규약**: 성공 시 `204 No Content`가 일반적이나, 삭제한 자원을 반환하거나 삭제 여부 플래그를 내려도 됨.
- **설계 선택**:
  - 실제 삭제 대신 소프트 삭제(논리 삭제) 도입 시 `is_deleted` 필드 업데이트로 구현. 복원 API 제공 여부 결정.
  - 자식 리소스나 연관 데이터가 있을 경우 409 또는 422 등으로 제약 조건 위반을 알리고, 사전 확인용 `/todos/{id}/relations` 같은 엔드포인트 고려.
- **보안/감사**: 중요 데이터 삭제 시 권한 확인, 감사 로그 기록, 삭제 전 확인 절차(Confirm) 필요.

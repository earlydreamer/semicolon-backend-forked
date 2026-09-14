# Spring AI 2.0.1 업그레이드

Spring AI BOM을 `2.0.0-M1`에서 `2.0.1`로 올렸다. Spring Boot `4.0.1`과 Java 25는 유지한다.

## 기존 DB 배포 순서

기존 `SPRING_AI_CHAT_MEMORY`에는 새 필수 컬럼 `sequence_id`가 없다.
`initialize-schema: always`는 기존 테이블에 컬럼을 추가하지 않으므로,
새 애플리케이션을 기동하기 전에 다음 작업이 필요하다.

1. 기존 AI 인스턴스를 중지하고 DB를 백업한다.
2. AI 서비스의 PostgreSQL DB에 `src/main/resources/sql/manual/20260912_spring_ai_chat_memory_sequence.sql`을 실행한다.
3. Spring AI 2.0.1 애플리케이션을 기동한다.
4. 이전 대화 조회와 도구를 사용하는 채팅 응답을 확인한다.

스크립트는 기존 대화와 timestamp를 보존하고 대화별 순서를 채운다.
같은 timestamp를 가진 과거 메시지의 원래 순서는 복원할 수 없다.
재실행할 수 있으며, 테이블이 없는 신규 DB에서는 아무 작업도 하지 않는다.
신규 테이블은 Spring AI의 자동 초기화가 생성한다.
자동 마이그레이션 도구에 등록된 파일이 아니므로 기존 DB에는 직접 실행해야 한다.

`sequence_id`에는 PostgreSQL 시퀀스 기본값을 함께 설정한다.
현재 배포된 `2.0.0-M1`은 이 컬럼 없이 INSERT하므로 기본값으로 순번을 할당한다.
`2.0.1`은 대화별 순번을 직접 지정하므로 기본값을 사용하지 않는다.
따라서 DB를 먼저 올린 뒤 기존 앱을 재기동하거나 앱 버전을 롤백해도 저장할 수 있다.
기본값은 새 버전 전환 후에도 유지할 수 있으며, 두 버전의 동시 쓰기 충돌을 방지하려면
앱 교체 시 기존 인스턴스 종료를 확인한다.

## 호환성 변경과 검증

- 모델 설정의 deprecated `options` 중첩을 제거했다. 모델명, temperature, 임베딩 차원은 유지한다.
- `ToolCallingAdvisor`를 order 300으로 등록했다. 입력 검증, 메모리, 검색은 도구 반복 실행 바깥에서 처리되어 JDBC 메모리에 최종 대화만 저장된다.
- `ChatClientConfigTest`는 모델 응답을 모의 처리하고 임시 H2 DB의 실제 JDBC 메모리 저장소로 동기/스트리밍 도구 실행, 이전 대화 유지, 최종 대화 저장, 입력 차단을 검증한다. 외부 API와 DB 서버는 필요 없다.

```powershell
.\gradlew.bat :ai:test --tests dukku.ai.global.config.ChatClientConfigTest
.\gradlew.bat :ai:build
```

명령은 저장소 루트에서 실행한다. 전체 `:ai:build`의 기존 `ApplicationTests`는
테스트 프로필에 지정된 PostgreSQL 등 외부 서비스가 준비되어 있어야 한다.
단위 테스트는 실제 PostgreSQL 마이그레이션이나 OpenAI 응답을 검증하지 않는다.

참고: [공식 업그레이드 가이드](https://docs.spring.io/spring-ai/reference/upgrade-notes.html),
[Spring Boot 호환 범위](https://docs.spring.io/spring-ai/reference/getting-started.html).

## 2026-09-15 원격 DB 적용 기록

- 대상: SSH `macbookair`, Docker의 `k3d-semicolon-local-server-0`, namespace `semicolon`, PostgreSQL `ai_service`.
- 맥북의 DB 백업: `/Users/earlydreamer/semicolon-migration-20260915/ai_service-before.dump` (권한 600, `pg_restore --list` 검증).
- AI 인스턴스 1 → 0으로 중지한 뒤 SQL 적용, 성공 후 1로 복구했다.
- `sequence_id BIGINT NOT NULL`, 구버전용 시퀀스 기본값, 복합 인덱스 생성 확인. 대화 메모리 행 수는 0 → 0.
- 실제 PostgreSQL에서 구버전의 컬럼 생략 INSERT와 새 버전의 명시적 순번 INSERT가 통과했다. 검증 행은 ROLLBACK했다.
- 별도 임시 스키마에서 과거 메시지 순번 채우기와 SQL 2회 실행을 검증한 뒤 ROLLBACK했다.
- AI Pod Ready 및 서비스 프록시 `/actuator/health`의 `status: UP`을 확인했다.
- 현재 원격 앱 JAR은 여전히 Spring AI `2.0.0-M1`이다. 이번 작업은 DB 선행 마이그레이션이며 앱 이미지는 교체하지 않았다.
- 기존 `showcase-db-reset` CronJob은 매일 한국 시간 00:00에 `ai_service`를 포함한 DB를 DROP/재생성한다. 앱이 M1인 동안 리셋되면 이전 스키마가 다시 생성되므로, 새 앱 배포 직전에 마이그레이션을 재확인하고 필요하면 재실행해야 한다. CronJob 설정은 변경하지 않았다.
- 최초 점검에서 저장소에 남은 옛 주소 `api.dukku.shop`을 사용했다. 해당 주소의 시간 초과는 현재 서비스 장애의 근거가 아니다.
- 맥북의 `cloudflared`가 2026-09-15 01:15:16 KST에 Cloudflare에서 받아 적용한 설정을 확인했다. 현재 API 주소는 `https://api-dukku.earlydreamer.dev`, origin은 `http://localhost:8080`이다.
- 실제 Ingress의 `/api/v1/ai` → `ai-service:80` 경로를 확인했다. 공개 주소의 `/api/v1/ai/chat`에 인증 없이 GET 요청하여 Cloudflare를 거친 애플리케이션의 JSON `401 Unauthorized` 응답을 확인했다. 인증된 채팅 생성 및 모델 호출까지 검증한 것은 아니다.

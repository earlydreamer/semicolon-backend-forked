# AI 서비스

## 현재 코드 기준 (2026-09-19)

AI 모듈은 Java 25, Spring Boot 4.0.1, Spring AI 2.0.1을 유지하면서 Gemini Developer API의 native 경로를 사용한다. 채팅은 `spring-ai-starter-model-google-genai`의 `GoogleGenAiChatModel`과 기존 `GEMINI_API_KEY`를 사용하고, 기본 모델은 `gemini-3.5-flash-lite`다. `CHAT_MODEL` 환경변수로 모델을 바꿀 수 있다.

임베딩은 `GeminiEmbeddingService`가 공식 Google GenAI SDK의 `Client.models.embedContent`를 직접 호출한다. Spring AI 2.0.1의 Google 임베딩 구현은 요청의 `taskType`을 전송하지 않아 이 경계에서는 `RETRIEVAL_QUERY`와 `RETRIEVAL_DOCUMENT`, `outputDimensionality=1536`을 명시한다. 검색 질의에는 query task type을, 기억과 상품의 저장·백필에는 document task type을 사용한다. 벡터는 1536차원·유한값·0이 아닌 norm을 확인하고 L2 정규화한 뒤 저장한다.

대화 응답 경계는 thought 텍스트를 사용자 응답·장기 기억·JDBC 채팅 메모리에 노출하지 않는다. 함수 호출 메시지의 opaque `thoughtSignatures`는 해당 tool turn에서 보존해 후속 native 요청에 돌려보낸다. 도구 반복은 `ToolCallingAdvisor` order 300에서 실행되고, JDBC 대화 메모리는 최종 user/assistant 교환만 저장한다.

채팅 HTTP 응답은 기존 문자열 SSE를 유지한다. Spring MVC의 `data:` 직렬화 뒤 표준 SSE 파서가 구분 공백 하나를 제거하는 규칙에 맞춰 controller에서 각 데이터 줄에 구분 공백을 명시한다. 따라서 chunk 시작 공백, 들여쓰기와 LF가 전송 중 사라지지 않으며 모델 출력과 JDBC 저장 문자열은 그대로 유지된다. 실제 MVC 응답을 표준 방식으로 파싱하는 회귀 테스트에 한글·공백만 있는 chunk·연속/마지막 LF를 포함한다.

`ai_user_memory`와 `product_search`에는 nullable `embedding_profile`이 있다. 현재 프로필은 `{model}:1536:retrieval-document:normalization-v1` 형식이다. 검색과 중복 비교는 현재 프로필이 붙은 벡터만 사용한다. 알 수 없는 기존 프로필은 `NULL`로 남겨 두며, 새 프로필로 표시하기 전에 실제 콘텐츠를 재임베딩해야 한다. `pgvector`가 설치되어 있고 `PGroonga`가 없는 DB에서는 벡터 전용 상품 검색을 사용한다.

## 설정

- `GEMINI_API_KEY`: 기존 Gemini 키. release에서 필수이며 애플리케이션 프로세스 환경으로만 전달한다.
- `CHAT_MODEL`: 기본값 `gemini-3.5-flash-lite`.
- `EMBEDDING_MODEL`: 기본값 `gemini-embedding-001`.
- `ai.embedding.dimensions`: DB의 `vector(1536)`과 맞춰 1536으로 유지한다. 다른 값은 애플리케이션 시작 시 거부된다.
- `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`, `DB_SSLMODE`: AI PostgreSQL 연결 설정.

Google SDK 요청 제한 시간은 60초다. 일반 `RestClient.Builder`와 `WebClient.Builder`의 기존 timeout 설정은 `AiHttpClientConfig`에서 유지한다. Gemini 경로에는 Vertex AI 프로젝트/인증을 사용하지 않는다.

## 기존 벡터를 보존하는 프로필 마이그레이션

### JDBC 채팅 메모리 선행 조건

기존 `SPRING_AI_CHAT_MEMORY` 테이블에 `sequence_id`가 없다면 새 앱을 기동하기 전에 백업하고 `src/main/resources/sql/manual/20260912_spring_ai_chat_memory_sequence.sql`을 실행해야 한다. 이 SQL은 Spring AI 2.0.1 JDBC 메모리 저장소가 요구하는 컬럼을 추가하는 기존 선행 마이그레이션이며, 대상 DB에 이미 `sequence_id`가 있는지는 각 환경에서 확인한다.

2026-09-19에 확인한 원격 기준선에는 `ai_user_memory` 7행이 있었고, 모두 `content`는 보존됐지만 `embedding`은 `NULL`이었다. 당시 `embedding_profile` 컬럼은 아직 없었고 `product_search`는 0행이었다. 이 기준선은 당시의 읽기 결과이며, 실제 변경·백필 뒤 상태를 뜻하지 않는다.

프로필 컬럼 추가 SQL은 `src/main/resources/sql/manual/20260919_gemini_embedding_profile.sql`이다. 앱 초기화도 두 테이블에 컬럼이 없으면 추가한다. 벡터 차원이 다르면 기존 행을 비우거나 컬럼 타입을 바꾸지 않고 오류를 내므로, 사전 검토한 별도 마이그레이션이 필요하다.

백필 CLI는 서버, Kafka listener, 샘플 초기화를 시작하지 않는다. `--dry-run`은 기본값이며 DB의 대상 건수만 확인하고 Gemini 키를 읽지 않는다. 실제 native 임베딩 요청은 `--apply`를 명시할 때만 한다. 옵션으로 `--batch-size`와 `--max-rows`를 제한할 수 있다.

패키징 앱의 기본 진입점은 `dukku.ai.AiApplication`로 유지한다. CLI는 `PropertiesLauncher`로 호출한다.

```powershell
$jar = 'ai/build/libs/ai-0.0.1-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw "bootJar 파일을 찾을 수 없어: $jar" }
java "-Dloader.main=dukku.ai.app.migration.GeminiEmbeddingMigrationCli" -cp $jar org.springframework.boot.loader.launch.PropertiesLauncher --dry-run
java "-Dloader.main=dukku.ai.app.migration.GeminiEmbeddingMigrationCli" -cp $jar org.springframework.boot.loader.launch.PropertiesLauncher --apply --batch-size 100 --max-rows 1000
```

운영 순서는 백업 확인 → AI writer 중지와 reset 일정 확인 → additive SQL → `--dry-run` 대상 확인 → 제한된 `--apply` → 행 수·content 보존·차원·프로필·오류 건수 확인 → 새 앱 검증 → writer 복구다. 실패한 행의 `content`, 기존 벡터, 프로필은 성공한 재생성이 끝날 때까지 유지해야 한다. 키 값이나 사용자 기억 원문을 채팅·명령 인자·로그에 넣지 않는다.

## `showcase-db-reset` 자동 샘플 메모리 임베딩

reset은 AI DB를 재생성해 `ai_user_memory`에 임베딩과 프로필이 없는 샘플 메모리 7건을 넣는다. 모든 서비스 rollout이 끝난 뒤 reset 스크립트가 현재 AI Deployment와 같은 이미지 reference를 쓰는 임시 Kubernetes Job을 실행해 위 `GeminiEmbeddingMigrationCli`를 `--apply --batch-size 7 --max-rows 7`로 호출한다. CLI의 테이블 처리 순서에서 `ai_user_memory`가 먼저이므로 이 제한은 reset 시드 메모리 7건에 적용되고 `product_search`는 처리하지 않는다.

임시 Job은 기존 `semicolon-env`와 `ai-db` Secret을 환경으로 읽으며 AI 애플리케이션 Pod 안에 두 번째 JVM을 띄우지 않는다. Job은 service account token 자동 마운트를 끄고, CPU 500m·메모리 512Mi·실행 540초 상한을 사용한다. 키와 DB 인증정보는 Job args와 resetter 로그에 나오지 않는다. 종료 메시지와 resetter 로그에는 `ai_user_memory` 고정 aggregate 및 CLI의 허용된 고정 실패 문구나 단순 예외 타입만 남긴다.

CLI 종료 코드가 0이고 요약이 `present=true`, `scanned=7`, `candidates=7`, `updated=7`, `conflicts=0`, `failures=0`을 모두 확인한 뒤에만 reset 완료를 기록한다. 성공 Job 삭제는 최선 노력으로 시도하며 삭제가 실패해도 경고만 남기고 reset 성공 여부에는 영향을 주지 않는다. Gemini API, Secret, DB, 스키마, 이미지 실행 또는 요약 검증이 실패하면 reset도 실패로 끝난다. 종료 메시지는 테이블 aggregate, CLI의 고정된 API 키/설정/스키마/인자 오류 문구, 또는 `Migration stopped before completion (<영숫자 예외 타입>).`만 허용한다. 임의 stdout/stderr와 메모리 본문은 resetter 로그로 보내지 않는다. 실패 Job과 삭제되지 않은 성공 Job은 TTL에 따라 최대 7일 뒤 정리된다. 실패 시 기존 EXIT trap이 앱과 product 초기화 flag 복구를 시도한다. 부모 `showcase-db-reset` CronJob의 기존 backoffLimit과 전체 reset 정책은 그대로 유지된다.

## 과거 기록과 현재 상태 구분

2026-09-15의 DB 변경 기록은 `sequence_id` 선행 마이그레이션 당시의 사실이다. 그 기록에서 “원격 앱은 Spring AI 2.0.0-M1”이라고 적힌 부분은 그 날짜의 실행 이미지에만 해당하며, 현재 코드의 Spring AI 2.0.1 상태나 이후 배포 상태를 뜻하지 않는다. 모델 설정이 2.0.1로 변경된 사실만으로 원격 앱이 배포됐다고 판정하지 말고, 이미지 digest와 실행 버전을 별도로 확인한다.

이전 문서의 “NULL 또는 DELETE 처리” 권고는 더 이상 전환 절차로 사용하지 않는다. NULL은 검색에서 제외될 뿐이고 DELETE는 기억 content를 지운다. 현재 절차는 기존 content를 보존하고 명시적 backfill CLI로 벡터와 profile을 갱신하는 방식이다.

## 검증

로컬 검증은 Gemini native HTTP fake를 사용해 실제 SDK의 채팅·스트리밍·함수 호출·signature 재전송과 임베딩 task type·차원을 검사한다. JDBC 채팅 메모리 테스트는 H2를 사용한다. 실제 PostgreSQL/pgvector 및 PGroonga 미설치 검색 경로의 결과와 원격 backfill·앱 배포는 별도 연동 검증으로 기록한다.

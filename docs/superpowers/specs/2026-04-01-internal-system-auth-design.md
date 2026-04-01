# Internal/System Auth Boundary Design

## Goal

내부 서비스 호출과 외부 HTTP 요청의 인증/인가 경계를 분리한다. 관리자 수동 트리거는 작업 시작의 주체만 남기고, 실제 downstream 실행은 system identity로 통일한다.

## Problem

- `RequestAuthorizationHeaderResolver`가 현재 요청의 `Authorization` 헤더와 `INTERNAL_SERVICE_TOKEN` fallback을 한 경로에서 처리한다.
- 같은 배치가 scheduler에서 돌면 fallback token, admin 수동 실행에서 돌면 admin JWT를 탈 수 있어 실행 의미가 바뀐다.
- `/api/v1/internal/**`와 `/api/v1/products/internal/**`, `/api/v1/carts/internal/**` 같은 내부 진입점이 사용자 JWT 체계에 기대고 있다.
- settlement 배치가 deposit의 admin endpoint를 호출하는 등 system-to-system 호출이 admin surface에 기대는 경로가 남아 있다.

## Decision

### 1. Identity split

- `public/user`: 사용자 JWT
- `admin/manual`: 관리자 JWT
- `system/internal`: 내부 전용 shared token -> `ROLE_SYSTEM`

`admin`은 trigger only이며, downstream 내부 호출은 사용자/admin JWT를 전파하지 않는다.

### 2. Internal auth transport

내부 전용 토큰은 `Authorization`가 아니라 별도 헤더로 전달한다.

- Header: `X-Internal-Service-Token`
- Secret source: `INTERNAL_SERVICE_TOKEN`
- Server behavior: 내부 경로에서 header 값이 secret과 일치하면 `ROLE_SYSTEM` 인증 객체를 세팅

### 3. Internal path scope

이번 변경에서 system-only 경로로 간주하는 엔드포인트는 아래다.

- `/api/v1/internal/**`
- `/api/v1/products/internal/**`
- `/api/v1/carts/internal/**`

예외:

- `/api/v1/internal/users/verify-password`
- `/api/v1/internal/users/social`

위 두 경로는 auth 로그인/소셜 가입 흐름 때문에 기존처럼 permit-all로 유지한다.

### 4. Client behavior

- internal endpoint를 호출하는 client는 항상 `X-Internal-Service-Token`을 사용한다.
- public/admin endpoint를 호출하는 client만 현재 요청의 `Authorization` 헤더를 사용한다.
- `RequestAuthorizationHeaderResolver`는 더 이상 env fallback에 의존하지 않는다.

### 5. Admin endpoint cleanup

system이 admin endpoint를 호출하는 경로는 제거한다.

이번 패치에서는 settlement가 사용하던 deposit account lookup을 internal endpoint로 옮긴다.

## Implementation Scope

### Included

- internal shared-token filter 추가
- internal path matcher 및 release security rule 조정
- internal client header 분리
- deposit internal account lookup endpoint 추가 및 client 전환
- 관련 unit/integration test 추가

### Excluded

- admin endpoint 전면 재설계
- internal path naming 전면 통일 (`/api/v1/internal/**`로 모두 이관)
- observability/audit schema 확장

## Testing

- common: internal token resolver/filter unit test
- common: request authorization resolver regression test
- deposit: internal account lookup regression test
- targeted module tests for changed clients / security behavior

## Risks

- 일부 internal-like 경로가 아직 `/api/v1/internal/**` 밖에 있어 누락될 수 있다.
- system이 admin endpoint를 호출하는 다른 경로가 남아 있으면 이번 패치 후 null auth로 드러날 수 있다.
- release security rule 순서가 잘못되면 `verify-password` / `social` 경로가 막힐 수 있다.

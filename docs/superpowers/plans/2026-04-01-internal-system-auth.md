# Internal/System Auth Boundary Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** internal 요청과 외부 요청의 인증 경계를 분리하고, 관리자 수동 트리거 이후 실행을 system identity로 통일한다.

**Architecture:** 현재 요청 헤더 전파와 env fallback을 분리한다. 내부 경로는 shared-token 기반 `ROLE_SYSTEM` 인증으로 처리하고, public/admin 경로는 기존 JWT 체계를 유지한다. settlement -> deposit 경로처럼 system이 admin endpoint를 호출하던 곳은 internal endpoint로 교체한다.

**Tech Stack:** Spring Boot 4, Spring Security, RestClient, JUnit 5, Spring Boot Test

---

### Task 1: Common Internal Auth Foundation

**Files:**
- Create: `common/src/main/java/dukku/common/global/auth/InternalServiceTokenResolver.java`
- Create: `common/src/main/java/dukku/common/global/auth/internal/InternalServiceAuthenticationFilter.java`
- Modify: `common/src/main/java/dukku/common/global/auth/RequestAuthorizationHeaderResolver.java`
- Modify: `common/src/main/java/dukku/common/global/auth/jwt/JwtAuthenticationFilter.java`
- Modify: `common/src/main/java/dukku/common/global/security/SecurityWhitelist.java`
- Test: `common/src/test/java/dukku/common/global/auth/InternalServiceTokenResolverTest.java`
- Test: `common/src/test/java/dukku/common/global/auth/internal/InternalServiceAuthenticationFilterTest.java`
- Test: `common/src/test/java/dukku/common/global/auth/RequestAuthorizationHeaderResolverTest.java`

- [ ] Add failing tests for internal token resolution and system authentication filter behavior.
- [ ] Make `RequestAuthorizationHeaderResolver` request-only and remove env fallback semantics.
- [ ] Add shared-token internal auth filter that authenticates `ROLE_SYSTEM` using `X-Internal-Service-Token`.
- [ ] Prevent JWT filter from overwriting an already-authenticated system context.
- [ ] Run `./gradlew :common:test --tests '*InternalService*' --tests '*RequestAuthorizationHeaderResolverTest'`.

### Task 2: Release Security Rules

**Files:**
- Modify: `ai/src/main/java/dukku/ai/global/config/SecurityReleaseConfig.java`
- Modify: `auth/src/main/java/dukku/auth/global/config/SecurityReleaseConfig.java`
- Modify: `coupon/src/main/java/dukku/coupon/global/config/SecurityReleaseConfig.java`
- Modify: `deposit/src/main/java/dukku/deposit/global/config/SecurityReleaseConfig.java`
- Modify: `order/src/main/java/dukku/order/global/config/SecurityReleaseConfig.java`
- Modify: `payment/src/main/java/dukku/payment/global/config/SecurityReleaseConfig.java`
- Modify: `product/src/main/java/dukku/product/global/config/SecurityReleaseConfig.java`
- Modify: `settlement/src/main/java/dukku/settlement/global/config/SecurityReleaseConfig.java`
- Modify: `user/src/main/java/dukku/user/global/config/SecurityReleaseConfig.java`

- [ ] Inject the internal auth filter into release security configs.
- [ ] Require `ROLE_SYSTEM` for internal/system-only paths and keep admin/user rules unchanged.
- [ ] Preserve user-service login/social exceptions before the system matcher.
- [ ] Run targeted security-related tests or module test suites impacted by config compilation.

### Task 3: Internal Client Split

**Files:**
- Modify: `common/src/main/java/dukku/common/shared/coupon/out/CouponApiClient.java`
- Modify: `common/src/main/java/dukku/common/shared/order/out/OrderApiClient.java`
- Modify: `common/src/main/java/dukku/common/shared/payment/out/PaymentApiClient.java`
- Modify: `common/src/main/java/dukku/common/shared/user/out/UserApiClient.java`
- Modify: `common/src/main/java/dukku/common/shared/product/out/ProductApiClient.java`
- Modify: `common/src/main/java/dukku/common/shared/product/out/CartApiClient.java`
- Modify: `common/src/main/java/dukku/common/shared/deposit/out/depositApiClient/DepositApiClient.java`

- [ ] Switch internal endpoint calls to `X-Internal-Service-Token`.
- [ ] Keep public/admin endpoint calls on request `Authorization` propagation.
- [ ] Re-check any mixed clients so one class can safely support both admin/public and internal calls.
- [ ] Run compilation for affected modules.

### Task 4: Deposit Internal Endpoint Cleanup

**Files:**
- Modify: `deposit/src/main/java/dukku/deposit/boundedContext/deposit/in/DepositInternalController.java`
- Test: `deposit/src/test/java/dukku/deposit/boundedContext/deposit/in/DepositInternalControllerE2ETest.java`

- [ ] Add internal deposit account lookup endpoint for system callers.
- [ ] Move settlement-facing deposit UUID lookup off the admin surface.
- [ ] Add regression test covering the new internal account lookup.
- [ ] Run `./gradlew :deposit:test --tests '*DepositInternalControllerE2ETest'`.

### Task 5: Verification and Delivery

**Files:**
- Modify: `docs/superpowers/specs/2026-04-01-internal-system-auth-design.md`
- Modify: `docs/superpowers/plans/2026-04-01-internal-system-auth.md`

- [ ] Run targeted module tests for `common`, `deposit`, `settlement`, `order`, `payment`, `product`, `user`.
- [ ] Review diff to confirm system/admin/public boundaries match the design.
- [ ] Commit with repository-style title + bullet body.
- [ ] Push branch and open/update draft PR with validation notes and the issue-disabled note.

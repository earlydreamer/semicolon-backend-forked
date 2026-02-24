# Redis 사용 목적 및 영향 분석

이 문서는 저장소에서 Redis가 어떤 목적으로 사용되는지, 관련 파일들, 데이터 유실 시 영향 등을 정리한 문서입니다.

## 요약
- Redis는 주로 캐시, 카운터, 일시적 토큰(예: 이메일 인증), 레이트 리미팅 용도로 사용됩니다.
- Spring Data Redis(`RedisTemplate`)를 사용하며, 기본 클라이언트는 Lettuce입니다.
- Redis Streams, Pub/Sub, Spring Session 등의 고급 패턴은 코드상에서 사용되지 않습니다.

## 1) 의존성 / 설정
- 의존성: `common/build.gradle`에 `org.springframework.boot:spring-boot-starter-data-redis` 포함.
- 클라이언트: 별도의 Jedis 의존성 없음 → Spring Boot 기본인 Lettuce 사용으로 간주.
- 환경 변수(설정 키): `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` (각 서비스의 `application.yml`에서 사용)
  - 예: `product/src/main/resources/application.yml`에서 `spring.data.redis.host: ${REDIS_HOST:localhost}` 등.

## 2) 코드 상 사용처(파일 목록)
- 공통(레이트리밋)
  - `common/src/main/java/dukku/common/global/ratelimit/RateLimitInterceptor.java` (레이트 제한 인터셉터)
  - `common/src/main/java/dukku/common/global/ratelimit/RedisFixedWindowRateLimiter.java` (fixed window 구현)
- 유저
  - `user/src/main/java/dukku/user/global/config/RedisConfig.java` (RedisTemplate Bean)
  - `user/src/main/java/dukku/user/boundedContext/user/app/email/EmailVerificationService.java` (이메일 검증 토큰 저장)
- 상품
  - `product/src/main/java/dukku/product/global/config/RedisConfig.java` (RedisTemplate Bean)
  - `product/src/main/java/dukku/product/boundedContext/product/app/cqrs/ProductStatsRedisSupport.java` (상품 통계 카운터 및 dirty-set)
  - `product/src/main/java/dukku/product/boundedContext/product/app/cqrs/review/ReviewStatsRedisSupport.java` (유사 용도)

> 참고: RedisTemplate 사용이 발견된 파일들을 중심으로 정리했습니다.

## 3) 사용 패턴 요약
- RedisTemplate을 통해 다음 연산을 사용합니다:
  - `opsForValue().increment / decrement` (카운터)
  - `opsForSet().add / members / remove` (dirty set 관리)
  - `opsForValue().set/get` (토큰 저장)
  - `multiGet(keys)` (여러 키 한 번에 조회)
- `@Cacheable` 계열의 Spring Cache 애노테이션, Spring Session, Redis Streams, Pub/Sub 기능은 코드에서 확인되지 않았습니다.

## 4) 지속성 필요성 판단
- Rate limiting: 임시 데이터이고 유실 허용(코드에서 Redis 오류 시 Fail-open 처리).
- Email verification token: 단기 토큰으로 유실 시 UX에 영향(재전송으로 보완 가능).
- Product stats: Redis에 임시 집계 후 DB에 동기화되는 구조(Dirty set 사용). 유실 시 일부 통계 손실 가능.

=> 결론: 대부분 soft-state(유실 허용) 용도로 사용되나, 일부 통계는 정확성 영향 가능.

## 5) Redis 데이터 유실 시 영향도 요약
- 유실 허용(서비스 동작에 중대한 영향 없음): rate-limit counters, 이메일 토큰
- 유실 가능하지만 영향 있음(정확성 저하): 상품 통계(조회수/좋아요/댓글 집계)
- 유실 불가(치명적): 코드상 Redis를 통한 영구 저장(예: 세션, 영속 큐) 사용 사례 없음

## 6) 권장 조치
- 운영 환경에서는 Redis HA(복제/클러스터) 및 적절한 persistence(AOF/RDB) 설정 권장
- 상품 통계 동기화 로직의 재시도/백오프와 모니터링 강화 권장
- Redis 모니터링 및 백업 전략 수립 권장

---

문서 생성일: 2026-02-22

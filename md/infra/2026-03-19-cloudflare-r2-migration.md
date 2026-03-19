# 2026-03-19 AWS S3 → Cloudflare R2 마이그레이션

## 작업 목표

- AWS 부트캠프 계정 만료로 S3 접근이 불가능해짐에 따라 오브젝트 스토리지를 교체한다.
- AWS 종속 구현을 걷어내고 S3 호환 솔루션을 교체할 수 있는 추상화 구조로 전환한다.
- 운영 환경은 Cloudflare R2로 전환하고, 로컬 개발 환경은 SeaweedFS를 대안으로 검토한다.
- showcase reset 시 R2 버킷의 이미지도 함께 초기화되도록 연동한다.
- 컨테이너 JVM timezone을 UTC에서 KST로 통일한다.

---

## 변경 내역

### feat(product): S3 의존성을 스토리지 중립적으로 추상화

- `S3Config`에서 `cloud.aws.*` 프로퍼티 참조를 `storage.object.*`로 교체
- `S3ClientBuilder`를 직접 사용하는 방식으로 전환 (Spring Cloud AWS 의존 제거)
- `path-style-access-enabled`, `endpoint` override 지원 추가 (R2, SeaweedFS 호환)
- `UploadImageUseCase`, `GeneratePresignedUrlUseCase`가 `S3Client` bean을 그대로 활용

### feat(product): Presigned URL 응답에 오브젝트 key와 publicUrl 추가

- `PresignedUpload` 레코드에 `key`, `publicUrl` 필드 추가
- `GeneratePresignedUrlUseCase`에서 presigned URL과 함께 key, publicUrl 반환
- `PresignedUrlResponse` DTO 및 `ProductImageController` 응답 반영

### feat(local): docker-compose 로컬 환경에 SeaweedFS 추가

- `docker-compose.local.dbms.yml`에 SeaweedFS 컨테이너 추가
- `OBJECT_STORAGE_ACCESS_KEY`, `OBJECT_STORAGE_SECRET_KEY` 환경변수로 S3 자격증명 주입
- 기본값: `weedadmin` / `weedsecret`, 포트: `18333`

### feat(k8s): SeaweedFS 쿠버네티스 매니페스트 추가

- `k8s/semicolon/dependencies/seaweedfs.yml` 추가 (온프레미스 대안용)
- StatefulSet + S3 호환 API 구성

### feat(infra): 운영 환경에 Cloudflare R2 오브젝트 스토리지 설정 추가

- `application-release.yml`에 `storage.object.*` 프로퍼티 블록 추가
- `OBJECT_STORAGE_ACCESS_KEY`, `OBJECT_STORAGE_SECRET_KEY`, `OBJECT_STORAGE_REGION`,
  `OBJECT_STORAGE_BUCKET`, `OBJECT_STORAGE_ENDPOINT`, `OBJECT_STORAGE_PRESIGNED_URL_ENDPOINT`,
  `OBJECT_STORAGE_PATH_STYLE_ACCESS_ENABLED` 환경변수 체계 수립

### feat(ci): 배포 워크플로우를 OBJECT_STORAGE_* 시크릿으로 전환

- `deploy-m1-tunnel.yml` Render step에 `OBJECT_STORAGE_*` 6개 항목 추가
- `semicolon.env` 렌더링 시 해당 값 포함
- 기존 `AWS_REGION`, `AWS_S3_BUCKET` 전역 env 제거

### fix(product): S3Client 빌더 타입 오류 수정

- AWS SDK v2에서 `S3Client`는 inner `Builder` 클래스가 없고 `S3ClientBuilder`가 별도 타입
- `S3Client.Builder` → `S3ClientBuilder` 타입 변경, import 추가

### fix(product): AWS 자격증명 fallback 제거

- `application-release.yml`의 `${OBJECT_STORAGE_ACCESS_KEY:${AWS_ACCESS_KEY}}` 형태 제거
- Spring Boot 4.x에서 중첩 placeholder 처리 시 외부 값이 있어도 내부 default를 파싱하여
  `AWS_ACCESS_KEY` 미존재 에러가 발생하는 문제 수정
- `OBJECT_STORAGE_*` 4개 항목을 필수값으로 명시

### feat(product): showcase reset 시 R2 오브젝트 스토리지 이미지 초기화 추가

- `ObjectStorageClearRunner` 추가 (`@Order(1)`, `product.init.clear-es-on-startup=true` 조건)
  - `products/` prefix 오브젝트를 페이지네이션하며 전량 삭제
- `showcase-reset.yml.tpl`의 `run-showcase-reset.sh`에서:
  - scale up 직전 `kubectl set env deployment/product PRODUCT_INIT_CLEAR_ES_ON_STARTUP=true`
  - rollout 완료 후 `kubectl set env deployment/product PRODUCT_INIT_CLEAR_ES_ON_STARTUP-` 원복
  - showcase reset 시에만 R2 이미지가 초기화되도록 제어

### fix(ci): 컨테이너 JVM timezone UTC → Asia/Seoul 설정

- `LocalDateTime` 사용으로 JVM timezone이 UTC일 경우 시간이 9시간 어긋나는 문제 수정
- `semicolon.env` 렌더링 시 `TZ=Asia/Seoul` 추가
- `application-common.yml`의 `jackson.time-zone: Asia/Seoul`,
  `hibernate.jdbc.time_zone: Asia/Seoul`과 함께 전 계층 KST 통일

---

## 트러블슈팅

### S3Client.Builder 컴파일 에러

AWS SDK v2에서 `S3Client`는 inner `Builder` 클래스가 없고 `S3ClientBuilder`가 별도 타입으로
존재한다. 기존 코드가 `S3Client.Builder`로 타입을 선언하여 빌드가 실패했고,
`S3ClientBuilder`로 교체 후 해결됐다.

### OBJECT_STORAGE_ACCESS_KEY 주입 후에도 InvalidAccessKeyId 발생

배포 워크플로우 변경 후 빌드가 실패하여 deploy job이 skip됐다. 이로 인해 `semicolon-env`
시크릿이 갱신되지 않아 pod에 `OBJECT_STORAGE_*` 환경변수가 없었다.
빌드 수정 후 재배포하여 시크릿이 갱신됐지만 pod가 CrashLoopBackOff에 빠졌다.

### Spring Boot 4.x 중첩 placeholder로 인한 기동 실패

`application-release.yml`에 `${OBJECT_STORAGE_ACCESS_KEY:${AWS_ACCESS_KEY}}` 형태의
fallback이 남아있었다. `OBJECT_STORAGE_ACCESS_KEY`가 시크릿에 있어도 에러가 발생했으며,
`cloud.aws.credentials.access-key` 체인을 통해 `AWS_ACCESS_KEY`를 resolve하려다 실패했다.
fallback을 제거하고 `OBJECT_STORAGE_*`를 필수값으로 명시하여 해결됐다.

---

## 선택 배경: Cloudflare R2

포트폴리오용 쇼케이스 서비스 특성상 사용량이 많지 않아 다음 기준으로 선택했다.

| 항목 | Cloudflare R2 |
|------|--------------|
| 스토리지 | 월 10GB 무료 |
| Class A 요청 | 100만 건/월 무료 |
| Class B 요청 | 1000만 건/월 무료 |
| egress | 무료 |

온프레미스 대안(MinIO, SeaweedFS, Garage)도 검토했으나, 세팅 편의성과 무료 egress를 고려해
R2를 우선 채택했다. 규모가 커지면 온프레미스 전환을 고려한다.

# 변경 이력

## 2026-04-03

### 배포 파이프라인 이미지 보존 규칙 보강
- `scripts/apply.sh`가 서비스 Deployment를 정적 `:latest`로 다시 apply 하지 않도록 수정했습니다.
- 변경 모듈은 `${GITHUB_SHA}` 기반 이미지로 렌더링하고, 미변경 모듈은 현재 클러스터에서 실행 중인 image reference를 유지하도록 정리했습니다.
- `scripts/remote-deploy.sh`가 `apply.sh` 실행 전에 변경 모듈별 `<MODULE>_IMAGE` 환경변수를 주입하도록 수정했습니다.
- `workflow_dispatch`에 `rebuild_all_images` 입력을 추가해, 원격 Deployment가 이미 구버전 `:latest`로 drift된 경우 전체 앱을 현재 커밋 SHA 이미지로 다시 고정할 수 있게 했습니다.
- 원격 배포에서는 앱 image reference가 `:latest`로 해석되면 실패하도록 가드를 추가했습니다. 로컬 `restore.sh` bootstrap 경로는 기존처럼 `:latest` 기반 로컬 이미지를 허용합니다.

### `product` CrashLoopBackOff 트러블슈팅 기록 반영
- `product` pod가 `AWS_ACCESS_KEY` placeholder 해석 실패로 죽던 장애를 분석했습니다.
- root cause는 Secret 키 체계 변경 자체보다, `:latest + IfNotPresent`와 서비스 Deployment 재적용이 겹치며 오래된 앱 이미지가 재사용된 배포 불일치였습니다.
- 상세 기록은 [`docs/troubleshooting-2026-04-03-product-crashloop.md`](./troubleshooting-2026-04-03-product-crashloop.md)에 남겼습니다.

### 쇼케이스 초기화 배치 안정성 보강
- `showcase-reset` CronJob이 실패하더라도 `PRODUCT_INIT_CLEAR_ES_ON_STARTUP` 임시 env를 제거하도록 cleanup trap을 보강했습니다.
- Redis pod 조회 실패 시 의도한 skip 분기에 도달하도록 pod 조회 함수를 안전하게 정리했습니다.
- Postgres 초기화/리셋 대상 DB 이름을 `[A-Za-z0-9_]` 패턴으로 검증해 잘못된 `SHOWCASE_DATABASES` 입력이 SQL에 직접 들어가지 않게 했습니다.

## 2026-03-17

### Cloudflare Tunnel 및 도메인 정리
- 프로덕션 브라우저 진입 도메인을 `dukku.earlydreamer.dev`로 고정하고, API/Grafana 공개 도메인은 `PUBLIC_API_HOST`, `PUBLIC_GRAFANA_HOST` 환경변수로 제어하도록 정리했습니다.
- `dukku.shop`, `api.dukku.shop`, Tailscale IP 기반 기본값을 제거하고 현재 도메인 구조에 맞는 설정으로 정리했습니다.

### 런타임별 설정 분리
- 루트 `.env`는 배포 기본값을 `postgres`, `redis`, `kafka`, `elasticsearch`, `mongodb-service` 같은 Kubernetes 서비스 DNS 기준으로 정리했습니다.
- 로컬 IDE와 로컬 Docker 실행은 `LOCAL_DB_*`, `LOCAL_REDIS_*`, `LOCAL_KAFKA_BOOTSTRAP_SERVERS`, `LOCAL_ELASTICSEARCH_URL`, `LOCAL_MONGO_*` 값을 통해 `localhost` 기반으로 분리했습니다.
- `create-secrets.sh`는 루트 `.env`에서 로컬 전용 `LOCAL_*` 키를 제외하고 Kubernetes Secret을 생성하도록 수정했습니다.

### Docker 및 네트워크 정리
- 공용 Docker 네트워크 이름을 `dukku-network`로 통일하고, Compose가 없으면 자동 생성하고 있으면 재사용하도록 변경했습니다.
- 로컬 DB/모니터링/Redpanda Compose의 레거시 `team05-*` 컨테이너명을 `semicolon-*` 계열로 정리했습니다.
- 더 이상 쓰지 않는 `docker-compose-tailscale.yml`은 제거했습니다.

### Kubernetes 및 문서 정리
- Redpanda 레거시 alias service(`team05-redpanda`)를 제거했습니다.
- 사용되지 않던 `k8s/_draft/base-20260219` 아카이브 스냅샷을 삭제했습니다.
- README, handover 문서, k8s 문서, frontend 문서의 오래된 절대 경로와 과거 환경 기준 설명을 현재 워크스페이스와 배포 구조에 맞게 수정했습니다.
- 루트 `.env.example`를 추가해 초기 bootstrap 시 `cp .env.example .env` 흐름으로 설정을 시작할 수 있게 정리했습니다.
- `restore.sh`가 k3d 로컬 bootstrap 시 커스텀 Postgres 이미지를 자동으로 빌드하고 import하도록 보강했습니다.
- `restore.sh`가 k3d 로컬 bootstrap 시 Spring Boot 앱 이미지(`auth`, `user`, `product`, `order`, `coupon`, `payment`, `deposit`, `settlement`, `ai`, `log-consumer`)도 함께 빌드/import하도록 확장했습니다.
- `log-consumer` Deployment에 `imagePullPolicy: IfNotPresent`를 추가해 로컬 import 이미지를 재사용하도록 보정했습니다.
- `redpanda` Deployment의 probe를 단일 노드 bootstrap 기준으로 조정해 startup/liveness/readiness 역할을 분리했습니다.
- `redpanda` Deployment가 이미지 기본 `/entrypoint.sh`를 우회하지 않도록 수정해 `--mode dev-container` 옵션이 정상 해석되게 정리했습니다.

### 검증
- `docker compose config`로 로컬 DB, 로컬 Redpanda, 로컬 모니터링, 로컬 Nginx, prod Compose 구성을 재검증했습니다.
- `bash -n k8s/semicolon/secrets/create-secrets.sh`로 Secret 생성 스크립트 문법을 확인했습니다.
- 실제 클러스터에 대한 `kubectl apply` 런타임 검증은 별도 환경에서 진행해야 합니다.

# 변경 이력

## 2026-03-17

### Cloudflare Tunnel 및 도메인 정리
- 프로덕션 브라우저 진입 도메인을 `dukku.earlydreamer.dev`로, API 공개 도메인을 `api.dukku.earlydreamer.dev`로 통일했습니다.
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

### 검증
- `docker compose config`로 로컬 DB, 로컬 Redpanda, 로컬 모니터링, 로컬 Nginx, prod Compose 구성을 재검증했습니다.
- `bash -n k8s/semicolon/secrets/create-secrets.sh`로 Secret 생성 스크립트 문법을 확인했습니다.
- 실제 클러스터에 대한 `kubectl apply` 런타임 검증은 별도 환경에서 진행해야 합니다.

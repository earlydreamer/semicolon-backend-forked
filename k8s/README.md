# K8s Manifests

## Source Of Truth

- 운영과 로컬 `k3d` 모두 `k8s/semicolon` 디렉토리를 기준으로 관리한다.
- 서버에서 직접 수정한 리소스는 SoT가 아니다.
- 변경 순서는 Git 변경 -> 검토 -> apply 순서를 따른다.
- `kubectl edit` 나 서버 직접 수정을 기준 상태로 삼지 않는다.

## Directory Layout

`k8s/semicolon` 구성은 아래 레이어로 나뉜다.

- `00-namespace.yml`
- `dependencies/`
  - `postgres`, `redis`, `redpanda`, `elasticsearch`
- `services/`
  - `auth`, `user`, `product`, `order`, `payment`, `coupon`, `deposit`, `settlement`, `ai`
- `monitoring/`
  - `mongodb`, `prometheus`, `grafana`, `log-consumer`
- `ingress/`
  - `api-gateway-ingress.yml` : prod 전용
  - `api-gateway-ingress.local.yml` : local 전용 hostless ingress
  - `clusterissuer-letsencrypt-prod.yml` : prod 전용
  - `grafana-ingress.yml` : prod 전용
- `edge/`
  - `cloudflared-deploy.yml` : 선택 적용
- `secrets/`
  - `.env.template`
  - `create-secrets.sh`

## Local k3d Workflow

### 1. 클러스터 생성

```bash
cd /mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked
bash scripts/k3d/create-local-cluster.sh
```

기본값:

- `K3D_CLUSTER_NAME=semicolon-local`
- `HTTP_PORT=8080`
- `HTTPS_PORT=8443`

필요하면 환경변수로 덮어쓴다.

```bash
HTTP_PORT=18080 HTTPS_PORT=18443 bash scripts/k3d/create-local-cluster.sh
```

### 2. 공통 시크릿 생성

```bash
cd /mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked/k8s/semicolon/secrets
bash create-secrets.sh
```

지원 환경변수:

- `K` 기본값 `kubectl`
- `NAMESPACE` 기본값 `semicolon`
- `SECRET_NAME` 기본값 `semicolon-env`
- `ENV_FILE` 기본값 `./.env.template`

빈 값은 Secret 생성에서 제외된다. 따라서 `.env.template` 에 값을 비워 두면 서비스 기본값 또는 코드 기본값을 사용한다.

### 3. 리소스 적용

```bash
cd /mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked
INGRESS_MODE=local ENABLE_EDGE=false bash scripts/apply.sh
```

기본 동작:

- namespace 적용
- `dependencies/` 적용
- `services/` 적용
- `monitoring/` 적용
- `api-gateway-ingress.local.yml` 적용
- `edge/` 는 `ENABLE_EDGE=true` 일 때만 적용

지원 환경변수:

- `K` 기본값 `kubectl`
- `NS` 기본값 `semicolon`
- `INGRESS_MODE` 기본값 `local`
- `ENABLE_MONITORING` 기본값 `true`
- `ENABLE_EDGE` 기본값 `false`
- `ENABLE_CERT_MANAGER` 기본값 `false`

### 4. 복구 순서 실행

```bash
cd /mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked
bash scripts/restore.sh
```

기본 복구 순서:

1. `postgres`, `redis`, `redpanda`, `elasticsearch`, `mongodb`
2. `prometheus`, `grafana`, `log-consumer`
3. `auth`, `user`, `product`

전체 앱까지 올리려면:

```bash
APP_SET=all bash scripts/restore.sh
```

### 5. 클러스터 삭제

```bash
cd /mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked
bash scripts/k3d/delete-local-cluster.sh
```

## Ingress Policy

- 로컬 기본 ingress 는 `api-gateway-ingress.local.yml` 이다.
- local ingress 는 host 를 지정하지 않는다.
- 저장소에는 공개 도메인을 하드코딩하지 않는다.
- prod 도메인/TLS 정책은 `api-gateway-ingress.yml`, `grafana-ingress.yml`, `clusterissuer-letsencrypt-prod.yml` 에 남겨 두고 로컬 기본 흐름에서는 적용하지 않는다.

## Cloudflared Policy

- `cloudflared` 는 `TUNNEL_TOKEN` 기반 `tunnel run` 으로만 동작한다.
- 공개 hostname, public route, ingress rule 은 Cloudflare Tunnel 원격 설정에서 관리한다.
- 저장소의 로컬 매니페스트에는 Cloudflare 공개 도메인을 적지 않는다.
- 로컬에서 `cloudflared` 를 붙이려면 `semicolon-env` Secret 에 `CLOUDFLARE_TUNNEL_TOKEN` 을 넣고 아래처럼 적용한다.

```bash
cd /mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked
ENABLE_EDGE=true bash scripts/apply.sh
```

## Validation Notes

- 서비스 Deployment 는 공통 `semicolon-env` Secret 을 필수로 사용한다.
- 서비스별 `*-db` Secret 은 선택 사항이다. 없으면 공통 `DB_*` 값을 사용한다.
- `scripts/check-cluster-readiness.sh` 로 배포 후 상태를 빠르게 점검할 수 있다.

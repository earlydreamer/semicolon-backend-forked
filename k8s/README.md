# K8s Manifests

## Source Of Truth

- 운영과 로컬 `k3d` 모두 `k8s/semicolon` 디렉토리를 기준으로 관리한다.
- GitHub Actions 배포도 이 매니페스트를 원격 맥북으로 전송한 뒤 `scripts/apply.sh`를 실행하는 방식이다.
- 서버나 클러스터에서 직접 수정한 리소스는 SoT가 아니다.
- 변경 순서는 Git 변경 -> 검토 -> apply/rollout 순서를 따른다.

## Current Deployment Topology

- GitHub-hosted runner가 `dev` push를 감지한다.
- runner가 변경 모듈만 `linux/arm64` 이미지로 빌드해 Docker Hub에 푸시한다.
- 같은 runner가 `cloudflared access tcp`로 Cloudflare Access 보호 SSH hostname에 접속한다.
- 접속 대상은 M1 맥북 host이며, `cloudflared`는 맥북 host 서비스로 실행한다.
- 맥북 host는 `k3d` 클러스터에 `scripts/apply.sh`를 적용하고, 변경 Deployment만 `set image -> rollout` 한다.
- 앱 공개 트래픽도 같은 Tunnel에서 `http://localhost:8080`으로 라우팅한다.

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
- `secrets/`
  - `.env.example`를 복사해 생성하는 루트 `.env`
  - `create-secrets.sh`

## Local M1 Bootstrap

### 1. 맥북 host 준비

- Docker Desktop 설치
- `kubectl`, `k3d`, `jq`, `cloudflared` 설치
- macOS `Remote Login` 활성화
- 배포 전용 SSH 사용자와 공개키 인증 준비

### 2. k3d 클러스터 생성

```bash
cd /mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked
bash scripts/k3d/create-local-cluster.sh
```

기본값:

- `K3D_CLUSTER_NAME=semicolon-local`
- `HTTP_PORT=8080`
- `HTTPS_PORT=8443`

포트를 바꾸려면 환경변수로 덮어쓴다.

```bash
HTTP_PORT=18080 HTTPS_PORT=18443 bash scripts/k3d/create-local-cluster.sh
```

### 3. 공통 Secret 생성

먼저 루트 `.env.example`를 복사해 `.env`를 만들고 값을 채운다.

```bash
cd /mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked
cp .env.example .env
```

그 다음 Secret을 생성한다.

```bash
cd /mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked/k8s/semicolon/secrets
bash create-secrets.sh
```

지원 환경변수:

- `K` 기본값 `kubectl`
- `NAMESPACE` 기본값 `semicolon`
- `SECRET_NAME` 기본값 `semicolon-env`
- `ENV_FILE` 기본값 `backend/semicolon-backend-forked/.env`

빈 값은 Secret 생성에서 제외된다. 따라서 루트 `.env`에서 값을 비워 두면 서비스 기본값 또는 코드 기본값을 사용한다.
`LOCAL_*` 값은 로컬 IDE/host nginx 실행용이며, `create-secrets.sh`가 Kubernetes Secret 생성 시 자동으로 제외한다.

### 4. 최초 복구

```bash
cd /mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked
APP_SET=all bash scripts/restore.sh
```

`restore.sh`는 아래 순서로 리소스를 기동한다.

1. 현재 context가 `k3d-*`이면 `scripts/k3d/ensure-local-dependency-images.sh`로 커스텀 Postgres 이미지와 앱 이미지를 host에서 빌드하고 클러스터에 import
2. `apply.sh`로 namespace, dependencies, services, monitoring, ingress 반영
3. `postgres`, `redis`, `redpanda`, `elasticsearch`, `mongodb`
4. `prometheus`, `grafana`, `log-consumer`
5. `auth`, `user`, `product`
6. `APP_SET=all`일 때 `order`, `payment`, `coupon`, `deposit`, `settlement`, `ai`

이 단계는 최초 bootstrap 또는 장애 복구용이다. 일상 배포에서는 GitHub Actions가 `restore.sh`를 호출하지 않는다.

`redpanda`는 단일 노드 bootstrap에서 초기 topic/controller 선출 시간이 걸릴 수 있으므로, liveness는 `rpk cluster info`, readiness는 `rpk cluster health` 기준으로 분리해 두었다.

## Apply Script

```bash
cd /mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked
INGRESS_MODE=local bash scripts/apply.sh
```

기본 동작:

- namespace 적용
- `dependencies/` 적용
- `services/` 적용
- `monitoring/` 적용
- `api-gateway-ingress.local.yml` 적용

지원 환경변수:

- `K` 기본값 `kubectl`
- `NS` 기본값 `semicolon`
- `INGRESS_MODE` 기본값 `local`
- `ENABLE_MONITORING` 기본값 `true`
- `ENABLE_CERT_MANAGER` 기본값 `false`

## Host-Level Cloudflare Tunnel

- `cloudflared`는 K8s 내부가 아니라 맥북 host에서 서비스로 실행한다.
- 저장소 매니페스트에는 공개 hostname을 하드코딩하지 않는다.
- 앱 공개 경로는 Cloudflare Tunnel 원격 설정에서 `http://localhost:8080`으로 보낸다.
- 배포 SSH 경로는 같은 Tunnel에서 `ssh://localhost:22`로 보낸다.
- GitHub Actions는 SSH hostname에 대해 `cloudflared access tcp`와 Service Token을 사용한다.

예시 설치:

```bash
sudo cloudflared service install <TUNNEL_TOKEN>
```

Cloudflare 측 수동 준비:

- Tunnel public hostname A -> `http://localhost:8080`
- Tunnel public hostname B -> `ssh://localhost:22`
- SSH hostname용 Access self-hosted app 생성
- GitHub Actions 전용 Service Auth 정책 연결

## GitHub Actions CD

현재 자동 배포 워크플로는 `.github/workflows/deploy-m1-tunnel.yml`이다.

트리거:

- `push` on `dev`
- `workflow_dispatch`

동작 요약:

1. 변경 모듈 계산
2. 변경 모듈 `linux/arm64` 이미지 빌드 및 Docker Hub 푸시
3. Tunnel SSH로 맥북에 배포 번들 업로드
4. `create-secrets.sh` 실행
5. `scripts/apply.sh` 실행
6. 변경 Deployment만 `set image -> annotation patch -> rollout status`

배포 job 필수 GitHub Secrets:

- Docker Hub: `DOCKER_USERNAME`, `DOCKER_PASSWORD`
- Tunnel Access: `CF_ACCESS_SERVICE_TOKEN_ID`, `CF_ACCESS_SERVICE_TOKEN_SECRET`
- SSH: `M1_DEPLOY_SSH_HOSTNAME`, `M1_DEPLOY_USER`, `M1_DEPLOY_SSH_KEY`, `M1_DEPLOY_KNOWN_HOSTS`
- 앱 공통 env: `DB_USERNAME`, `DB_PASSWORD`, `JWT_ACCESS_SECRET`, `JWT_REFRESH_SECRET`, `CRYPTO_KEY`, `INTERNAL_SERVICE_TOKEN`, `OPENAI_API_KEY`, `SYSTEM_DEPOSIT_PASSWORD`, `TOSS_API_SECRET_KEY`, `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`, `AWS_REGION`, `AWS_S3_BUCKET`, `GOOGLE_OAUTH_CLIENT_ID`, `GOOGLE_OAUTH_CLIENT_SECRET`, `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD`, `SLACK_WEBHOOK_URL_SETTLEMENT`, `SLACK_WEBHOOK_URL_GRAFANA`

선택 GitHub Variables:

- `K8S_NAMESPACE` 기본값 `semicolon`
- `REMOTE_WORKSPACE` 기본값 `deploy/semicolon-backend`

## Ingress Policy

- 로컬 기본 ingress는 `api-gateway-ingress.local.yml`이다.
- local ingress는 host를 지정하지 않는다.
- 저장소에는 공개 도메인을 하드코딩하지 않는다.
- prod 도메인/TLS 정책은 `api-gateway-ingress.yml`, `grafana-ingress.yml`, `clusterissuer-letsencrypt-prod.yml`에 남겨 두고 로컬 기본 흐름에서는 적용하지 않는다.

## Validation Notes

- 서비스 Deployment는 공통 `semicolon-env` Secret을 필수로 사용한다.
- 서비스별 `*-db` Secret은 선택 사항이다. 없으면 공통 `DB_*` 값을 사용한다.
- 새 클러스터 bootstrap 후에는 `kubectl get deploy,pods,ingress -n semicolon`과 `scripts/check-cluster-readiness.sh`로 상태를 확인한다.
- Tunnel 및 SSH는 GitHub Actions와 같은 방식으로 `cloudflared access tcp --hostname <ssh-host> --url localhost:2222`로 사전 검증할 수 있다.

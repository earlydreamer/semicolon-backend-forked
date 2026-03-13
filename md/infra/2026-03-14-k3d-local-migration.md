# 2026-03-14 k3d 로컬 k3s 복원 작업 로그

## 작업 목표

- M1 맥북에서 `k3d`로 기존 `k3s` 운영 구조를 로컬에 복원한다.
- 로컬 매니페스트에는 공개 도메인을 하드코딩하지 않고, 외부 도메인 제어는 Cloudflare Tunnel 원격 설정에 맡긴다.
- 클러스터 내부 라우팅은 hostless Traefik ingress를 기준으로 정리한다.

## 1차 커밋 예정 범위

### 의도

- 운영에서 빠져 있던 `k8s/semicolon/dependencies` 레이어를 Git SoT로 복원한다.
- Postgres를 `services`에서 분리해 의존성 계층으로 이동한다.
- 로컬 hostless ingress와 토큰 기반 `cloudflared` 배포 초안을 K8s 자산으로 추가한다.

### 변경

- `k8s/semicolon/dependencies`에 `postgres`, `redis`, `redpanda`, `elasticsearch` 매니페스트를 추가했다.
- `k8s/semicolon/services/postgres-deploy.yml` 삭제를 유지해 DB 배치를 `dependencies`로 일원화했다.
- `k8s/semicolon/ingress/api-gateway-ingress.local.yml`을 추가해 host 없는 로컬 ingress를 분리했다.
- `k8s/semicolon/edge/cloudflared-deploy.yml`을 추가해 `TUNNEL_TOKEN` 기반 실행 구조를 잡았다.

### 검증

- `python3` YAML 파싱으로 `dependencies`, local ingress, `cloudflared` 매니페스트 문법을 확인했다.
- `kubectl apply --dry-run=client`는 현재 연결된 클러스터가 없어 OpenAPI/API recognition 단계에서 검증을 진행하지 못했다.

### 다음 단계

- 서비스별 `envFrom` 완화와 `.env.template`, `create-secrets.sh`, `apply.sh`, `restore.sh`, `k3d` 스크립트를 정리한다.

## 2차 커밋 예정 범위

### 의도

- 로컬 `k3d` 부트스트랩에 필요한 시크릿 주입 방식과 복구 스크립트를 정리한다.
- 기존 서비스 매니페스트가 실제로 `kubectl apply` 가능한 형태가 되도록 `template.metadata`와 `secretRef` 구조를 정상화한다.

### 변경

- `create-secrets.sh`에 `K`, `NAMESPACE`, `SECRET_NAME`, `ENV_FILE` 주입 옵션을 추가하고, 빈 env 키를 Secret 생성에서 제외하도록 바꿨다.
- `.env.template`을 `release` 기준 로컬 기본값과 placeholder 중심으로 다시 구성했다.
- `scripts/apply.sh`를 local/prod ingress, monitoring, edge, cert-manager 토글 방식으로 재작성했다.
- `scripts/restore.sh`를 `APP_SET=core|all` 기반 복구 흐름으로 재작성했다.
- `scripts/k3d/create-local-cluster.sh`, `scripts/k3d/delete-local-cluster.sh`를 추가했다.
- 서비스 Deployment의 `template.metadata` 누락을 보정하고, 서비스별 `*-db` secret을 `optional: true`로 완화했다.

### 검증

- `bash -n`으로 `create-secrets.sh`, `apply.sh`, `restore.sh`, `scripts/k3d/*.sh` 문법을 확인했다.
- `python3` YAML 파싱으로 `k8s/semicolon` 하위 YAML 42개를 모두 확인했다.
- 현재 작업 환경에는 `k3d` 바이너리가 없어 실제 로컬 클러스터 기동 검증은 아직 수행하지 못했다.

### 다음 단계

- `k8s/README.md`를 로컬 `k3d` 기준으로 다시 쓰고, 남은 수동 검증 절차와 제약사항을 작업 로그에 정리한다.

## 3차 커밋 예정 범위

### 의도

- 로컬 `k3d` 사용법과 prod/local 차이를 문서로 고정한다.
- 이번 세션에서 실제로 검증된 범위와 아직 남은 수동 검증 범위를 명확히 남긴다.

### 변경

- `k8s/README.md`를 `dependencies`, `services`, `monitoring`, `ingress`, `edge`, `secrets` 기준으로 다시 정리했다.
- 로컬 `k3d` 생성, Secret 생성, apply, restore, delete 절차를 명령 예시와 함께 문서화했다.
- `cloudflared`는 토큰 기반 실행만 지원하고, 공개 도메인과 라우팅은 Cloudflare 원격 설정에서 관리한다는 원칙을 명시했다.

### 검증

- 문서에 기술한 스크립트 이름, 기본 환경변수 이름, ingress/cloudflared 정책이 현재 Git 트리와 일치하는지 재확인했다.
- `k3d` 바이너리가 없는 환경이라 실제 `k3d create -> kubectl apply -> restore` 런타임 검증은 아직 수행하지 못했다.

### 남은 수동 검증

- `k3d` 설치 후 `bash scripts/k3d/create-local-cluster.sh`
- `bash k8s/semicolon/secrets/create-secrets.sh`
- `INGRESS_MODE=local ENABLE_EDGE=false bash scripts/apply.sh`
- `bash scripts/restore.sh`
- 필요 시 `ENABLE_EDGE=true` 와 실제 `CLOUDFLARE_TUNNEL_TOKEN` 으로 `cloudflared` Pod Ready 확인

## 4차 커밋 예정 범위

### 의도

- EC2 SSH 전용 GitHub Actions 배포 경로를 제거하고, M1 맥북 host로 향하는 Tunnel SSH 배포 경로를 새 표준으로 전환한다.
- 빌드는 GitHub-hosted runner에서 수행하고, 원격 맥북에서는 매니페스트 동기화와 변경 모듈 롤아웃만 수행하도록 역할을 분리한다.

### 변경

- `.github/workflows/deploy.yml`, `.github/workflows/deploy-all.yml`을 삭제해 EC2 배포 잔재를 제거했다.
- `.github/workflows/deploy-m1-tunnel.yml`을 추가해 `dev` push 시 `linux/arm64` 이미지를 빌드하고 Tunnel SSH로 M1 맥북에 배포하도록 구성했다.
- `scripts/remote-deploy.sh`를 추가해 원격 맥북에서 Secret 생성, `apply.sh`, 이미지 롤아웃 흐름을 한 번에 실행하도록 정리했다.
- `scripts/rollout-images.sh`를 추가해 변경된 모듈만 `kubectl set image -> annotation patch -> rollout status` 순서로 배포하도록 분리했다.

### 검증

- `python3` YAML 파싱으로 `deploy-m1-tunnel.yml` 문법을 확인했다.
- `bash -n`으로 `scripts/remote-deploy.sh`, `scripts/rollout-images.sh` 문법을 확인했다.
- `cloudflared access tcp` 도움말을 확인해 Service Token 기반 SSH 포워딩 인자 구성을 검증했다.

### 다음 단계

- K8s 내부 `cloudflared` 경로와 `ENABLE_EDGE` 토글을 제거하고, host-level `cloudflared` 기준으로 스크립트와 문서를 다시 정리한다.

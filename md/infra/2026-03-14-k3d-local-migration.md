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

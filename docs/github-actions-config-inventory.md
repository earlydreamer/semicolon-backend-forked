# GitHub Actions Config Inventory

현재 백엔드 저장소의 배포 기준은 `GitHub Secrets + .env.example` 이다.

- `.env.example`
  - 로컬 부팅과 수동 배포 기준 예시 파일
  - GitHub Secrets에 복사해 넣을 런타임/배포 설정의 기준 파일
- GitHub Secrets
  - CI 배포 시 source of truth
  - `.env.example`와 같은 키를 우선 사용하고, CI 전용 secret만 별도 예시 파일로 관리한다
- GitHub Variables
  - 표준 경로에서는 사용하지 않는다.

## 1. `.env.example`와 값을 맞춰서 GitHub Secrets에 등록하는 키

아래 값은 `.env.example`에도 있고, GitHub Secrets에도 같은 이름으로 등록해서 사용한다.

### 배포 제어 / 공개 호스트

- `K8S_NAMESPACE`
- `REMOTE_WORKSPACE`
- `DEPLOY_INGRESS_MODE`
- `DEPLOY_ENABLE_MONITORING`
- `DEPLOY_ENABLE_CERT_MANAGER`
- `PUBLIC_WEB_HOST`
- `PUBLIC_API_HOST`
- `PUBLIC_GRAFANA_HOST`
- `CORS_ALLOWED_ORIGINS`

### 런타임 / 외부 연동

- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_PASSWORD`
- `JWT_ACCESS_SECRET`
- `JWT_REFRESH_SECRET`
- `CRYPTO_KEY`
- `INTERNAL_SERVICE_TOKEN`
- `TOSS_API_SECRET_KEY`
- `OBJECT_STORAGE_ACCESS_KEY`
- `OBJECT_STORAGE_SECRET_KEY`
- `OBJECT_STORAGE_REGION`
- `OBJECT_STORAGE_BUCKET`
- `OBJECT_STORAGE_ENDPOINT`
- `OBJECT_STORAGE_PATH_STYLE_ACCESS_ENABLED`
- `AWS_ACCESS_KEY` (legacy fallback)
- `AWS_SECRET_KEY` (legacy fallback)
- `AWS_REGION` (legacy fallback)
- `AWS_S3_BUCKET` (legacy fallback)
- `GOOGLE_OAUTH_CLIENT_ID`
- `GOOGLE_OAUTH_CLIENT_SECRET`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `EMAIL_PASSWORD`
- `GRAFANA_ADMIN_USER`
- `GRAFANA_ADMIN_PASSWORD`
- `SLACK_WEBHOOK_URL_SETTLEMENT`
- `SLACK_WEBHOOK_URL_GRAFANA`
- `OPENAI_API_KEY`
- `SYSTEM_DEPOSIT_PASSWORD`

## 2. GitHub Secrets에만 두는 CI/CD 전용 키

아래 값은 앱 런타임 설정이 아니라 CI/CD 접속과 배포 단계에만 사용되므로 `.env.example`에 넣지 않는다.

이 값들은 루트 [`github-actions-secrets.example.env`](/mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked/github-actions-secrets.example.env) 에만 정리한다.

- `DOCKER_USERNAME`
- `DOCKER_PASSWORD`
- `M1_DEPLOY_USER`
- `M1_DEPLOY_SSH_HOSTNAME`
- `M1_DEPLOY_SSH_KEY`
- `M1_DEPLOY_KNOWN_HOSTS`
- `CF_ACCESS_SERVICE_TOKEN_ID`
- `CF_ACCESS_SERVICE_TOKEN_SECRET`

## 3. create-secrets.sh에서 제외되는 키

아래 값은 `.env`에 있어도 Kubernetes `semicolon-env` Secret으로 넣지 않는다.

- `DOCKER_SHARED_NETWORK`
- `K8S_NAMESPACE`
- `REMOTE_WORKSPACE`
- `DEPLOY_INGRESS_MODE`
- `DEPLOY_ENABLE_MONITORING`
- `DEPLOY_ENABLE_CERT_MANAGER`

이 값들은 배포 제어 또는 로컬 Docker 설정용이므로 앱 컨테이너 런타임 Secret에는 불필요하다.

## 4. 주의사항

- `REDIS_PASSWORD`
  - 선택값이다.
  - Redis 비밀번호를 쓰지 않으면 GitHub Secret을 등록하지 않는다.
  - 빈 문자열을 GitHub Secret에 넣는 방식으로 관리하지 않는다.
- `PUBLIC_*`
  - ingress 렌더링과 OAuth / redirect 기본값에 함께 영향을 준다.
- `GitHub Variables`
  - 표준 배포 경로에서는 더 이상 사용하지 않는다.

## 5. 운영 원칙

- 런타임/배포 설정값
  - 먼저 `.env.example`를 기준으로 관리한다.
  - GitHub Secrets에도 같은 키 이름으로 등록한다.
- CI 전용 접속 자격증명
  - 루트 `github-actions-secrets.example.env`를 기준으로 관리한다.
- 같은 값이 두 파일에 중복으로 들어가지 않게 유지한다.

## 6. 참고 파일

- [`.env.example`](/mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked/.env.example)
- [`github-actions-secrets.example.env`](/mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked/github-actions-secrets.example.env)
- [`deploy-m1-tunnel.yml`](/mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked/.github/workflows/deploy-m1-tunnel.yml)
- [`ci.yml`](/mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked/.github/workflows/ci.yml)

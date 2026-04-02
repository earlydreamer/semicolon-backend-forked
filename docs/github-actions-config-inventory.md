# GitHub Actions Config Inventory

현재 백엔드 저장소의 배포 기준은 `GitHub Secrets + GitHub Variables + .env.example` 이다.

- `.env.example`
  - 로컬 부팅과 수동 배포 기준 예시 파일
  - GitHub Actions 설정값의 기준 파일
- GitHub Secrets
  - 비밀번호, 토큰, private key처럼 민감한 값만 보관한다
- GitHub Variables
  - 공개 호스트, 배포 토글, 사용자명처럼 비민감한 값을 보관한다

## 1. GitHub Variables로 두는 비민감 키

아래 값은 `.env.example`를 기준으로 관리하되, GitHub Actions에서는 Variables에 같은 이름으로 등록해서 사용한다.

### 배포 제어 / 공개 호스트

- `DOCKER_USERNAME`
- `K8S_NAMESPACE`
- `REMOTE_WORKSPACE`
- `DEPLOY_INGRESS_MODE`
- `DEPLOY_ENABLE_MONITORING`
- `DEPLOY_ENABLE_CERT_MANAGER`
- `PUBLIC_WEB_HOST`
- `PUBLIC_API_HOST`
- `PUBLIC_GRAFANA_HOST`
- `CORS_ALLOWED_ORIGINS`
- `M1_DEPLOY_USER`
- `M1_DEPLOY_SSH_HOSTNAME`

### 런타임 / 외부 연동

- `INIT_ADMIN_EMAIL`
- `OBJECT_STORAGE_REGION`
- `OBJECT_STORAGE_BUCKET`
- `OBJECT_STORAGE_ENDPOINT`
- `OBJECT_STORAGE_PATH_STYLE_ACCESS_ENABLED`
- `GOOGLE_OAUTH_CLIENT_ID`
- `GRAFANA_ADMIN_USER`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`

## 2. GitHub Secrets로 두는 민감 키

아래 값은 런타임에 필요하더라도 민감 정보이므로 Secrets에 같은 이름으로 등록해서 사용한다.

- `DB_USERNAME`
- `DB_PASSWORD`
- `REDIS_PASSWORD`
- `JWT_ACCESS_SECRET`
- `JWT_REFRESH_SECRET`
- `CRYPTO_KEY`
- `INTERNAL_SERVICE_TOKEN`
- `INIT_ADMIN_PASSWORD`
- `TOSS_API_SECRET_KEY`
- `OBJECT_STORAGE_ACCESS_KEY`
- `OBJECT_STORAGE_SECRET_KEY`
- `AWS_ACCESS_KEY` (legacy fallback)
- `AWS_SECRET_KEY` (legacy fallback)
- `AWS_REGION` (legacy fallback)
- `AWS_S3_BUCKET` (legacy fallback)
- `GOOGLE_OAUTH_CLIENT_SECRET`
- `EMAIL_PASSWORD`
- `GRAFANA_ADMIN_PASSWORD`
- `SLACK_WEBHOOK_URL_SETTLEMENT`
- `SLACK_WEBHOOK_URL_GRAFANA`
- `OPENAI_API_KEY`
- `SYSTEM_DEPOSIT_PASSWORD`

## 3. GitHub Secrets에만 두는 CI/CD 전용 키

아래 값은 앱 런타임 설정이 아니라 CI/CD 접속과 배포 단계에만 사용되므로 `.env.example`에 넣지 않는다.

이 값들은 루트 [`github-actions-secrets.example.env`](/mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked/github-actions-secrets.example.env) 에만 정리한다.

- `DOCKER_PASSWORD`
- `M1_DEPLOY_SSH_KEY`
- `M1_DEPLOY_KNOWN_HOSTS`
- `CF_ACCESS_SERVICE_TOKEN_ID`
- `CF_ACCESS_SERVICE_TOKEN_SECRET`
- `MIRROR_REPOSITORY`
- `MIRROR_REPO_TOKEN`

## 4. create-secrets.sh에서 제외되는 키

아래 값은 `.env`에 있어도 Kubernetes `semicolon-env` Secret으로 넣지 않는다.

- `DOCKER_SHARED_NETWORK`
- `K8S_NAMESPACE`
- `REMOTE_WORKSPACE`
- `DEPLOY_INGRESS_MODE`
- `DEPLOY_ENABLE_MONITORING`
- `DEPLOY_ENABLE_CERT_MANAGER`

이 값들은 배포 제어 또는 로컬 Docker 설정용이므로 앱 컨테이너 런타임 Secret에는 불필요하다.

## 5. 주의사항

- `REDIS_PASSWORD`
  - 선택값이다.
  - Redis 비밀번호를 쓰지 않으면 GitHub Secret을 등록하지 않는다.
  - 빈 문자열을 GitHub Secret에 넣는 방식으로 관리하지 않는다.
- `PUBLIC_*`
  - ingress 렌더링과 OAuth / redirect 기본값에 함께 영향을 준다.
- `Variables vs Secrets`
  - 비밀번호/토큰/키는 무조건 Secrets에 둔다.
  - 사용자명/호스트명/포트/토글은 Variables에 둔다.
- `Mirror Push`
  - 대상 저장소는 fork가 아닌 별도 저장소로 새로 만든다.
  - `git push --mirror`를 쓰므로 대상 저장소는 mirror 전용 빈 저장소로 두는 편이 안전하다.
  - 잔디 반영이 목적이면 대상 저장소의 default branch를 현재 사용하는 브랜치와 맞춘다. 이 저장소 기준으로는 `dev`가 가장 자연스럽다.
  - 커밋 author email이 GitHub 계정의 verified email과 일치해야 contribution graph에 잡힌다.
  - private 저장소면 GitHub 프로필의 private contributions 표시 옵션도 켜야 한다.
## 6. 운영 원칙

- 런타임/배포 설정값
  - 먼저 `.env.example`를 기준으로 관리한다.
  - 비민감값은 GitHub Variables에 같은 키 이름으로 등록한다.
  - 민감값은 GitHub Secrets에 같은 키 이름으로 등록한다.
- CI 전용 접속 자격증명
  - 루트 `github-actions-secrets.example.env`를 기준으로 관리한다.
- 비민감 CI/배포 식별자
  - 루트 `github-actions-variables.example.env`를 기준으로 관리한다.
- 같은 값이 Variables와 Secrets에 중복으로 들어가지 않게 유지한다.

## 7. 참고 파일

- [`.env.example`](/mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked/.env.example)
- [`github-actions-variables.example.env`](/mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked/github-actions-variables.example.env)
- [`github-actions-secrets.example.env`](/mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked/github-actions-secrets.example.env)
- [`deploy-m1-tunnel.yml`](/mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked/.github/workflows/deploy-m1-tunnel.yml)
- [`ci.yml`](/mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked/.github/workflows/ci.yml)
- [`mirror-push.yml`](/mnt/d/Projects/Programmers/Final-Project-Fork/backend/semicolon-backend-forked/.github/workflows/mirror-push.yml)

# K8s Manifests (SoT)

## 1. Source of Truth (SoT)

- 클러스터 배포는 이 레포의 k8s manifests를 기준으로 한다.
- EC2(서버)에만 존재하는 수동 수정 내용은 Source of Truth가 아니다.
- 변경은 반드시 Git → PR → 리뷰 → 머지 → apply 순으로 진행한다.

---

## 2. Directory Strategy

현재 구조는 두 단계로 나뉜다.

### 2-1. k8s/_draft/base-20260219/

- namespace
- services
- deployments
- ingress

→ 현재 클러스터에서 추출한 "베이스 스냅샷"
→ 이후 리팩터링 및 구조 통합 대상

### 2-2. k8s/semicolon/

- 기존 서버 운영 기준 매니페스트
- legacy / per-service ingress 템플릿 포함

→ 점진적으로 draft 구조로 이관 예정

---

## 3. Apply Target (Current 기준)

현재 apply 기본 대상은 다음이다:

k8s/_draft/base-20260219/

### Apply Example

kubectl apply -f k8s/_draft/base-20260219/namespace
kubectl apply -f k8s/_draft/base-20260219/services -n semicolon
kubectl apply -f k8s/_draft/base-20260219/deployments -n semicolon
kubectl apply -f k8s/_draft/base-20260219/ingress -n semicolon

---

## 4. Ingress 정책

- 현재 운영 ingress는 api-gateway-ingress 기준
- 서비스별 ingress는 "서브도메인 대비용 템플릿"
- 기본 apply 대상이 아님

---

## 5. Legacy 정책

- k8s/semicolon/legacy 는 과거 백업
- 운영 apply 대상이 아님
- 향후 삭제 또는 archive 예정

---

## 6. 목표 상태

- Git이 단일 Source of Truth
- CI/CD → kubectl apply 자동화
- 수동 서버 수정 제거
- Kustomize 또는 Overlay 구조 도입 예정


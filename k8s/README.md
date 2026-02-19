# K8s Manifests (SoT)

## 1. Source of Truth (SoT)

- 클러스터 배포 기준은 `k8s/semicolon` 디렉토리이다.
- 서버(EC2)에서 수동 수정한 내용은 Source of Truth가 아니다.
- 모든 변경은 Git → PR → 리뷰 → 머지 → CI → kubectl apply 순으로 진행한다.
- kubectl edit / 서버 직접 수정은 금지한다.

---

## 2. Directory Structure

### 2-1. k8s/semicolon (운영 기준 SoT)

구조는 서비스 단위로 구성된다.

k8s/semicolon/
├── 00-namespace.yml
├── ingress/
│   ├── api-gateway-ingress.yml
│   └── clusterissuer-letsencrypt-prod.yml
├── services/
│   ├── auth/
│   │   ├── deploy.yml
│   │   └── svc.yml
│   ├── user/
│   ├── product/
│   ├── order/
│   ├── payment/
│   ├── coupon/
│   ├── deposit/
│   └── settlement/

설명:

- 서비스별 디렉토리 구조는 MSA 경계 기준을 반영한다.
- 각 서비스는 deploy.yml + svc.yml을 가진다.
- ingress는 api-gateway-ingress 기준으로 운영한다.
- per-service ingress 템플릿은 기본 apply 대상이 아니다.

---

### 2-2. k8s/_draft (참고/과거 스냅샷)

- 과거 클러스터 상태 스냅샷
- 현재 운영 apply 대상이 아님
- 점진적으로 정리 또는 삭제 예정

---

## 3. Apply Target (Current)

현재 운영 apply 기준은 아래 디렉토리이다.

k8s/semicolon

### Apply 순서

1) Namespace

kubectl apply -f k8s/semicolon/00-namespace.yml


2) Ingress

kubectl -n semicolon apply -f k8s/semicolon/ingress


3) Services (서비스별)

for svc in auth user product order coupon payment deposit settlement; do
kubectl -n semicolon apply -f k8s/semicolon/services/$svc
done

---

## 4. Ingress 정책

- 운영 ingress는 api-gateway-ingress 기준
- per-service ingress는 서브도메인 대비용 템플릿
- 기본 apply 대상이 아님

---

## 5. YAML 컨벤션

- 확장자는 `.yml`로 통일한다.
- `kubectl get -o yaml` 결과를 그대로 커밋하지 않는다.
- metadata.resourceVersion, status, clusterIP 등 런타임 필드는 제거한다.
- Service spec에는 clusterIP, ipFamilies 등을 포함하지 않는다.

정상 Service 예시:

apiVersion: v1
kind: Service
metadata:
name: coupon-service
namespace: semicolon
spec:
type: ClusterIP
selector:
app: coupon
ports:
- name: http
port: 80
targetPort: 8085
protocol: TCP

---

## 6. 목표 상태

- Git 단일 SoT 유지
- SHA 기반 이미지 태그 전략 적용
- imagePullPolicy 명확화
- envFrom + common-secret 구조 적용
- CI에서 자동 kubectl set image 또는 apply 수행
- 수동 서버 수정 완전 제거

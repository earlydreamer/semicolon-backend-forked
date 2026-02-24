#!/usr/bin/env bash
set -euo pipefail

NAMESPACE=${1:-semicolon}

echo "네임스페이스: $NAMESPACE 의 k8s 리소스 점검을 시작합니다"

echo "\n1) 서비스 점검: redpanda/redis/postgres 조회"
kubectl get svc -n $NAMESPACE -o wide | grep -E 'redpanda|redis|postgres' || echo "redpanda/redis/postgres 서비스가 없습니다"

echo "\n2) Running 또는 Completed 상태가 아닌 파드 목록"
kubectl get pods -n $NAMESPACE --no-headers | awk '
  $3 != "Running" && $3 != "Completed" {print "POD: " $1 " 상태: " $3 " 재시작: " $4}
' || true

echo "\n3) Ingress 목록"
kubectl get ingress -n $NAMESPACE -o custom-columns=NAME:.metadata.name,HOSTS:.spec.rules[*].host

echo "\n4) semicolon-env 시크릿 키 확인 (base64 인코딩)"
if kubectl get secret semicolon-env -n $NAMESPACE >/dev/null 2>&1; then
  echo "시크릿 semicolon-env의 키 목록:"
  kubectl get secret semicolon-env -n $NAMESPACE -o jsonpath='{.data}' | jq 'keys'
else
  echo "네임스페이스 $NAMESPACE 에 semicolon-env 시크릿이 없습니다"
fi

echo "\n점검을 종료합니다."

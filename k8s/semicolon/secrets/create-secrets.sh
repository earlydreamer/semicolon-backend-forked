#!/usr/bin/env bash
set -e

NAMESPACE=semicolon
SECRET_NAME=semicolon-env
ENV_FILE="./.env.template"

if [ ! -f "$ENV_FILE" ]; then
  echo "Env 템플릿 $ENV_FILE을(를) 찾을 수 없습니다. 먼저 생성하고 실제 값으로 채워주세요." >&2
  exit 1
fi

# .env 파일에서 시크릿 생성 (값은 평문이어야 함)
kubectl -n $NAMESPACE create secret generic $SECRET_NAME --from-env-file=$ENV_FILE --dry-run=client -o yaml | kubectl apply -f -

echo "네임스페이스 '$NAMESPACE'에 시크릿 '$SECRET_NAME'을(를) 적용했습니다."

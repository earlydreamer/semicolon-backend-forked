# User Flyway Ops

간단한 적용/확인 절차(운영 k3s 클러스터 `semicolon` 네임스페이스)

1. 매니페스트와 마이그레이션이 repo에 있는지 확인

```bash
# repo 루트에서
ls k8s/semicolon/services/ai
ls user/src/main/resources/db/migration/user
```

2. 커밋/푸시(이미 반영한 경우 생략)

```bash
git add user/src/main/resources/db/migration/user/V1__init.sql user/src/main/resources/application-release.yml
git commit -m "chore(user): add flyway migration V1__init.sql and enable flyway in release profile"
git push origin infra/k8s-deploy-refactor
```

3. 배포 트리거(또는 수동 적용)

- Actions를 통해 이미 빌드/푸시된 이미지를 사용하여 배포가 자동으로 진행된다면 PR/merge 후 deploy workflow를 기다리면 됩니다.

- 수동으로 적용하려면(EC2에서):
```bash
kubectl apply -f k8s/semicolon/services/user/deploy.yml -n semicolon
kubectl apply -f k8s/semicolon/services/user/service.yml -n semicolon
kubectl rollout restart deploy/user -n semicolon
kubectl rollout status deploy/user -n semicolon --timeout=180s
```

4. Flyway 로그 확인

```bash
# 가장 최근 파드 이름 조회
POD=$(kubectl -n semicolon get pods -l app=user -o jsonpath='{.items[0].metadata.name}')
# 파드 로그에서 flyway 메시지 확인
kubectl -n semicolon logs $POD -c user | grep -i flyway -n || true
kubectl -n semicolon logs $POD -c user | sed -n '1,200p'
```

5. health 확인

```bash
# 클러스터 내부에서 curl (임시 pod 사용)
kubectl -n semicolon run -it --rm --image=curlimages/curl tmp-curl -- /bin/sh -c "curl -sS http://user-service:8082/actuator/health | jq ."
```

6. 문제 발생 시
- `kubectl describe pod <pod>` 와 `kubectl logs <pod> -c user --previous` 확인
- DB 접속이 안되면 secret/환경변수(DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD)가 올바른지 확인

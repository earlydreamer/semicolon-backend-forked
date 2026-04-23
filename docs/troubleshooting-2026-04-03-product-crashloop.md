# 2026-04-03 `product` CrashLoopBackOff 트러블슈팅 로그

## 요약

- 증상: `product` pod가 `CrashLoopBackOff`로 재시작을 반복했다.
- 직접 원인: 새 pod가 현재 `semicolon-env` Secret 기준으로 재기동되면서 `AWS_ACCESS_KEY`를 찾던 예전 바이너리가 부팅에 실패했다.
- 근본 원인: 배포 파이프라인이 mutable tag인 `:latest`를 다시 적용할 수 있는 구조였고, `imagePullPolicy: IfNotPresent`와 결합되며 오래된 이미지가 재사용됐다.
- 조치: `scripts/apply.sh`가 앱 Deployment를 정적 `:latest`로 덮어쓰지 않도록 변경했고, 변경 모듈만 `${GITHUB_SHA}` 이미지로 apply 단계부터 주입하도록 `scripts/remote-deploy.sh`를 수정했다.
- 추가 보강: `workflow_dispatch`에 `rebuild_all_images` 입력을 추가해, 이미 `:latest`로 drift된 원격 Deployment를 전체 SHA 태그 이미지로 다시 고정할 수 있게 했다.
- 추가 가드: 원격 배포에서 앱 image reference가 `:latest`로 해석되면 배포를 실패시켜, 낡은 `latest` 상태를 조용히 보존하지 않게 했다.

## 증상

- `product` pod가 `CrashLoopBackOff`로 재시작을 반복했다.
- startup 단계에서 `Could not resolve placeholder 'AWS_ACCESS_KEY'` 예외로 종료됐다.
- 같은 시점 `semicolon-env` Secret에는 `AWS_ACCESS_KEY`가 없고 `OBJECT_STORAGE_*` 키만 존재했다.

## 타임라인

- 2026-03-19: `product` release 설정이 `AWS_* fallback`에서 `OBJECT_STORAGE_* only`로 변경됐다.
- 2026-03-19: GitHub Actions 배포 워크플로도 `OBJECT_STORAGE_*`를 렌더링하도록 변경됐다.
- 2026-04-03 00:18:57 KST: 현재 `product` pod가 새로 생성됐다.
- 2026-04-03: 새 pod가 `AWS_ACCESS_KEY` placeholder 해석 실패로 즉시 죽기 시작했다.

## 확인된 사실

- 현재 `product` 소스의 release 설정은 `OBJECT_STORAGE_ACCESS_KEY`, `OBJECT_STORAGE_SECRET_KEY`, `OBJECT_STORAGE_REGION`, `OBJECT_STORAGE_BUCKET`를 사용한다.
- 실제 장애 pod는 `AWS_ACCESS_KEY`를 찾고 있었으므로, 실행 중이던 컨테이너는 현재 소스보다 오래된 설정 체계의 바이너리였다.
- Deployment spec은 `dukku/semicolon-product:latest`를 참조하고 있었고, pod `imageID`는 특정 digest를 가리키고 있었다.
- `imagePullPolicy: IfNotPresent` 조합 때문에 노드에 캐시된 예전 `latest`가 재사용될 수 있었다.
- 당시 파이프라인은 `rollout-images.sh`에서만 변경 모듈에 SHA 이미지를 주입했지만, 그 전에 `scripts/apply.sh`가 서비스 Deployment를 다시 apply 하면서 정적 `:latest`를 재적용할 수 있었다.

## 왜 어제까지는 동작했는가

- Kubernetes의 `envFrom` 시크릿 값은 pod 시작 시점에만 주입된다.
- 기존 `product` pod는 예전 Secret 스냅샷을 들고 살아 있었기 때문에, Secret 구조가 바뀐 뒤에도 재시작 전까지는 정상처럼 보일 수 있었다.
- 새 pod가 생성된 시점부터는 현재 Secret 기준으로 다시 환경변수를 주입받았고, 그 순간 예전 바이너리와 현재 Secret 체계가 충돌했다.

## Root Cause

직접 원인:

- 새 `product` pod가 재생성되면서 현재 Secret 기준으로 환경변수를 다시 주입받았고, 예전 바이너리가 요구하던 `AWS_ACCESS_KEY`가 없어 부팅에 실패했다.

근본 원인:

- mutable tag인 `:latest`에 의존한 상태에서 `apply.sh`가 앱 Deployment를 다시 적용해, 이전에 SHA로 고정됐던 서비스도 후속 배포에서 `:latest`로 되감길 수 있었다.
- `:latest + IfNotPresent` 조합 때문에 새로 빌드해도 kubelet이 새 이미지를 다시 pull하지 않고, 노드 캐시에 남아 있던 오래된 `latest`를 재사용할 수 있었다.

## 조치

- `scripts/apply.sh`를 수정해 앱 Deployment를 별도로 렌더링하도록 변경했다.
- 변경 모듈은 `<MODULE>_IMAGE` env를 통해 `${GITHUB_SHA}` 기반 이미지를 사용한다.
- 미변경 모듈은 현재 클러스터의 `.spec.template.spec.containers[0].image`를 조회해 그대로 유지한다.
- `scripts/remote-deploy.sh`가 `apply.sh` 실행 전에 변경 모듈별 `<MODULE>_IMAGE` env를 export 하도록 수정했다.
- `deploy-m1-tunnel.yml` 수동 실행에 `rebuild_all_images` 입력을 추가했다. 이 값을 `true`로 실행하면 전체 앱 이미지를 현재 커밋 SHA로 빌드하고 모든 앱 Deployment를 SHA 태그로 롤아웃한다.
- `remote-deploy.sh`는 `apply.sh`에 `REJECT_APP_LATEST_IMAGE=true`를 넘긴다. 따라서 원격 배포 중 `<MODULE>_IMAGE`나 기존 Deployment image가 `:latest`로 해석되면 즉시 실패한다.

## 즉시 복구 절차

1. GitHub Actions에서 `Deploy to M1 via Tunnel SSH` 워크플로를 수동 실행한다.
2. `rebuild_all_images`를 `true`로 지정한다.
3. 완료 후 원격 host에서 앱 Deployment image가 `:latest`가 아닌 SHA 태그인지 확인한다.
4. `product` 같은 장애 서비스는 rollout 상태와 로그를 같이 확인한다.

## 재발 방지 규칙

- 앱 Deployment apply는 mutable `:latest`를 신뢰하지 않는다.
- 새 배포의 이미지 불변성은 `${GITHUB_SHA}` 태그로 보장한다.
- infra-only 배포나 다른 모듈 배포가 비대상 앱의 image reference를 바꾸면 안 된다.
- 원격 Deployment가 이미 `:latest`를 들고 있으면 보존 로직도 낡은 상태를 보존하므로, `workflow_dispatch` + `rebuild_all_images=true`로 SHA 태그 기준선을 먼저 만든다.
- 원격 배포에서 `:latest` 가드가 실패하면 같은 경로를 반복하지 말고 전체 rebuild 옵션으로 기준선을 복구한다.
- 신규 클러스터 bootstrap이나 Deployment 유실 복구 같은 경로도 장기적으로는 `:latest` fallback 없이 불변 이미지 참조를 받도록 보강해야 한다.

## 확인 명령

```bash
kubectl get deploy product -n semicolon -o jsonpath='{.spec.template.spec.containers[0].image}'; echo
kubectl get deploy -n semicolon -o jsonpath='{range .items[*]}{.metadata.name}{"\t"}{.spec.template.spec.containers[0].image}{"\n"}{end}'
kubectl get pod -n semicolon -l app=product -o jsonpath='{.items[0].status.containerStatuses[0].imageID}'; echo
kubectl get pod product-7577f95799-k4zkk -n semicolon -o jsonpath='{.metadata.creationTimestamp}'; echo
kubectl logs -n semicolon deploy/product --tail=200
kubectl get secret semicolon-env -n semicolon -o json | jq -r '.data | keys[]'
```

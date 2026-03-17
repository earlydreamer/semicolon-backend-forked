apiVersion: v1
kind: ServiceAccount
metadata:
  name: showcase-resetter
  namespace: ${SHOWCASE_RESET_NAMESPACE}

---
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: showcase-resetter
  namespace: ${SHOWCASE_RESET_NAMESPACE}
rules:
  - apiGroups: ["apps"]
    resources: ["deployments", "deployments/scale"]
    verbs: ["get", "list", "watch", "patch", "update"]
  - apiGroups: [""]
    resources: ["pods"]
    verbs: ["get", "list", "watch"]
  - apiGroups: [""]
    resources: ["pods/exec"]
    verbs: ["create"]

---
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: showcase-resetter
  namespace: ${SHOWCASE_RESET_NAMESPACE}
subjects:
  - kind: ServiceAccount
    name: showcase-resetter
    namespace: ${SHOWCASE_RESET_NAMESPACE}
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: showcase-resetter

---
apiVersion: v1
kind: ConfigMap
metadata:
  name: showcase-reset-scripts
  namespace: ${SHOWCASE_RESET_NAMESPACE}
data:
  run-showcase-reset.sh: |
    #!/bin/sh
    set -eu

    NS="${SHOWCASE_RESET_NAMESPACE}"
    APPS="auth user product order payment coupon deposit settlement ai"
    TIMEOUT_SECONDS="${SHOWCASE_RESET_TIMEOUT_SECONDS}"
    APPS_SCALED_DOWN="false"

    log() {
      echo "[$(date -Iseconds)] $*"
    }

    restore_apps() {
      if [ "$APPS_SCALED_DOWN" = "true" ]; then
        log "Restoring app replicas to 1 after reset attempt"
        scale_apps 1 || true
      fi
    }

    deployment_exists() {
      kubectl -n "$NS" get deployment "$1" >/dev/null 2>&1
    }

    wait_for_pods_gone() {
      app="$1"
      deadline=$(( $(date +%s) + TIMEOUT_SECONDS ))

      while :; do
        pod_names="$(kubectl -n "$NS" get pods -l app="$app" -o jsonpath='{.items[*].metadata.name}')"
        if [ -z "$pod_names" ]; then
          return 0
        fi

        if [ "$(date +%s)" -ge "$deadline" ]; then
          log "Timed out waiting for $app pods to terminate."
          kubectl -n "$NS" get pods -l app="$app" || true
          exit 1
        fi

        sleep 5
      done
    }

    wait_for_rollout() {
      app="$1"
      kubectl -n "$NS" rollout status deployment/"$app" --timeout="$TIMEOUT_SECONDS"s
    }

    get_first_pod() {
      selector="$1"
      kubectl -n "$NS" get pods -l "$selector" -o jsonpath='{.items[0].metadata.name}'
    }

    scale_apps() {
      replicas="$1"
      for app in $APPS; do
        if deployment_exists "$app"; then
          log "Scaling $app to $replicas"
          kubectl -n "$NS" scale deployment "$app" --replicas="$replicas"
        fi
      done
    }

    flush_redis() {
      redis_pod="$(get_first_pod app=redis)"
      if [ -z "$redis_pod" ]; then
        log "Redis pod not found. Skipping flush."
        return 0
      fi

      log "Flushing Redis state"
      kubectl -n "$NS" exec "$redis_pod" -- sh -lc '
        if [ -n "$REDIS_PASSWORD" ]; then
          redis-cli -a "$REDIS_PASSWORD" FLUSHALL
        else
          redis-cli FLUSHALL
        fi
      '
    }

    trap restore_apps EXIT

    log "Showcase reset started"
    scale_apps 0
    APPS_SCALED_DOWN="true"

    for app in $APPS; do
      if deployment_exists "$app"; then
        wait_for_pods_gone "$app"
      fi
    done

    wait_for_rollout postgres

    postgres_pod="$(get_first_pod app=postgres)"
    if [ -z "$postgres_pod" ]; then
      log "Postgres pod not found."
      exit 1
    fi

    log "Resetting PostgreSQL showcase databases"
    kubectl -n "$NS" exec "$postgres_pod" -- sh /opt/postgres/reset-databases.sh

    flush_redis

    scale_apps 1

    for app in $APPS; do
      if deployment_exists "$app"; then
        wait_for_rollout "$app"
      fi
    done

    APPS_SCALED_DOWN="false"
    log "Showcase reset completed"

---
apiVersion: batch/v1
kind: CronJob
metadata:
  name: showcase-db-reset
  namespace: ${SHOWCASE_RESET_NAMESPACE}
spec:
  schedule: "${SHOWCASE_RESET_CRON}"
  timeZone: "${SHOWCASE_RESET_TIMEZONE}"
  suspend: ${SHOWCASE_RESET_SUSPEND}
  concurrencyPolicy: Forbid
  successfulJobsHistoryLimit: 1
  failedJobsHistoryLimit: 3
  jobTemplate:
    spec:
      backoffLimit: 1
      template:
        spec:
          serviceAccountName: showcase-resetter
          restartPolicy: Never
          containers:
            - name: showcase-reset
              image: bitnami/kubectl:latest
              imagePullPolicy: IfNotPresent
              command:
                - /bin/sh
                - /scripts/run-showcase-reset.sh
              volumeMounts:
                - name: scripts
                  mountPath: /scripts
                  readOnly: true
          volumes:
            - name: scripts
              configMap:
                name: showcase-reset-scripts
                defaultMode: 0755

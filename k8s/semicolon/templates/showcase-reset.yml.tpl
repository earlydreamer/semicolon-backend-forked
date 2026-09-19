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
  - apiGroups: ["batch"]
    resources: ["jobs"]
    verbs: ["create", "get", "delete"]

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
    PRODUCT_CLEAR_FLAG_SET="false"

    log() {
      echo "[$(date -Iseconds)] $*"
    }

    restore_apps() {
      if [ "$APPS_SCALED_DOWN" = "true" ]; then
        log "Restoring app replicas to 1 after reset attempt"
        scale_apps 1 || true
      fi
    }

    restore_product_clear_flag() {
      if [ "$PRODUCT_CLEAR_FLAG_SET" = "true" ] && deployment_exists product; then
        log "Restoring product storage clear flag after reset attempt"
        kubectl -n "$NS" set env deployment/product PRODUCT_INIT_CLEAR_ES_ON_STARTUP- || true
      fi
    }

    cleanup_after_reset_attempt() {
      restore_product_clear_flag
      restore_apps
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
      kubectl -n "$NS" get pods -l "$selector" -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true
    }

    summary_field_is() {
      summary_text="$1"
      field_name="$2"
      expected_value="$3"
      case " $summary_text " in
        *" $field_name=$expected_value "*) return 0 ;;
        *) return 1 ;;
      esac
    }

    ai_memory_summary_is_complete() {
      summary_text="$1"
      case " $summary_text " in
        *" table=ai_user_memory "*) ;;
        *) return 1 ;;
      esac

      summary_field_is "$summary_text" present true \
        && summary_field_is "$summary_text" scanned 7 \
        && summary_field_is "$summary_text" candidates 7 \
        && summary_field_is "$summary_text" updated 7 \
        && summary_field_is "$summary_text" conflicts 0 \
        && summary_field_is "$summary_text" failures 0
    }

    summarize_ai_backfill_job() {
      job_name="$1"
      pod_name="$(kubectl -n "$NS" get pods -l showcase-ai-memory-backfill-job="$job_name" -o jsonpath='{.items[0].metadata.name}' 2>/dev/null || true)"
      if [ -z "$pod_name" ]; then
        log "AI embedding Job pod is not available for summary: $job_name"
        return 1
      fi

      job_pod_summary="$(kubectl -n "$NS" get pod "$pod_name" -o jsonpath='{.status.containerStatuses[0].state.terminated.message}' 2>/dev/null || true)"
      if [ -n "$job_pod_summary" ]; then
        log "AI embedding Job summary: $job_pod_summary"
        return 0
      fi

      job_pod_phase="$(kubectl -n "$NS" get pod "$pod_name" -o jsonpath='{.status.phase}' 2>/dev/null || true)"
      job_pod_reason="$(kubectl -n "$NS" get pod "$pod_name" -o jsonpath='{.status.containerStatuses[0].state.terminated.reason}' 2>/dev/null || true)"
      if [ -z "$job_pod_reason" ]; then
        job_pod_reason="$(kubectl -n "$NS" get pod "$pod_name" -o jsonpath='{.status.containerStatuses[0].state.waiting.reason}' 2>/dev/null || true)"
      fi
      if [ -z "$job_pod_reason" ]; then
        job_pod_reason="container did not report a termination reason"
      fi
      log "AI embedding Job summary unavailable (phase=$job_pod_phase reason=$job_pod_reason)"
      return 1
    }

    wait_for_ai_backfill_job() {
      job_name="$1"
      deadline=$(( $(date +%s) + 600 ))

      while :; do
        if ! job_status="$(kubectl -n "$NS" get job "$job_name" -o jsonpath='{.status.succeeded} {.status.failed}' 2>/dev/null)"; then
          return 1
        fi

        succeeded_pods="${job_status%% *}"
        failed_pods="${job_status#* }"
        if [ "${succeeded_pods:-0}" -gt 0 ]; then
          return 0
        fi

        if [ "${failed_pods:-0}" -gt 0 ]; then
          return 1
        fi

        if [ "$(date +%s)" -ge "$deadline" ]; then
          return 1
        fi
        sleep 5
      done
    }

    run_ai_memory_backfill() {
      ai_image="$(kubectl -n "$NS" get deployment ai -o jsonpath='{.spec.template.spec.containers[?(@.name=="ai")].image}')"
      if [ -z "$ai_image" ]; then
        log "AI deployment image not found; cannot run sample memory embedding Job."
        return 1
      fi

      pod_suffix="${HOSTNAME:-}"
      pod_suffix="${pod_suffix##*-}"
      case "$pod_suffix" in
        ''|*[!a-z0-9-]*) pod_suffix="$$" ;;
      esac
      job_name="showcase-ai-memory-backfill-$(date +%s)-$pod_suffix"
      log "Creating AI sample memory embedding Job from the current AI deployment image."
      kubectl -n "$NS" apply -f - <<EOF
    apiVersion: batch/v1
    kind: Job
    metadata:
      name: $job_name
      namespace: $NS
      labels:
        app.kubernetes.io/name: showcase-ai-memory-backfill
    spec:
      backoffLimit: 0
      activeDeadlineSeconds: 540
      ttlSecondsAfterFinished: 604800
      template:
        metadata:
          labels:
            app.kubernetes.io/name: showcase-ai-memory-backfill
            showcase-ai-memory-backfill-job: $job_name
        spec:
          automountServiceAccountToken: false
          restartPolicy: Never
          terminationGracePeriodSeconds: 15
          containers:
            - name: embedding-backfill
              image: "$ai_image"
              imagePullPolicy: IfNotPresent
              command: ["/bin/sh", "-c"]
              args:
                - |
                  output_file="/tmp/ai-memory-backfill-output"
                  if java -Dloader.main=dukku.ai.app.migration.GeminiEmbeddingMigrationCli -cp /app/app.jar org.springframework.boot.loader.launch.PropertiesLauncher --apply --batch-size 7 --max-rows 7 >"\$output_file" 2>&1; then
                    cli_status=0
                  else
                    cli_status=\$?
                  fi
                  summary=""
                  cli_reason=""
                  while IFS= read -r line; do
                    case "\$line" in
                      "table=ai_user_memory "*) summary="\$line" ;;
                      "--apply requires the existing GEMINI_API_KEY environment variable."|"Invalid EMBEDDING_MODEL configuration for the nullable 160-character profile."|"Required tables or 1536-dimensional profile schema are missing; initialize both tables and apply sql/manual/20260919_gemini_embedding_profile.sql first."|"Invalid arguments. Use --help to see supported options.")
                        if [ -z "\$cli_reason" ]; then cli_reason="\$line"; fi
                        ;;
                      "Migration stopped before completion ("*)
                        safe_type="\${line#Migration stopped before completion (}"
                        case "\$safe_type" in
                          *").")
                            safe_type="\${safe_type%.}"
                            safe_type="\${safe_type%)}"
                            case "\$safe_type" in
                              ''|*[!A-Za-z0-9]*) ;;
                              *)
                                if [ -z "\$cli_reason" ]; then
                                  cli_reason="Migration stopped before completion (\$safe_type)."
                                fi
                                ;;
                            esac
                            ;;
                        esac
                        ;;
                    esac
                  done < "\$output_file"
                  if [ -n "\$summary" ]; then
                    if [ -n "\$cli_reason" ]; then
                      printf '%s | %s\n' "\$summary" "\$cli_reason" > /dev/termination-log
                    else
                      printf '%s\n' "\$summary" > /dev/termination-log
                    fi
                  elif [ -n "\$cli_reason" ]; then
                    printf '%s\n' "\$cli_reason" > /dev/termination-log
                  else
                    printf 'ai_user_memory summary=missing cli_exit_code=%s\n' "\$cli_status" > /dev/termination-log
                  fi
                  exit "\$cli_status"
              envFrom:
                - secretRef:
                    name: semicolon-env
                - secretRef:
                    name: ai-db
              env:
                - name: JAVA_TOOL_OPTIONS
                  value: "-Xms64m -Xmx256m"
              resources:
                requests:
                  cpu: 100m
                  memory: 256Mi
                limits:
                  cpu: 500m
                  memory: 512Mi
              terminationMessagePath: /dev/termination-log
              terminationMessagePolicy: File
    EOF

      if ! wait_for_ai_backfill_job "$job_name"; then
        log "AI embedding Job failed or timed out; preserving it for inspection: $job_name"
        summarize_ai_backfill_job "$job_name" || true
        return 1
      fi

      if ! summarize_ai_backfill_job "$job_name"; then
        log "AI embedding Job completed without a readable AI memory summary; preserving it: $job_name"
        return 1
      fi

      if ! ai_memory_summary_is_complete "$job_pod_summary"; then
        log "AI embedding Job did not confirm all 7 sample memories with zero conflicts and failures; preserving it: $job_name"
        return 1
      fi

      log "AI sample memory embedding completed; trying to delete successful Job $job_name"
      if ! kubectl -n "$NS" delete job "$job_name" --wait=false; then
        log "Warning: could not delete successful AI memory backfill Job; TTL cleanup will remove it."
      fi
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

    trap cleanup_after_reset_attempt EXIT

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

    log "Enabling storage clear on product startup"
    if deployment_exists product; then
      kubectl -n "$NS" set env deployment/product PRODUCT_INIT_CLEAR_ES_ON_STARTUP=true
      PRODUCT_CLEAR_FLAG_SET="true"
    else
      log "Product deployment not found. Skipping product storage clear flag."
    fi

    scale_apps 1

    for app in $APPS; do
      if deployment_exists "$app"; then
        wait_for_rollout "$app"
      fi
    done

    log "Restoring product storage clear flag"
    restore_product_clear_flag
    run_ai_memory_backfill
    PRODUCT_CLEAR_FLAG_SET="false"

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

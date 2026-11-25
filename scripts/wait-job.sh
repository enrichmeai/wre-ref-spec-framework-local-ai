#!/usr/bin/env bash
# Wait for a Job to succeed or fail, whichever comes first, then print its log.
# (kubectl wait --for=condition=complete never returns for a Job that failed.)
set -euo pipefail
NS=$1; JOB=$2; TIMEOUT=${3:-1200}
for _ in $(seq 1 $((TIMEOUT / 5))); do
  ok=$(kubectl -n "$NS" get job "$JOB" -o jsonpath='{.status.succeeded}' 2>/dev/null || true)
  bad=$(kubectl -n "$NS" get job "$JOB" -o jsonpath='{.status.failed}' 2>/dev/null || true)
  if [ "${ok:-0}" -ge 1 ] || [ "${bad:-0}" -ge 1 ]; then
    kubectl -n "$NS" logs "job/$JOB"
    [ "${ok:-0}" -ge 1 ]; exit $?
  fi
  sleep 5
done
echo "timed out after ${TIMEOUT}s" >&2; exit 2

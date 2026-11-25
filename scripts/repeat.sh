#!/usr/bin/env bash
# Same spec, N times: how often does it pass, and how many different outputs come back?
# Usage: scripts/repeat.sh <label> <runs> <temperature> [fixed-seed|vary-seed]
# One warm-up request first: we saw the first request after the model loads give a different file
# from the warm ones (docs/results.md), so cold and warm are measured separately.
# With a local model this costs CPU time, not API tokens; the point is to measure once, not to do it every time.
set -euo pipefail
LABEL=$1; N=$2; TEMP=$3; SEEDS=${4:-fixed-seed}
URL=${WRE_OLLAMA_URL:-http://localhost:11434}
curl -fsS "$URL/api/chat" -d '{"model":"local-model","messages":[{"role":"user","content":"hi"}],"stream":false,"think":false}' >/dev/null \
  || { echo "NOT RUN: the model at $URL did not answer"; exit 2; }
for i in $(seq 1 "$N"); do
  seed=1; [ "$SEEDS" = vary-seed ] && seed=$i
  python3 -c "import shutil,sys; shutil.rmtree(sys.argv[1], ignore_errors=True)" "runs/$LABEL-$i"   # never read a stale result
  java generator/Generator.java --ollama-url "$URL" --run-id "$LABEL-$i" --temperature "$TEMP" --seed "$seed" --attempts 1 >/dev/null 2>&1 || true
done
python3 scripts/summarise.py "$LABEL" "$N"

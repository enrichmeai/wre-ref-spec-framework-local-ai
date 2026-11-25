#!/bin/sh
# For trying a model without building an image: serve, import the mounted GGUF, then keep serving.
# docker run --rm -p 127.0.0.1:11434:11434 -v "$PWD/.work/model/<tag>:/import:ro" \
#   -v "$PWD/model-runtime/serve-mounted.sh:/serve-mounted.sh:ro" --entrypoint /bin/sh wre/ollama-cpu:0.40.18 /serve-mounted.sh
# The API has no authentication: publish it on 127.0.0.1 only.
set -e
ollama serve &
for i in $(seq 1 30); do ollama list >/dev/null 2>&1 && break; sleep 1; done
GGUF=$(ls /import/*.gguf | head -1)
printf 'FROM %s\nPARAMETER num_ctx 8192\n' "$GGUF" > /tmp/Modelfile
ollama create local-model -f /tmp/Modelfile
wait

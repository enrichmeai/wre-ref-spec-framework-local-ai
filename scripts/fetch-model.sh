#!/usr/bin/env bash
# Download the model's GGUF from Docker Hub's ai/ namespace and check it against the registry digest.
# Why Docker Hub: it serves the same open weights as a plain OCI artifact, reachable wherever Docker is.
set -euo pipefail
# sha256sum on Linux, shasum -a 256 on macOS
sha256check() { if command -v sha256sum >/dev/null; then sha256sum -c -; else shasum -a 256 -c -; fi; }
REPO=${REPO:-ai/qwen3}
TAG=${TAG:-4b-instruct-2507-q4_K_M}
# The default tag is pinned; set EXPECT_DIGEST= (empty) to unpin it. Other tags are unpinned unless you set EXPECT_DIGEST.
if [ "$TAG" = 4b-instruct-2507-q4_K_M ]; then EXPECT_DIGEST=${EXPECT_DIGEST-sha256:3605803b982cb64aead44f6c1b2ae36e3acdb41d8e46c8a94c6533bc4c67e597}; fi
OUT=${OUT:-.work/model/${TAG}}
mkdir -p "$OUT"
TOKEN=$(curl -fsS "https://auth.docker.io/token?service=registry.docker.io&scope=repository:${REPO}:pull" \
  | python3 -c 'import json,sys; print(json.load(sys.stdin)["token"])')
MANIFEST=$(curl -fsS -H "Authorization: Bearer $TOKEN" \
  -H "Accept: application/vnd.oci.image.manifest.v1+json" \
  "https://registry-1.docker.io/v2/${REPO}/manifests/${TAG}")
read -r DIGEST FILE < <(python3 -c 'import json,sys
m = json.loads(sys.argv[1])
l = next(l for l in m["layers"] if l["mediaType"].endswith("weight.v1.raw"))
print(l["digest"], l["annotations"]["org.cncf.model.filepath"])' "$MANIFEST")
# The tag can move; the digest cannot. Set EXPECT_DIGEST (the Makefile does) to refuse anything else.
if [ -n "${EXPECT_DIGEST:-}" ] && [ "$DIGEST" != "$EXPECT_DIGEST" ]; then
  echo "refusing: ${REPO}:${TAG} now points at $DIGEST, expected $EXPECT_DIGEST" >&2; exit 1
fi
FILE=$(basename "$FILE")   # the file name comes from the registry; never let it point outside OUT
echo "downloading $FILE ($DIGEST)"
curl -fsSL -H "Authorization: Bearer $TOKEN" "https://registry-1.docker.io/v2/${REPO}/blobs/${DIGEST}" -o "$OUT/$FILE"
echo "${DIGEST#sha256:}  $OUT/$FILE" | sha256check
cp model-runtime/Modelfile "$OUT/Modelfile"

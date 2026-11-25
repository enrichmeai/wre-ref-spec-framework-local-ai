#!/usr/bin/env bash
# Download an Ollama release, check its SHA-256, and keep only the binary and the CPU libraries.
set -euo pipefail
# sha256sum on Linux, shasum -a 256 on macOS
sha256check() { if command -v sha256sum >/dev/null; then sha256sum -c -; else shasum -a 256 -c -; fi; }
VERSION=${OLLAMA_VERSION:-v0.40.18}
OUT=${OUT:-.work/ollama}
case "$(uname -m)" in arm64|aarch64) DEFAULT_ARCH=arm64 ;; *) DEFAULT_ARCH=amd64 ;; esac
ARCH=${ARCH:-$DEFAULT_ARCH}   # the node's architecture: arm64 on Apple Silicon, amd64 elsewhere
mkdir -p "$OUT"
BASE="https://github.com/ollama/ollama/releases/download/${VERSION}"
curl -fsSL "$BASE/ollama-linux-${ARCH}.tar.zst" -o "$OUT/ollama.tar.zst"
curl -fsSL "$BASE/sha256sum.txt" | grep "ollama-linux-${ARCH}.tar.zst" | awk -v f="$OUT/ollama.tar.zst" '{print $1"  "f}' | sha256check
# zstd + tar rather than tar --zstd, so it also works with the tar that ships with macOS (brew install zstd)
zstd -dc "$OUT/ollama.tar.zst" | tar -x -C "$OUT" \
  --exclude='lib/ollama/cuda*' --exclude='lib/ollama/rocm*' --exclude='lib/ollama/vulkan*' \
  --exclude='lib/ollama/*mlx*' --exclude='lib/ollama/*cuda*'
rm "$OUT/ollama.tar.zst"
du -sh "$OUT"

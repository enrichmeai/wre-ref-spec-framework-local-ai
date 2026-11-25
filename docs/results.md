# What we measured

Everything here was run on 25 November 2026, in a container with 4 CPU cores and no GPU, with Ollama
v0.40.18 on CPU and the models from Docker Hub's `ai/qwen3`. Each run's code and result is in
[`evidence/`](evidence/). Times are for this machine; a laptop will differ.

**How the model was served.** The first 1.7B runs used an image built from an earlier version of
`model-runtime/Dockerfile.model` (the model baked in). All later runs, including every 4B run, served the GGUF
mounted into the CPU runtime image with `model-runtime/serve-mounted.sh`, because the 4B image build needed
more disk than this machine had (the current Dockerfile ran every step, then ran out of space exporting).

## Current spec: Qwen3 4B Instruct (2507), 4-bit

The spec in `specs/001-shipping-quote` (7 acceptance criteria), the framework and harness as committed.
Prompt 926 tokens; one attempt per run.

| Runs | Settings | Passed | Different outputs | Model time per run | Output tokens |
|---|---|---|---|---|---|
| `cold-1`, `cold-2`: each the first request after a fresh model start | temperature 0, seed 1 | 2 / 2 | 1 | 51.6 s, 46.0 s | 207 |
| `warm-t0-fixed-1..5`: after a warm-up request | temperature 0, seed 1 | 5 / 5 | 1 (the same file as the cold runs) | 29.5 to 31.3 s | 207 |
| `warm-t07-vary-1..5` | temperature 0.7, seeds 1 to 5 | 5 / 5 | 5 | 24.9 to 34.0 s | 179 to 211 |

Every passing run passed the 7 acceptance tests and the platform rules (`mvn clean verify`, about 10 s).

What this does and does not show:
- At temperature 0 with a fixed seed, all seven runs gave the same file on this machine. That is one machine,
  one model build, one runtime version; we did not test across hardware or versions.
- At temperature 0.7 the five runs gave five different files, one of them (seed 4) the same as the
  temperature-0 file: 5 distinct files across all 12 runs. All five passed. Different code is not wrong code:
  **the tests decide, not the diff.**

## Earlier spec (6 acceptance criteria): the path to a passing run

These runs used the spec before AC-7 and a small wording change ([`evidence/earlier-spec/`](evidence/earlier-spec/)
and the `run-*` folders). Each row changed one thing.

| Run | What changed | Result | What it taught |
|---|---|---|---|
| [`run-before-errors-helper`](evidence/run-before-errors-helper/) | The framework offered only an abstract `DomainException` (1.7B) | 3 attempts, all failed to compile: `new DomainException(...)`, no `@Service` | An abstract class with a protected constructor is easy for people and hard for a small model |
| [`run-after-errors-helper`](evidence/run-after-errors-helper/) | Framework gained `Errors.notFound` / `Errors.unprocessable` | Failed: invented `Errors.unknownZone` and a `model` package | A friendlier API helps, but a whole file still leaves room to guess |
| [`run-slot-body-signature-repeated`](evidence/run-slot-body-signature-repeated/) | The model fills only the method body; the template holds the rest | Failed: the model repeated the method signature inside the slot | The harness must tolerate what models do, not only what they are told |
| [`run-stale-classes-and-feedback`](evidence/run-stale-classes-and-feedback/) | Harness takes the body out of a repeated method | Failed: the platform rules test and one weight criterion (attempts 1-2 AC-5, attempt 3 AC-4, inferred from identical files in the next rows) | We first blamed classes left over from an earlier build and added `clean`; the next row shows the real cause |
| [`run-empty-rule`](evidence/run-empty-rule/) | `mvn clean verify`; failing tests fed back by name | Failed: an architecture rule with nothing to check failed the build, and AC-5 | **Framework bug**, found by generated code that declared no exception classes |
| [`run-spec-ambiguous-weight`](evidence/run-spec-ambiguous-weight/) | Rules allow an empty check | One failure left: weight 0 got `WEIGHT_LIMIT` instead of `INVALID_WEIGHT` | **Spec gap**: the error rules were only in the acceptance criteria |
| [`run-1.7b-cannot-repair`](evidence/run-1.7b-cannot-repair/) | Spec states each error and its order | Weight rules right; other mistakes (`Quote.builder()`), repeated on every attempt | This model does not use the feedback |
| [`earlier-spec/4b-run1`](evidence/earlier-spec/4b-run1/) | Qwen3 4B Instruct (2507) | **Passed on the first attempt**, 39.6 s model + 9.1 s build | Model size matters once the framework and spec are clear |
| [`earlier-spec/container-check`](evidence/earlier-spec/container-check/) | `Generator.java` run inside the base image `maven:3.10.2-eclipse-temurin-21`, Maven offline | Passed on the first attempt, 50.2 s + 8.6 s | The generator runs in the JDK image the generator Dockerfile starts from (the Dockerfile itself was not built) |

Repeated runs with the earlier spec, one attempt each:

| Model | Settings | Passed | Different outputs |
|---|---|---|---|
| 4B | temperature 0, seed 1, five runs, after `earlier-spec/4b-run1` and `container-check` (both cold) | 5 / 5 | 1, but a **different file from the two cold runs** (7 runs, 2 files) |
| 4B | temperature 0.7, seeds 1 to 5 | 5 / 5 | 5 (seed 4 gave the same file as the cold runs) |
| 1.7B | temperature 0, seed 1 (first run cold) | 0 / 5 | 2: the first run differs from runs 2 to 5 |
| 1.7B | temperature 0.7, seeds 1 to 5 | 1 / 5 | 5 |

With the earlier spec, the first request after the model loaded gave a different file from the warm ones,
for both models. With the current spec it did not (table above). We did not investigate why; `make repeat`
now sends a warm-up request first and the cold runs are measured separately. Across all its single attempts
the 1.7B model passed 1 of 10; with three repair attempts it also failed (`earlier-spec/small-with-repair`).

## Not run here

- **minikube.** This environment's sandbox does not let containers inside containers start
  (`failed to update /proc/self/oom_score_adj: Permission denied` for every pod), so `make minikube-up` and
  `make generate` were not run.
- **`generator/Dockerfile`** was not built: its `RUN mvn` steps need Maven Central, which this environment
  reaches only through a proxy the build cannot use. The generator was run in its base image instead.
- **The current `model-runtime/Dockerfile.model`** with the 4B model: every step ran, then the export ran out
  of disk. An earlier version of it built and served the 1.7B model.
- **Schema validation of the manifests** (kubeconform): not run. They render with `kubectl kustomize`.
- **The GPU overlay** (`k8s/gpu`): rendered, not run; there was no GPU.
- **macOS and Apple Silicon**: not run. The arm64 Ollama build exists in the release's checksums; nothing
  more was checked.

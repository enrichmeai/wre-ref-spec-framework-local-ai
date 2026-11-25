# From a laptop to a GPU cluster

This repository runs the model on CPU so anyone can run it. Here is what changes for real use.

| | Laptop (this repo) | GPU node |
|---|---|---|
| Runtime image | `wre/ollama-cpu`: Ollama's binary and CPU libraries, about 168 MB of layers (amd64; `docker images` showed 227 MB here, counting the compressed copies too) | The official `ollama/ollama` image: a 3.2 GiB compressed download for amd64, carrying the CUDA, Vulkan and MLX libraries; ROCm is its `-rocm` tag |
| Model image | `model-runtime/Dockerfile.model` with `RUNTIME=wre/ollama-cpu:...` | The same Dockerfile with `RUNTIME=ollama/ollama:0.40.18` |
| Scheduling | CPU and memory requests | Add `nvidia.com/gpu: 1` (or your vendor's resource) under **limits**; Kubernetes uses the limit as the request |
| Node set-up | none | The vendor's drivers and device plugin on the node |
| Manifests | `k8s/base` | `k8s/gpu` (a kustomize overlay on the base) |

Kubernetes' own guide: <https://kubernetes.io/docs/tasks/manage-gpus/scheduling-gpus/>. It says GPUs are
"only supposed to be specified in the limits section", and that the drivers and device plugin are the
administrator's job.

**Not run here**: there was no GPU. The overlay renders with `kubectl kustomize k8s/gpu`.

## Why bake the model into an image

*Our view:* a pod that downloads several gigabytes on start is slow to start, depends on the network at the worst
moment, and may get a different file next time. Baking the model in at build time means:
- the image digest pins the exact weights;
- a pod starts with no network;
- rolling back the model is rolling back an image.

*The other view:* very large models make images unwieldy; teams then keep weights on a shared volume or
object store, loaded by an init container, and pin them by checksum instead.

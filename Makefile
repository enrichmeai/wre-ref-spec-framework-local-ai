# The whole example, from an empty laptop to a generated, tested service in minikube.
# Needs: Docker, minikube, kubectl, curl, zstd, python3, and (an estimate) 15 GB free disk: the model image
# build used about 10 GB of builder space here before running out. Not yet run end to end (see README). Java 21 and Maven only for the targets that skip Kubernetes.

OLLAMA_VERSION ?= v0.40.18
MODEL_TAG      ?= 4b-instruct-2507-q4_K_M
MODEL_GGUF     ?= Qwen3-4B-Instruct-2507-Q4_K_M.gguf
# The tag on Docker Hub can move; this digest cannot. fetch refuses any other file.
MODEL_DIGEST   ?= sha256:3605803b982cb64aead44f6c1b2ae36e3acdb41d8e46c8a94c6533bc4c67e597
RUNTIME_IMAGE  ?= wre/ollama-cpu:$(OLLAMA_VERSION:v%=%)
MODEL_IMAGE    ?= wre/model:4b
GEN_IMAGE      ?= wre/generator:0.1.0
NS             ?= wre-local-ai
SLOT           := services/shipping-quote/src/main/java/com/enrichmeai/ref/shipping/ShippingQuoteService.java

.PHONY: fetch images minikube-up generate logs down framework reference-check local-model local-model-mounted stop-model generate-local repeat

## 1. Download the runtime and the model, each checked against its published SHA-256.
fetch:
	OLLAMA_VERSION=$(OLLAMA_VERSION) scripts/fetch-ollama.sh
	TAG=$(MODEL_TAG) EXPECT_DIGEST=$(MODEL_DIGEST) scripts/fetch-model.sh

## 2. Build three images: the CPU runtime, the runtime with the model baked in, and the generator.
images:
	docker build -f model-runtime/Dockerfile.runtime -t $(RUNTIME_IMAGE) .work/ollama
	docker build -f model-runtime/Dockerfile.model --build-arg RUNTIME=$(RUNTIME_IMAGE) \
	  --build-arg GGUF=$(MODEL_GGUF) -t $(MODEL_IMAGE) .work/model/$(MODEL_TAG)
	docker build -f generator/Dockerfile -t $(GEN_IMAGE) .

## 3. Start minikube, load the images, deploy the model and wait until it is ready.
minikube-up:
	minikube start --cpus=4 --memory=8g
	minikube image load $(MODEL_IMAGE)
	minikube image load $(GEN_IMAGE)
	kubectl apply -k k8s/base
	kubectl -n $(NS) rollout status deployment/model --timeout=10m

## 4. One generation run in the cluster. The Job succeeds only if the generated service passes its tests.
generate:
	kubectl -n $(NS) delete job generate-shipping-quote --ignore-not-found
	kubectl apply -f k8s/base/generator-job.yaml
	scripts/wait-job.sh $(NS) generate-shipping-quote 1200

logs:
	kubectl -n $(NS) logs job/generate-shipping-quote

down:
	minikube delete

## Without Kubernetes: the framework, the known-good slot, the model in Docker, the generator on your machine.
framework:
	mvn -q -B -f framework/pom.xml install

## Proves the acceptance tests can pass: builds the service with the hand-written slot, then takes it out again.
reference-check: framework
	cp generator/reference/ShippingQuoteService.java $(SLOT)
	mvn -q -B -f services/shipping-quote/pom.xml clean verify; status=$$?; rm -f $(SLOT); exit $$status

local-model:
	docker run -d --rm --name wre-model -p 127.0.0.1:11434:11434 $(MODEL_IMAGE)

## The route every measured result used: the CPU runtime with the GGUF mounted, no model image to build.
local-model-mounted:
	docker build -f model-runtime/Dockerfile.runtime -t $(RUNTIME_IMAGE) .work/ollama
	docker run -d --rm --name wre-model -p 127.0.0.1:11434:11434 -v "$(CURDIR)/.work/model/$(MODEL_TAG):/import:ro" \
	  -v "$(CURDIR)/model-runtime/serve-mounted.sh:/serve-mounted.sh:ro" --entrypoint /bin/sh $(RUNTIME_IMAGE) /serve-mounted.sh

stop-model:
	docker rm -f wre-model

generate-local: framework
	java generator/Generator.java --run-id local-$$(date +%s)

## Same spec, five times each way: pass rate and how many different outputs come back.
repeat: framework
	scripts/repeat.sh warm-t0-fixed 5 0 fixed-seed
	scripts/repeat.sh warm-t07-vary 5 0.7 vary-seed

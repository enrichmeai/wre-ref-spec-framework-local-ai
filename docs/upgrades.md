# Upgrades: the part to automate completely

*Our view:* a library upgrade needs no spec and little judgement, so it suits automation. *The other view:* a
major upgrade can change behaviour, so those still need a person to read the release notes. Two pieces do it here.

**1. One place decides versions.** `framework/platform-parent/pom.xml` is the company's Maven template.
It names the Spring Boot version, the framework's own version and the build plugins. Every service
inherits it (`services/shipping-quote/pom.xml` has `platform-parent` as its parent). Upgrading Spring Boot
for every service is one change to that file, then each service bumps its parent version.

**2. A bot opens the change.** `.github/dependabot.yml` asks Dependabot to check `platform-parent`, the Dockerfiles'
base images and the GitHub Actions versions monthly and open a pull request. CI then builds the framework and runs the known-good
slot against the acceptance tests and the platform rules. If that is green, a person merges. Patch updates can merge without a
person once auto-merge is enabled and CI is a required check; this repository does not set that up (how:
<https://enrichmeai.com/well-rounded-engineer/guardrails/>).

What a bot cannot do here: a service's parent version that points at an internal framework artefact is not on
Maven Central, so Dependabot does not see it. Teams handle that with their own artefact registry
(Dependabot supports private registries) or a small script that bumps every service's parent.

For code changes an upgrade needs (renamed APIs), OpenRewrite's recipes rewrite source automatically;
see <https://github.com/openrewrite/rewrite-spring>. **Not run here.**

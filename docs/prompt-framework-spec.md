# Prompt, framework or spec: where each fits

## Three ways to get code from AI

- **Prompting**: a person asks for code now, reads it, keeps it or not. Nothing about the request is kept.
- **Spec-driven generation**: a written spec, with numbered acceptance criteria, is the input. It is still
  generated code; the difference from prompting is that the request is written down, reviewed and testable.
- **A framework**: code written once, by people, used by every service. Nothing is generated; it is shared.

## The factory question

*Our view:* spec-driven generation pays most where work looks like a factory: many copies of the same pattern
(the same kind of report, the same kind of CRUD screen, the same pipeline step). One good spec template, reused
many times, earns back its cost.

*Our view:* most products are not like that. A domain has a handful of services, each talking to different systems over
different messages. A spec for one does not copy to the next, and writing a careful spec takes about as long
as prompting. What *is* the same across them is the plumbing: errors, logging, health, security, the build,
the deployment, the pipeline. That belongs in a framework the company builds once.

So the balance, as we see it:

| If this is true for the work | Leans towards |
|---|---|
| The same pattern is produced many times | A spec template, reused; generation earns its keep |
| It is cross-cutting (errors, logging, security, build, deploy) | A framework or template, written once by people |
| It is one service's own logic, behind a stable contract | A short spec with acceptance tests, written against the framework; generate only the slot |
| It is a contract other services depend on (an API, an event) | A careful spec, agreed by both sides |
| It is a library or framework upgrade | No spec, little judgement: automate it (Dependabot, a parent POM bump) |
| Nobody knows yet what users need | Prompt, try, learn; spec what you keep |

*The other view:* teams that generate whole services from detailed specs argue that the spec becomes the
lasting record and the code can be regenerated. That suits teams with many similar services. This repository
does not measure that trade-off; it shows one service.

## Is running the same spec ten times useful?

To *learn* how a model behaves, measuring once is useful, and this repository does it (`make repeat`):
with the 4B model and the current spec, seven runs at temperature 0 gave the same file; at temperature 0.7,
five runs gave five different files (one the same as the temperature-0 file), all of which passed. With an earlier spec, the first request after the
model loaded gave a different file from the warm ones ([results](results.md)). *Our view:* to *build
software*, repeating is waste. The question is never "do I get
the same output?" but "does this output pass the tests?", and that is answered once per change.

With a hosted model, each repeat costs tokens. With a local model, as here, it costs CPU time (about 25 to 35 s per
run on 4 cores), which is why the experiment is cheap to repeat here.

## What building this taught

Every failure in [results.md](results.md) was fixed somewhere other than the prompt:

1. **The framework's API**: an abstract exception confused a small model; factory methods (`Errors.notFound`) did not.
2. **The slot's size**: asking for one method body, inside a template that holds everything else, left less room to guess.
3. **The spec**: one rule stated only in an acceptance criterion was misread; stating the rules plainly fixed it.
4. **The harness**: building from clean, and feeding back failing tests by name.
5. **The framework itself**: an architecture rule failed on a service with nothing to check, a bug generated code exposed.
6. **The model's size**: once all of the above was right, a 4B model passed first time; a 1.7B model passed 1 of 10 single attempts.

*Our view:* that order matters. A bigger model hides weak frameworks and vague specs; a small one shows them.
*The other view:* a bigger hosted model would have passed sooner, and for a one-off service that is cheaper than
fixing the framework; the fixes pay off when many services and many runs share them.

"""Summarise repeated runs: pass rate, distinct outputs, time and tokens. A run that could not reach the model,
or left no result, is counted as not run, never as a failure or a pass."""
import json, pathlib, sys

label, n = sys.argv[1], int(sys.argv[2])
ran, not_run = [], 0
for i in range(1, n + 1):
    f = pathlib.Path(f"runs/{label}-{i}/result.json")
    if not f.exists():
        not_run += 1
        continue
    r = json.loads(f.read_text())
    if r.get("exit") == 2:
        not_run += 1
        continue
    a = r["attempts"][0]
    ran.append((r["passed"], a["sha256"], a["modelSeconds"], a["outputTokens"]))
if not ran:
    print(f"{label}: NOT RUN ({not_run} of {n} runs could not reach the model or left no result)")
    sys.exit(2)
passed = sum(1 for p, *_ in ran if p)
print(f"{label}: {passed}/{len(ran)} passed, {len({h for _, h, *_ in ran})} distinct output(s), "
      f"model time {min(r[2] for r in ran)}-{max(r[2] for r in ran)} s, "
      f"output tokens {min(r[3] for r in ran)}-{max(r[3] for r in ran)}"
      + (f", {not_run} not run" if not_run else ""))

# Allure Agent Mode

Use Allure agent-mode to design, review, validate, debug, and enrich tests in this project.

## Project Bootstrap

- The root test suite already includes `io.qameta.allure:allure-junit5` in `test` scope.
- Test-side Allure results are configured through `src/test/resources/allure.properties`.
- The default results directory for local test runs is `target/allure-results`.
- For fast feedback on the unit suite, prefer `./mvnw -q test`.
- For targeted verifier ITs, prefer `./mvnw -q -Dmaven.repo.local=target/local-repo -Dit.test=<VerifierIT> verify`.
- For full lifecycle checks, use `./mvnw -q -Dmaven.repo.local=target/local-repo verify`.

## Review Principle

Runtime first, source second.

- If a command executes tests and its result will be used for smoke checking, reasoning, review, coverage analysis, debugging, or any user-facing conclusion, run it through `allure agent`. It preserves the original console logs and adds agent-mode artifacts without inheriting the normal report or export plugins from the project config.
- Use `ALLURE_AGENT_*` with `allure run` only as the lower-level fallback when you need direct environment control.
- If the agent-mode output is missing or incomplete, debug that first and treat console-only conclusions as provisional.

## Verification Standard

- Use `allure agent` for smoke checks too, even when the change is small or mechanical.
- Only skip agent mode when it is impossible or when you are debugging agent mode itself.
- After each agent-mode test run, print the `index.md` path from that run's output directory so users can open the run overview quickly.

## Helpful Commands

- `allure agent latest` prints the latest agent output directory for the current project cwd.
- `allure agent state-dir` prints the state directory for the current project cwd.
- `allure agent select --latest` or `allure agent select --from <output-dir>` prints the review-targeted test plan from a prior agent run.
- `allure agent --rerun-latest -- <command>` or `allure agent --rerun-from <output-dir> -- <command>` reruns only the selected tests through the Allure testplan flow.

## Advanced Reruns

- `--rerun-preset review|failed|unsuccessful|all` changes how the rerun seed set is chosen.
- `--rerun-environment <id>` narrows reruns to one or more environments from the previous agent output.
- `--rerun-label name=value` narrows reruns to tests with exact matching labels from the previous agent output.
- `ALLURE_AGENT_STATE_DIR` overrides the default project-scoped state directory used by `allure agent latest`, `allure agent state-dir`, and `--rerun-latest`.

## Core Loops

### Test Review Loop

1. Identify the exact review scope.
2. Create a fresh expectations file for this run in a temp directory.
3. Run only that scope with `allure agent`.
4. Read `index.md`, `manifest/run.json`, `manifest/tests.jsonl`, and `manifest/findings.jsonl`.
5. Read per-test markdown only for tests that failed, drifted, or have findings.
6. Only after runtime review, inspect source code for root cause or coverage gaps.
7. If evidence is weak or partial, enrich the tests and rerun.
8. When iterating on the same scope, prefer `allure agent --rerun-latest -- <command>` or `allure agent --rerun-from <output-dir> -- <command>` so the rerun stays focused on the review-targeted tests.

### Feature Delivery Loop

1. Understand the feature or issue.
2. Create a fresh expectations file for this run in a temp directory.
3. Write or update the tests.
4. Run the target scope with `allure agent`.
5. Review `index.md`, manifests, and per-test markdown.
6. Enrich tests when evidence is weak.
7. Rerun until scope and evidence are acceptable.

### Small Test Change Workflow

1. Create a fresh expectations file and temp output directory for the touched scope.
2. Run the touched scope with `allure agent`, even if the goal is only a smoke check after a mechanical change such as typing cleanup, mock refactors, or helper extraction.
3. Review `index.md`, `manifest/run.json`, `manifest/tests.jsonl`, and `manifest/findings.jsonl`.
4. Only then make a final statement about regression safety or test correctness.

## Project Commands

- Full unit suite: `allure agent -- ./mvnw -q test`
- Single test class: `allure agent -- ./mvnw -q -Dtest=AllureVersionTest test`
- Single verifier IT class: `allure agent -- ./mvnw -q -Dmaven.repo.local=target/local-repo -Dit.test=VerifierAllure3IT verify`
- Full build verification: `allure agent -- ./mvnw -q -Dmaven.repo.local=target/local-repo verify`

## Per-Run Artifacts

- `ALLURE_AGENT_OUTPUT` must use a unique temp directory per run.
- `ALLURE_AGENT_EXPECTATIONS` must use a unique temp file per run.
- Do not reuse those paths across parallel runs.

YAML is preferred for expectations in v1.

Review-oriented expectations example:

```yaml
goal: Review Maven test scope
task_id: unit-test-review
notes:
  - Review runtime evidence before source inspection.
  - Do not invent label-based expectations until the tests actually emit matching labels.
```

Compact agent-mode pattern:

```bash
TMP_DIR="$(mktemp -d)"
EXPECTATIONS="$TMP_DIR/expectations.yaml"

allure agent \
  --output "$TMP_DIR/agent-output" \
  --expectations "$EXPECTATIONS" \
  -- ./mvnw -q test
```

## Evidence Rules

- Steps must wrap real setup, actions, state transitions, or assertions.
- Attachments must contain real runtime evidence from that execution.
- Metadata should stay minimal and purposeful.
- Prefer helper-boundary instrumentation over repetitive caller wrapping.

## When Console Errors Are Not Represented As Test Results

- Suite-load, import, or setup failures may appear only in agent global stderr artifacts.
- If `manifest/tests.jsonl` does not account for all visible failures from the test runner, inspect global stderr before concluding the run is fully modeled.
- Treat that state as a partial runtime review, not as a clean or complete result set.

## Acceptance Rules

Accept a run only when:

- scope matches expectations
- evidence is strong enough to explain what happened
- no high-confidence noop or placeholder findings remain

# Knowledge agent (Use Case 3)

Java / Spring Boot service that **ingests two uploaded files**—Jira stories and a regression-master of test cases—then generates Markdown **induction / onboarding documentation**.

Primary LLM backend is **GitHub Models** (GitHub Copilot / GitHub token), not OpenAI or Azure. Without a token the heuristic generator still produces a full pack.

This repo ships a **50-case internet banking regression master** plus **20 Jira stories** so you can run the slice without providing files.

## What it does

1. Upload (or auto-load) Jira stories (`JSON` / `CSV` / Markdown).
2. Upload (or auto-load) a regression master (`CSV` / `JSON` / Markdown).
3. Link story keys such as `IB-107` to test rows.
4. Write `output/induction.md` (overview, story walkthrough, regression landscape, onboarding checklist, automation context).

Live Jira API and requirements-document ingest are out of scope.

## Prerequisites

- Java 21
- Maven 3.8+

## Run locally (no GitHub token)

```bash
cd knowledge-agent
mvn test
mvn spring-boot:run
```

On startup the agent loads:

- `sample-data/jira/stories.json` (20 internet-banking stories)
- `sample-data/regression/regression-master.csv` (50 regression cases)

Then:

- UI: http://localhost:8080
- Health: `GET http://localhost:8080/api/health`
- Generate from the sample corpus:

```bash
curl -s -X POST http://localhost:8080/api/induction | python3 -m json.tool | head
```

- Upload your own files (same shapes as the samples):

```bash
curl -s -X POST http://localhost:8080/api/ingest \
  -F "jiraStories=@sample-data/jira/stories.json" \
  -F "regressionTests=@sample-data/regression/regression-master.csv"

curl -s -X POST http://localhost:8080/api/induction -o /tmp/induction.json
curl -s http://localhost:8080/api/induction
```

Generated Markdown is also on disk at `output/induction.md`. A checked-in example from this corpus is `sample-output/internet-banking-induction.md`.

## GitHub Copilot / GitHub Models (optional)

GitHub Copilot in the IDE is not an HTTP API. This service calls **GitHub Models** with a GitHub token.

1. Enable GitHub Models / Copilot for the account (org policy may block this).
2. Create a **fine-grained PAT** with **Models: Read-only**, or use `export GITHUB_TOKEN=$(gh auth token)` if that token includes Models.
3. Start with:

```bash
export KNOWLEDGE_LLM_ENABLED=true
export KNOWLEDGE_CHAT_MODEL=openai   # Spring AI adapter name; backend is GitHub Models
export GITHUB_TOKEN=github_pat_...
export GITHUB_MODELS_MODEL=openai/gpt-4o-mini
mvn spring-boot:run
```

| Variable | Default | Purpose |
| --- | --- | --- |
| `KNOWLEDGE_LLM_ENABLED` | `false` | Use GitHub Models |
| `KNOWLEDGE_CHAT_MODEL` | `none` | Set `openai` when enabling Models |
| `GITHUB_TOKEN` | empty | PAT / `gh` token |
| `GITHUB_MODELS_BASE_URL` | `https://models.github.ai/inference` | Inference root |
| `GITHUB_MODELS_CHAT_PATH` | `/chat/completions` | Chat path (not `/v1/...`) |
| `GITHUB_MODELS_MODEL` | `openai/gpt-4o-mini` | Model id |

Never commit the token. If the Models call fails, the agent falls back to the heuristic pack.

## Sample internet banking corpus

| File | Count | Coverage |
| --- | --- | --- |
| `sample-data/jira/stories.json` | 20 stories `IB-101`–`IB-120` | Login/MFA, session, accounts, transfers, limits, bill pay, beneficiaries, statements, cards, alerts, lockout, insufficient funds, password reset, future-dated pay |
| `sample-data/regression/regression-master.csv` | 50 cases `TC-01`–`TC-50` | Happy path and negatives for those flows |

CSV columns: `tcId,title,story,steps,expected`.

## REST

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/health` | Corpus sizes + LLM status |
| `POST` | `/api/ingest` | Multipart `jiraStories` + `regressionTests` |
| `POST` | `/api/ingest/jira-stories` | Multipart `file` |
| `POST` | `/api/ingest/regression-tests` | Multipart `file` |
| `GET` | `/api/artifacts` | In-memory corpus |
| `POST` | `/api/induction` | Generate Markdown |
| `GET` | `/api/induction` | Latest Markdown |

POC plan: see the Use Case 3 document in the project docs store (`docs/knowledge-agent-poc.md`).

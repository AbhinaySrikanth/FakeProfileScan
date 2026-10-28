# Fake Profile Scan: Revamp Audit

Labels: **Verified** (seen in source), **Claimed** (report/slides only), **Proposed**.

## 1. What the old prototype actually did (Verified)
| Finding | Severity |
|---|---|
| `Main_Activity.kt` decided fake/genuine locally with string heuristics; `ApiService`/`RetrofitClient` were never used for prediction | Critical |
| Retrofit pointed at `http://your_flask_server_ip:5003`; Flask ran on 5000 | Critical |
| Retrofit sent form-encoded fields; Flask called `request.get_json()` and read different field names | Critical |
| Backend ran its own heuristic first and called the model only if the heuristic said "not fake" | Critical |
| Model call was `model3(model3, query)` (a model is not callable), so the model path could never have worked | Critical |
| All exceptions returned `200 {"error": ...}` | High |
| Result screen used big red/green "fake / genuine" buttons that did nothing | High |
| Unselected radio groups silently became `0`; numeric fields were free text | Medium |
| Model shipped as an unversioned pickle of a Keras model plus three duplicate artifacts; no evaluation | High |
| `.venv`, Gradle/IDE output, APK build output sat next to the source | Low |

## 2. Claimed but not true (report/slides)
Real-world accuracy, HTTPS, input validation, scalability, direct extraction from Instagram, and "reporting" of accounts.
None is implemented. The product no longer makes these claims; reporting is a link to the platform's own help.

## 3. What changed
- **One decision path.** The server model decides. The app contains no classifier.
- **Contract.** `POST /api/v1/assessments`, JSON, strict pydantic validation (types, bounds, unknown fields rejected), typed errors with request ID, 4 KB payload limit, health and model-metadata endpoints.
- **ML.** `features.py` is used by both training and serving. `train.py` compares logistic regression and gradient boosting with 5-fold CV (same seed), removes train/test overlap, and writes `model_card.json` (metrics, thresholds, data hash, limitations). Interpretable logistic regression was selected; test ROC-AUC 0.936, F1 0.874 at 0.5.
- **Honest output.** Risk band, not a verdict. Per-signal contributions, a low-confidence flag (mixed score or inputs far outside training range), model version and timestamp. Missing data is rejected, never defaulted.
- **Privacy.** App sends bio length, not bio text. Server logs request ID and band only. History is local, 30-day default retention, username stored only if enabled, backup disabled, export and delete-all provided. No password/cookie/scraping features.
- **App.** Compose, one ViewModel, bottom navigation (Assess, History, Safety, Settings), form → result flow whose Back returns to Assess, loading with cancel, offline/timeout/server/validation states, 3-colour theme (ink, paper, amber) with dark mode, adaptive icon, release build is HTTPS-only with no placeholder host.
- **Removed.** Fake buttons, duplicate heuristics, Retrofit/Gson/AppCompat/ViewBinding/XML layouts, hard-coded URLs. The old prototype files were removed from the repository.

## 4. Not done / next
- Run the UI on a device or emulator (not possible in this environment) and do accessibility passes (TalkBack, font scale).
- Authoritative dataset provenance and licence; larger, time-split data; calibration check; threshold review (0.30/0.70 are currently set by judgement, not tuned).
- Instrumented/UI tests; CI; TLS termination, shared-store rate limiting (current limiter is per-process), auth, monitoring and rollback for deployment.
- Rewrite the academic report and slides to match this document.
- Final name check (trademark, domain, store) for "Fake Profile Scan"; package is `com.fakeprofilescan.app`.

## 5. Production-ready means
Reproducible training with reviewed metrics; API behind HTTPS with rate limits and alerts; signed release build tested on devices; privacy notice published; documented rollback. None of the deployment items exist yet.

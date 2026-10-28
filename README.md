# Fake Profile Scan

Profile risk assessment for suspicious social profiles. Enter what you can see on a public profile; get a risk band
(Low / Review / Elevated), the signals behind it, a confidence flag, and next steps. It does **not** claim an account is
fake, never contacts any platform, and never reports anyone.

## Layout
- `app/` Android app (Kotlin, Compose, single module). Screens: Assess, Form, Result, History, Safety, Settings.
- `backend/` Flask API + reproducible training (`train.py`), shared feature code (`features.py`), tests.
- `docs/REVAMP_PLAN.md` the plan this revamp follows; `docs/PRODUCT_REVAMP_AUDIT.md` what was wrong and what changed.

## Features
- Risk band + per-signal explanation + low-confidence flag, from one server-side model.
- **Behaviour flags:** tick what the profile did (asked for money, urgency, copied photos...). The server may raise the band, never lower it, and says so.
- **Re-check trend:** assessing the same saved profile again shows how the score moved (needs "keep username" on).
- **Decision tracking:** tag saved results Ignored / Blocked / Reported / Followed / Still unsure; filter history by it.
- Counts accepted as shown on profiles (`850`, `12,400`, `1.2K`, `3M`).
- API rate limit (30 POSTs/min/IP, in-process), local history with retention, export and delete-all.

## Run the backend
```bash
cd backend
pip install -r requirements.txt
python train.py                      # writes model.joblib + model_card.json (metrics, thresholds, provenance)
python -m unittest discover -s tests # API contract + feature tests
flask --app app run --port 5000      # dev;  production: waitress-serve --port 5000 app:app (behind HTTPS)
```
Endpoints: `POST /api/v1/assessments`, `GET /api/v1/model`, `GET /health/live`, `GET /health/ready`.
Errors: `{"error": {"code", "message", "fields"?, "request_id"}}` (400/405/413/422/500).

## Run the app
```bash
./gradlew testDebugUnitTest assembleDebug
```
Debug builds talk to `http://10.0.2.2:5000/` (emulator to host). On a phone or in release, set an HTTPS server
address in Settings; release builds ship with no server configured and refuse plain HTTP.

## Known limits
- Model: logistic regression on a 576-row public dataset; test ROC-AUC about 0.94. Dataset provenance/licence is unverified.
- History is app-private storage, excluded from backup, not separately encrypted.
- No accounts, rate limiting or TLS termination are included; put the API behind a reverse proxy before exposing it.
- The Android UI has been compiled, linted and unit-tested but not run on a device or emulator.

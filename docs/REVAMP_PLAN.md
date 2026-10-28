# Product Revamp Audit and COO Plan

## 1. Executive problem statement

The current project is an academic prototype presented as a production-capable **“Fake Social Media (Instagram) Profile Detection and Reporting Using Machine Learning”** product. Its implementation does not support that description consistently:

- The Android app makes a local hard-coded heuristic decision instead of calling the declared ML API.
- The Android Retrofit contract sends form-encoded data to a placeholder host/port, while Flask expects JSON and runs on a different port.
- The backend contains duplicated heuristic logic and conditionally invokes the model only after a heuristic says the account is not fake.
- The model-serving and serialization path is unclear and not reproducibly deployable.
- The reported “reporting” feature is not implemented as an authorized platform workflow.
- The UI contains fake result buttons that visually communicate a classification rather than providing meaningful actions; these are not gesture recognition or product functionality.
- The report and presentations describe HTTPS, validation, scalability, accurate real-world detection, direct reporting, and production-like reliability that are not demonstrated by the repository.
- The codebase includes generated build output, virtual-environment contents, duplicate model artifacts, unclear naming, sparse tests, and no credible operational baseline.

The goal is not to hide these issues. The goal is to convert the project into an honest, useful, demonstrable product with a defensible purpose, measurable behavior, responsible UX, reproducible ML, and a path to production deployment.

## 2. Proposed product direction

### Working product name

**TrustLens** — Profile Risk Assessment and Moderation Assistant.

Other candidates to evaluate during branding:

- SignalGuard
- ProfileCheck
- AccountScope
- TrustSignal

The final name must be checked for trademark, domain, package-name, app-store, and platform-brand conflicts. Avoid using “Instagram” in the product name unless an authorized integration and branding permission exist.

### Purpose

TrustLens helps a user or authorized moderation team assess whether a social profile exhibits signals commonly associated with impersonation, spam, coordinated abuse, or low-authenticity behavior. It returns an explainable **risk assessment**, evidence, confidence/uncertainty, and recommended next steps. It does not decide that a person is fake, prove malicious intent, or automatically report an account.

### Real-life users

1. Individual users checking a suspicious profile before following, messaging, paying, or sharing information.
2. Community moderators screening user-submitted profiles for manual review.
3. Trust-and-safety teams triaging reports using consistent signals.
4. Researchers and students evaluating responsible profile-risk models.

### Responsible product promise

> “Assess observable profile signals and guide safer decisions; do not replace human judgment or platform enforcement.”

The product must avoid defamation, discriminatory inference, false certainty, automated harassment, mass-report abuse, and unauthorized data collection.

### MVP scope

- Manual entry or authorized import of profile metadata.
- Input validation with clear units, ranges, and examples.
- Risk score or risk band: Low, Review, or Elevated.
- Signal breakdown explaining which observable inputs affected the result.
- Confidence/uncertainty and “insufficient data” state.
- Local assessment history with delete/export controls, if history is retained.
- Manual user notes and a safety checklist.
- A non-automated handoff to the platform’s official reporting/help flow.
- Model/version and assessment timestamp in the result.
- API and Android client with one documented, tested contract.

### Explicit non-goals

- No claim that the model knows the true identity or intent of an account.
- No scraping, credential collection, login automation, or bypassing platform controls.
- No automated Instagram reporting without an authorized API and abuse-prevention design.
- No use of profile appearance, name, language, location, or other proxies to infer protected traits.
- No silent background monitoring of people.
- No “100% accurate,” “real-time guaranteed,” or “production-ready” claims without evidence.

## 3. Evidence-based current-state inventory

The audit Markdown must describe the current project as it actually exists:

### Repository components to inspect and classify

- Android project under `app/`.
- Kotlin activities, Retrofit service, response model, layouts, themes, strings, manifest, and Gradle configuration.
- Flask project under `Fake_profile_detection_app/`.
- `Flask_app.py`, `Model.py`, serialized model files, virtual environment, and Qodana output.
- Training notebook and checkpoint notebook.
- `train.csv` and `test.csv`.
- H5/Keras and pickle model artifacts.
- Report DOCX.
- Both PPTX presentations.
- UML and architecture images.
- Screenshots, icons, generated APKs, Gradle/build output, and IDE metadata.

### Verified implementation facts to record

- `Main_Activity.kt` performs `detectFakeAccount()` locally using string and count heuristics.
- The Android activity does not use `ApiService` or `RetrofitClient` for its prediction flow.
- `RetrofitClient.kt` contains `http://your_flask_server_ip:5003`, which is not a usable configured deployment endpoint.
- `ApiService.kt` declares `@FormUrlEncoded`, while `Flask_app.py` calls `request.get_json()`.
- Flask defaults to port 5000, not the Android placeholder port 5003.
- The backend catches all exceptions and returns `{'error': str(e)}` without a consistent HTTP error contract.
- `Model.py` contains a separate heuristic `prediction()` function and a model helper whose parameter is not used correctly.
- `Flask_app.py` imports `prediction` and `query`, loads a pickle model, and invokes the model only for the heuristic “not fake” branch.
- The result screen uses large fake/genuine buttons as visual labels; they are not meaningful actions, and the requested audit must explicitly identify them as fake or misleading UI.
- Form validation is incomplete and can silently map an unselected radio group to `0`.
- Numeric inputs are configured as text and do not visibly communicate valid ranges or units.
- There is no demonstrated authenticated user/account layer, persistence service, audit log, rate limiting, monitoring, CI/CD, or production deployment configuration.

### Evidence classification

Every audit statement must be labeled conceptually as one of:

- **Verified:** directly observed in source or artifact.
- **Claimed:** stated in the report or PPT but not proven by implementation.
- **Inferred risk:** reasonable consequence of verified behavior.
- **Proposed:** future product or engineering decision.

## 4. Report and presentation truth audit

The audit Markdown must compare every major claim in the report/PPT with repository evidence.

### Claims to correct or remove

- “Detection and reporting” should become “risk assessment and moderation support.”
- “Advanced machine learning” should be replaced with the exact model, features, training procedure, evaluation metrics, and limitations.
- “Accurate,” “robust,” “scalable,” “reliable,” and “real-world” must require reproducible measurements.
- HTTPS must not be claimed until TLS is configured and tested in the deployed environment.
- “Input validation” must not be claimed based on narrative diagrams when the implementation lacks schema validation and consistent error codes.
- Direct extraction from Instagram must be removed unless an authorized API integration exists.
- Automatic reporting must be removed or described as a future, human-confirmed handoff.
- UML/database/deployment diagrams must match the actual architecture, not an aspirational one.
- Postman testing must include saved requests, response contracts, negative cases, and reproducible results.

### Documentation rebuild

Replace the academic narrative with:

1. Problem and user research assumptions.
2. Responsible product positioning and non-goals.
3. Current prototype limitations.
4. Data source, license, provenance, label quality, class balance, and privacy.
5. Reproducible model training and evaluation.
6. API specification and error contract.
7. Android UX and accessibility decisions.
8. Threat model and abuse-prevention controls.
9. Deployment architecture and operational runbook.
10. Known limitations, monitoring, rollback, and roadmap.

## 5. Feature triage

### Useful and retain, but rebuild

| Current capability | Product value | Required change |
|---|---|---|
| Profile metadata input | Enables manual assessment | Replace raw form with guided sections, validation, examples, and sufficient-data checks |
| Feature engineering for counts/ratios | Potential model input foundation | Centralize in one typed preprocessing pipeline shared by training and inference |
| Flask API concept | Reasonable serving boundary | Implement versioned JSON API, schema validation, health checks, typed errors, and tests |
| Android client | Useful distribution channel | Use a modern state-driven UI, actual API integration, loading/error states, and accessible components |
| Result screen | Important user outcome | Show risk band, uncertainty, signals, limitations, and recommended actions instead of oversized fake/genuine buttons |
| Notebook and dataset | Academic reproducibility foundation | Clean, document, version, split, evaluate, and export through a reproducible pipeline |

### Working but misleading or low-value

- Hard-coded string heuristics that label profiles “fake” from dots, digits, short names, or words such as “scam.”
- A binary fake/genuine result without uncertainty or reason codes.
- Buttons that only serve as visual status labels.
- “Predict another account” navigation that starts a new activity instead of clearing or restoring a controlled assessment state.
- Manually typed numeric fields without range, unit, or semantic guidance.
- A separate local detector that bypasses the declared backend.

### Broken, unimplemented, or falsely represented

- Android-to-Flask prediction integration.
- Consistent request format and port configuration.
- Real reporting workflow.
- Verified HTTPS deployment.
- Reliable model invocation for every eligible assessment.
- Meaningful server-side validation and machine-readable error handling.
- Production monitoring, authentication/authorization, rate limiting, persistence, and auditability.
- Evidence for claimed real-world accuracy and scalability.

### Remove

- Fake result buttons as if they were interactive functionality.
- Duplicated heuristic classifiers in client and backend.
- Unsupported “fake” certainty language.
- Unused or misleading imports, variables, and model helpers.
- Hard-coded placeholder URLs and environment-specific values.
- Generated APK/build output, `.gradle`, IDE metadata, and `.venv` from source control.
- Duplicate/ambiguous model files unless each artifact has a documented purpose and version.
- Any claim or feature implying unauthorized Instagram scraping or automatic reporting.

## 6. Target product experience and navigation

### Primary navigation

1. **Home / Assess**
   - Explain what the product can and cannot determine.
   - Start a new assessment.
   - Choose manual entry or authorized/imported metadata.
2. **Assessment form**
   - Identity/profile presentation signals.
   - Activity and audience signals.
   - Links and privacy signals.
   - Optional context and user notes.
   - Inline validation and progress.
3. **Assessment result**
   - Risk band and uncertainty.
   - Signal explanation with neutral language.
   - Data completeness indicator.
   - Safety recommendations.
   - Save, export, delete, or open official reporting/help guidance.
4. **History**
   - Locally stored or account-scoped assessments.
   - Search, filter, detail, delete, and retention explanation.
5. **Learn / Safety**
   - Scam patterns, privacy guidance, limitations, and reporting education.
6. **Settings**
   - Privacy, retention, export/delete, accessibility, model/app version, feedback, and legal notices.

### Required UX states

- Empty state.
- First-use education.
- Editing and validation.
- Loading with cancellation.
- Network timeout/offline.
- Server unavailable.
- Invalid or incomplete input.
- Insufficient evidence.
- Low-confidence prediction.
- Successful result.
- Saved result.
- Deleted result.
- Model unavailable or version retired.
- Unauthorized integration or expired authorization.

### UX and visual polish

- Replace the purple prototype styling with a restrained trust-and-safety visual system.
- Use semantic colors with text/icon labels, not color alone.
- Avoid red/green moral judgment; use neutral risk bands and explanatory language.
- Use Material components, consistent spacing, typography hierarchy, touch targets, contrast, focus order, and screen-reader labels.
- Show examples and definitions for every non-obvious input.
- Avoid collecting fields that do not influence the model or user decision.
- Make the primary action clear and keep destructive actions separate.
- Use a result card with “What we observed,” “What this does not mean,” and “What you can do.”

## 7. Target technical architecture

### Client

- Kotlin with a single source of truth for screen state.
- ViewModel and repository boundary.
- Retrofit/OkHttp with one JSON contract.
- Secure configurable base URL per build environment.
- Structured error mapping, retry policy, cancellation, and offline behavior.
- ViewBinding or Compose consistently; do not mix patterns without a reason.
- No model decision logic duplicated in the client.

### API

- Versioned endpoint such as `POST /api/v1/assessments`.
- JSON request/response documented with OpenAPI.
- Pydantic/Marshmallow/schema validation.
- Typed fields and explicit bounds.
- Stable error shape: code, message, field errors, request ID.
- `GET /health/live`, `GET /health/ready`, and model metadata endpoint.
- Request IDs, structured logs, rate limits, payload limits, and safe redaction.
- Authentication and authorization for team/history features.

### Inference service

- One preprocessing package shared by training and inference.
- Explicit model artifact, feature schema, version, threshold, calibration, and provenance.
- No heuristic gate that silently overrides the model.
- Return risk score, band, reason codes, model version, and uncertainty.
- Reject unsupported or insufficient inputs rather than inventing defaults.
- Keep model loading at process startup with readiness checks.

### Storage

- Start with encrypted local history for MVP if no account is necessary.
- Use a server database only when cross-device history, team moderation, or analytics requires it.
- Define retention, deletion, export, access controls, and audit events.
- Do not retain raw profile data by default.

## 8. ML and data remediation

### Dataset governance

- Identify dataset source, license, collection date, labels, class balance, and limitations.
- Avoid presenting a small Kaggle-style dataset as representative of all social platforms.
- Inspect duplicates, leakage, missing values, outliers, and train/test overlap.
- Use a reproducible split strategy and record random seeds.
- Document that profile-level labels may be noisy and time/platform dependent.

### Model process

- Establish a baseline model before a neural network.
- Compare logistic regression, tree ensembles, and the current neural network using the same splits.
- Measure precision, recall, F1, ROC-AUC/PR-AUC, confusion matrix, calibration, and subgroup/data-slice behavior where lawful and meaningful.
- Select thresholds based on product risk; do not default to a binary 0.5 decision.
- Add uncertainty and insufficient-data handling.
- Track model registry metadata and reproducible training artifacts.
- Add inference parity tests to ensure training and serving features are identical.
- Monitor drift and feedback quality; do not retrain directly from unverified user reports.

### Responsible output language

- “Elevated risk based on available signals,” not “This account is fake.”
- “This is not proof of fraud or identity.”
- “Review the profile independently and use the platform’s official safety/reporting controls.”

## 9. Code quality and optimization plan

- Rename `Main_Activity.kt` to the repository’s chosen naming convention and align package/application names with the product.
- Move constants and environment settings out of source code.
- Replace `!!`, sentinel `-1`, and silent radio defaults with typed validation.
- Use numeric input types and range checks.
- Introduce request/response DTOs and a domain assessment model.
- Centralize feature engineering and remove duplicate `prediction()` logic.
- Remove broad exception serialization and return safe, logged errors.
- Add unit tests for validation, feature extraction, thresholds, API mapping, and result rendering.
- Add integration tests for the API and client contract.
- Add lint, formatting, dependency scanning, and CI.
- Keep source control limited to source, configuration templates, documentation, tests, and reproducible small fixtures.
- Add `.gitignore`, `.env.example`, dependency lock/version policy, and setup instructions.
- Pin and document Python, Android, Gradle, Kotlin, and model runtime versions.
- Separate development, staging, and production configuration.

## 10. Security, privacy, and abuse prevention

- Never request Instagram passwords or session cookies.
- Use only user-provided or authorized data.
- Minimize collection and default to short retention.
- Encrypt data in transit and at rest.
- Redact profile identifiers from logs.
- Add authentication/authorization for stored assessments and team features.
- Add rate limiting, payload limits, abuse detection, and request tracing.
- Prevent mass-assessment automation and bulk targeting.
- Provide deletion, export, privacy notice, consent, and data-source disclosure.
- Threat-model model extraction, prompt/input abuse if AI is later added, insecure deserialization, SSRF, denial of service, and unauthorized reporting.
- Replace unsafe pickle deployment with a controlled, compatible artifact format where possible; if pickle remains temporarily, restrict artifact provenance and loading environment.

## 11. Testing and acceptance criteria

### Functional acceptance

- A valid Android assessment reaches the configured API and receives the documented response.
- Invalid, incomplete, out-of-range, offline, timeout, and server-error states are user-visible and actionable.
- The result displays risk band, signals, uncertainty, model version, timestamp, and limitations.
- No client-side classifier can bypass or contradict the server result.
- History, deletion, and export follow the declared privacy policy.

### ML acceptance

- Training can be reproduced from documented inputs and configuration.
- Evaluation reports include class-aware metrics and confusion matrix.
- Inference features exactly match training features.
- Threshold and calibration decisions are documented.
- Model version is present in every result.
- Performance limitations are disclosed and monitored.

### Product acceptance

- A new user can complete an assessment without external explanation.
- All major actions have accessible labels and adequate touch targets.
- The UI never implies certainty or official platform enforcement.
- A user can understand what to do after an elevated-risk result.
- The app has a credible failure path for every network/model failure.

### Operational acceptance

- CI runs formatting, static analysis, unit tests, API tests, and build checks.
- Staging deployment is reproducible.
- Health/readiness checks work.
- Logs contain request IDs but no sensitive raw profile data.
- Alerts exist for elevated error rate, latency, unavailable model, and drift signals.
- Rollback to a prior API/model version is documented and tested.

## 12. Delivery roadmap

### Phase 0 — Truth and cleanup

- Freeze unsupported claims.
- Create the audit Markdown.
- Remove generated artifacts and secrets from version control.
- Inventory dependencies and model files.
- Establish a clean baseline build and test report.

### Phase 1 — Working vertical slice

- Choose final name and package identity.
- Define the JSON/OpenAPI contract.
- Implement one backend inference path.
- Connect Android to the backend.
- Replace fake buttons with an honest result experience.
- Add validation, loading, error, and insufficient-data states.

### Phase 2 — Reproducible ML

- Clean and document data.
- Rebuild the preprocessing pipeline.
- Benchmark baselines and current model.
- Add evaluation, calibration, thresholding, model versioning, and parity tests.

### Phase 3 — Product UX and privacy

- Implement navigation, history, safety education, retention controls, accessibility, and polished design system.
- Add neutral language, explainability, feedback, and official reporting handoff.
- Add privacy/legal content and consent flows where required.

### Phase 4 — Production hardening

- Add authentication only for features that need it.
- Add secure deployment, observability, rate limits, CI/CD, staging, backups, and rollback.
- Run security, performance, accessibility, and abuse testing.

### Phase 5 — Evidence-backed expansion

- Validate with real users and authorized moderation workflows.
- Consider additional platforms only through compliant integrations.
- Add new signals only when data quality, fairness, consent, and operational value are demonstrated.

## 13. Final audit deliverable

Create a repository Markdown file, proposed name:

`PRODUCT_REVAMP_AUDIT.md`

It must contain:

1. Executive summary and proposed TrustLens identity.
2. Current repository inventory.
3. What the report and PPT claim.
4. What the code actually does.
5. Verified discrepancies and severity.
6. Working features.
7. Working but useless or misleading features.
8. Unimplemented, broken, or wrongly implemented features.
9. Fake gesture/result buttons and other deceptive UI elements.
10. Features to remove.
11. Features to retain and rebuild.
12. UX, navigation, copy, accessibility, and visual-design improvements.
13. Code, API, architecture, ML, data, security, and deployment optimizations.
14. Proposed product features and non-goals.
15. Product requirements and acceptance criteria.
16. Roadmap, priorities, dependencies, and risks.
17. Documentation/report/PPT rewrite guidance.
18. Definition of “production-ready” for this project.

The Markdown must be specific enough that another engineer, designer, ML practitioner, or product manager can execute the revamp without relying on the old report’s unsupported assumptions.

## 14. Planning constraints

- Audit the whole project system: Android app, Flask/ML backend, notebook, datasets, model artifacts, report, both presentations, UML/images, and generated/dependency artifacts.
- Preserve useful academic evidence where possible, but rewrite or remove claims the implementation cannot support.
- Explicitly call out the client/server mismatch, duplicated classifier logic, conditional model invocation, fake result buttons, and unsupported reporting claims.
- Treat current model quality as unverified until reproducible evaluation, leakage checks, calibration, threshold selection, and provenance are added.
- Do not claim direct Instagram extraction/reporting unless an authorized, documented platform integration exists.
- The audit document is the first repository deliverable; implementation changes follow the prioritized plan.
- Do not hide prototype limitations. Product trust depends on accurately separating current behavior from future design.

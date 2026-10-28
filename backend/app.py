"""Fake Profile Scan API. Run: flask --app app run  |  prod: waitress-serve --port 5000 app:app"""
import json, logging, time, uuid
from collections import defaultdict, deque
from pathlib import Path
from typing import Literal
from datetime import datetime, timezone
import joblib
from flask import Flask, g, jsonify, request
from pydantic import BaseModel, ConfigDict, Field, ValidationError, field_validator
from features import FEATURES, LABELS, extract

HERE = Path(__file__).parent
log = logging.getLogger("fakeprofilescan")
logging.basicConfig(level=logging.INFO, format="%(asctime)s %(message)s")


# Behaviour the user observed. Weighted evidence that can raise (never lower) the model's band.
FLAGS = {
    "asks_money": ("Asked for money, gift cards or crypto", 2),
    "asks_codes": ("Asked for passwords, codes or card details", 2),
    "urgency": ("Pressure to act quickly", 1),
    "off_platform": ("Pushed to move the chat to another app", 1),
    "stolen_photos": ("Photos appear copied from elsewhere", 1),
    "lookalike": ("Username imitates someone or a brand you know", 1),
    "too_good": ("Offers prizes, jobs or investment returns", 1),
}
FlagId = Literal[tuple(FLAGS)]
BANDS = ["low", "review", "elevated"]


def escalate(band: str, flag_ids: list[str]):
    """Return (final_band, reason|None). 2 points -> at least review, 3+ -> elevated."""
    pts = sum(FLAGS[f][1] for f in flag_ids)
    floor = "elevated" if pts >= 3 else "review" if pts >= 2 else "low"
    if BANDS.index(floor) > BANDS.index(band):
        return floor, f"Raised from {band} to {floor} because of behaviour you reported."
    return band, None


class Profile(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True)
    username: str = Field(min_length=1, max_length=30)
    full_name: str = Field(max_length=100)
    has_profile_picture: bool
    bio_length: int = Field(ge=0, le=150)
    has_external_url: bool
    is_private: bool
    posts: int = Field(ge=0, le=10_000_000)
    followers: int = Field(ge=0, le=1_000_000_000)
    following: int = Field(ge=0, le=100_000)
    observed_flags: list[FlagId] = Field(default_factory=list, max_length=len(FLAGS))

    @field_validator("username")
    @classmethod
    def _no_space(cls, v):
        if any(c.isspace() for c in v):
            raise ValueError("must not contain spaces")
        return v


def create_app(model_path=HERE / "model.joblib", card_path=HERE / "model_card.json", rate_limit=30, rate_window=60):
    app = Flask(__name__)
    app.config["MAX_CONTENT_LENGTH"] = 4096
    card = json.loads(Path(card_path).read_text())
    model = joblib.load(model_path)  # loaded once at startup; joblib file built by train.py only
    scaler, clf = model.steps[0][1], model.steps[1][1]
    t = card["thresholds"]

    def error(status, code, message, fields=None):
        body = {"error": {"code": code, "message": message, "request_id": g.rid}}
        if fields:
            body["error"]["fields"] = fields
        return jsonify(body), status

    hits = defaultdict(deque)  # ip -> request times; in-process only (one worker). Use a shared store behind many workers.

    @app.before_request
    def _rid():
        g.rid = request.headers.get("X-Request-ID", uuid.uuid4().hex[:12])[:64]
        if request.method == "POST":
            q, now = hits[request.remote_addr], time.monotonic()
            while q and now - q[0] > rate_window:
                q.popleft()
            if len(q) >= rate_limit:
                return error(429, "rate_limited", "Too many assessments. Please wait a minute and try again.")
            q.append(now)

    @app.after_request
    def _hdr(r):
        r.headers["X-Request-ID"] = g.rid
        return r

    @app.get("/health/live")
    def live():
        return {"status": "ok"}

    @app.get("/health/ready")
    def ready():
        return {"status": "ready", "model_version": card["model_version"]}

    @app.get("/api/v1/model")
    def meta():
        return {k: card[k] for k in ("model_version", "algorithm", "thresholds", "test", "limitations")}

    @app.post("/api/v1/assessments")
    def assess():
        if (request.content_length or 0) > app.config["MAX_CONTENT_LENGTH"]:
            return error(413, "payload_too_large", "Request body too large.")
        body = request.get_json(silent=True)
        if not isinstance(body, dict):
            return error(400, "invalid_json", "Request body must be a JSON object.")
        try:
            p = Profile.model_validate(body).model_dump()
        except ValidationError as e:
            fields = {".".join(map(str, x["loc"])): x["msg"] for x in e.errors()}
            return error(422, "validation_failed", "Some fields are missing or out of range.", fields)

        x = extract(p)
        score = float(model.predict_proba([x])[0][1])
        model_band = "elevated" if score >= t["elevated"] else "review" if score >= t["review"] else "low"
        flag_ids = list(dict.fromkeys(p["observed_flags"]))
        band, escalation = escalate(model_band, flag_ids)
        scaled = scaler.transform([x])[0]
        contrib = clf.coef_[0] * scaled
        order = sorted(range(len(FEATURES)), key=lambda i: -abs(contrib[i]))[:4]
        signals = [{"name": LABELS[FEATURES[i]],
                    "effect": "raises" if contrib[i] > 0 else "lowers",
                    "weight": round(float(abs(contrib[i])), 2)} for i in order]
        # Raw values far outside what the model was fitted on make the score unreliable.
        lo, hi = scaler.mean_ - 4 * scaler.scale_, scaler.mean_ + 4 * scaler.scale_
        out_of_range = [LABELS[f] for f, v, a, b in zip(FEATURES, x, lo, hi) if not a <= v <= b]
        confidence = "low" if out_of_range or abs(score - 0.5) < 0.15 else "normal"
        log.info("assessment rid=%s band=%s flags=%d", g.rid, band, len(flag_ids))  # never log profile data
        return jsonify({
            "risk_score": round(score, 3), "risk_band": band, "model_band": model_band, "escalation": escalation,
            "reported_flags": [{"id": f, "label": FLAGS[f][0]} for f in flag_ids], "confidence": confidence,
            "signals": signals, "unusual_inputs": out_of_range,
            "model_version": card["model_version"],
            "assessed_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
            "request_id": g.rid,
        })

    @app.errorhandler(404)
    def _404(_):
        return error(404, "not_found", "Unknown endpoint.")

    @app.errorhandler(413)
    def _413(_):
        return error(413, "payload_too_large", "Request body too large.")

    @app.errorhandler(405)
    def _405(_):
        return error(405, "method_not_allowed", "Method not allowed.")

    @app.errorhandler(Exception)
    def _500(e):
        log.exception("unhandled rid=%s", g.get("rid"))
        return error(500, "internal_error", "Unexpected server error.")

    return app


app = create_app()

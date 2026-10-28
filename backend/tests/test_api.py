import sys, unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent.parent))
import pandas as pd, joblib
from app import create_app
from features import FEATURES, extract

OK = dict(username="anna_travels", full_name="Anna Lee", has_profile_picture=True, bio_length=60,
          has_external_url=False, is_private=False, posts=120, followers=900, following=400)
SUSPECT = dict(username="user83920193", full_name="", has_profile_picture=False, bio_length=0,
               has_external_url=False, is_private=False, posts=0, followers=3, following=2000)


class Api(unittest.TestCase):
    def setUp(self):
        self.c = create_app().test_client()

    def post(self, body):
        return self.c.post("/api/v1/assessments", json=body)

    def test_contract(self):
        r = self.post(OK); j = r.get_json()
        self.assertEqual(r.status_code, 200)
        for k in ("risk_score", "risk_band", "confidence", "signals", "model_version", "assessed_at", "request_id"):
            self.assertIn(k, j)
        self.assertIn(j["risk_band"], ("low", "review", "elevated"))

    def test_ordering(self):
        self.assertLess(self.post(OK).get_json()["risk_score"], self.post(SUSPECT).get_json()["risk_score"])
        self.assertEqual(self.post(SUSPECT).get_json()["risk_band"], "elevated")

    def test_validation(self):
        for bad in ({**OK, "followers": -1}, {**OK, "posts": "5"}, {k: v for k, v in OK.items() if k != "bio_length"},
                    {**OK, "extra": 1}, {**OK, "username": "has space"}):
            r = self.post(bad); self.assertEqual(r.status_code, 422, bad)
            self.assertIn("fields", r.get_json()["error"])

    def test_errors(self):
        self.assertEqual(self.c.post("/api/v1/assessments", data="x").status_code, 400)
        self.assertEqual(self.c.get("/nope").status_code, 404)
        self.assertEqual(self.c.post("/api/v1/assessments", data="x" * 5000).status_code, 413)

    def test_health(self):
        self.assertEqual(self.c.get("/health/ready").status_code, 200)


class Flags(unittest.TestCase):
    def setUp(self):
        self.c = create_app().test_client()

    def test_flags_escalate_never_lower(self):
        j = self.c.post("/api/v1/assessments", json={**OK, "observed_flags": ["asks_money"]}).get_json()
        self.assertGreaterEqual(["low", "review", "elevated"].index(j["risk_band"]), 1)
        self.assertEqual(j["reported_flags"][0]["id"], "asks_money")
        self.assertIn("Raised", j["escalation"])
        base = self.c.post("/api/v1/assessments", json=OK).get_json()
        self.assertEqual(base["risk_band"], base["model_band"])
        self.assertIsNone(base["escalation"])
        s = self.c.post("/api/v1/assessments", json={**SUSPECT, "observed_flags": []}).get_json()
        self.assertEqual(s["risk_band"], "elevated")

    def test_three_points_is_elevated(self):
        j = self.c.post("/api/v1/assessments", json={**OK, "observed_flags": ["asks_money", "urgency"]}).get_json()
        self.assertEqual(j["risk_band"], "elevated")

    def test_unknown_flag_rejected(self):
        self.assertEqual(self.c.post("/api/v1/assessments", json={**OK, "observed_flags": ["nope"]}).status_code, 422)

    def test_rate_limit(self):
        c = create_app(rate_limit=3).test_client()
        codes = [c.post("/api/v1/assessments", json=OK).status_code for _ in range(5)]
        self.assertEqual(codes, [200, 200, 200, 429, 429])
        self.assertEqual(c.get("/health/live").status_code, 200)


class Parity(unittest.TestCase):
    def test_serving_features_match_training_csv(self):
        """extract() must reproduce the dataset's engineered columns from raw inputs."""
        row = extract(OK)
        self.assertEqual(len(row), len(FEATURES))
        self.assertAlmostEqual(extract({**OK, "username": "ab12"})[1], 0.5)
        self.assertEqual(extract({**OK, "full_name": "ANNA_TRAVELS"})[4], 1.0)
        self.assertEqual(extract(OK)[2], 2.0)


if __name__ == "__main__":
    unittest.main()

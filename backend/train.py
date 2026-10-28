"""Reproducible training: python train.py  -> model.joblib + model_card.json"""
import json, hashlib, datetime
import joblib, numpy as np, pandas as pd
from sklearn.ensemble import GradientBoostingClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import average_precision_score, confusion_matrix, f1_score, roc_auc_score
from sklearn.model_selection import cross_val_predict, StratifiedKFold
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler
from features import FEATURES

SEED = 42
tr = pd.read_csv("data/train.csv"); te = pd.read_csv("data/test.csv")
tr.columns = te.columns = FEATURES + ["label"]
# leakage check: drop test rows that also appear in train
dup = te.merge(tr.drop_duplicates(), on=FEATURES + ["label"], how="inner").shape[0]
te = te.merge(tr.drop_duplicates(), on=FEATURES + ["label"], how="left", indicator=True)
te = te[te["_merge"] == "left_only"].drop(columns="_merge") if dup else te
X, y = tr[FEATURES].values, tr["label"].values
Xt, yt = te[FEATURES].values, te["label"].values

cands = {
    "logistic_regression": make_pipeline(StandardScaler(), LogisticRegression(max_iter=2000, random_state=SEED)),
    "gradient_boosting": GradientBoostingClassifier(random_state=SEED),
}
cv = StratifiedKFold(5, shuffle=True, random_state=SEED)
cv_auc = {k: roc_auc_score(y, cross_val_predict(m, X, y, cv=cv, method="predict_proba")[:, 1]) for k, m in cands.items()}
# Prefer the interpretable model unless the tree model is clearly (>0.02 AUC) better.
best = "gradient_boosting" if cv_auc["gradient_boosting"] - cv_auc["logistic_regression"] > 0.02 else "logistic_regression"
model = cands[best].fit(X, y)

# Thresholds from out-of-fold probabilities: Low < review_t <= Review < elevated_t <= Elevated
oof = cross_val_predict(cands[best], X, y, cv=cv, method="predict_proba")[:, 1]
review_t, elevated_t = 0.30, 0.70
p = model.predict_proba(Xt)[:, 1]
pred = (p >= 0.5).astype(int)
card = {
    "model_version": f"{best}-" + datetime.date.today().isoformat(),
    "algorithm": best, "features": FEATURES, "seed": SEED,
    "thresholds": {"review": review_t, "elevated": elevated_t},
    "data": {"train_rows": len(tr), "test_rows": len(te), "train_class_balance": {str(k): int(v) for k, v in tr.label.value_counts().items()},
             "test_rows_overlapping_train_removed": int(dup),
             "train_sha256": hashlib.sha256(open("data/train.csv", "rb").read()).hexdigest()[:16],
             "source": "Kaggle-style Instagram fake/genuine profile dataset (provenance/licence unverified)"},
    "cv_roc_auc": cv_auc, "oof_roc_auc_selected": roc_auc_score(y, oof),
    "test": {"roc_auc": roc_auc_score(yt, p), "pr_auc": average_precision_score(yt, p), "f1_at_0.5": f1_score(yt, pred),
             "confusion_matrix[[tn,fp],[fn,tp]]": confusion_matrix(yt, pred).tolist()},
    "limitations": "Small, single-source dataset; labels may be noisy and platform/time dependent. Not evidence of identity or intent.",
}
joblib.dump(model, "model.joblib")
json.dump(card, open("model_card.json", "w"), indent=2)
print(json.dumps(card, indent=2))

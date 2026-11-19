"""Session 11: does a trained model beat the app's AverageCyclePredictor? Plus population defaults.

Candidates (all predictions rounded to whole days with Kotlin's roundToInt rule):
  A. AverageCyclePredictor (Python copy in baseline.py) - the baseline.
  B. Blend: (n * mean(last <=6 valid lengths) + k * P) / (n + k), P = train median length.
  C. Ridge on the last N valid lengths (h1 = most recent, with intercept) when n >= N, else B.

k, N and alpha are tuned with 5-fold GroupKFold over the 127 train people only.
The 32 test people are scored once at the end.

Needs resources/processed/cycles_clean.csv and split.csv (run load_data.py, then examples.py).
Run from the repo root or from inside predictions/:
    python predictions/model.py
"""

import json

import numpy as np
import pandas as pd
from sklearn.linear_model import Ridge
from sklearn.model_selection import GroupKFold

from baseline import CLOSE_DAYS, average_predict, history_of, num, round_half_up
from examples import HISTORY_COLUMNS, MAX_HISTORY, build_examples, split_people
from load_data import PROCESSED

N_FOLDS = 5
K_GRID = np.round(np.arange(0, 5.0001, 0.25), 2)
RIDGE_N_GRID = [3, 4, 5, 6]
ALPHA_GRID = np.logspace(-2, 2, 9)   # 0.01 ... 100

LUTEAL_MIN, LUTEAL_MAX = 7, 20       # plausible luteal lengths kept for the default

# Decision rule, fixed before scoring the test people.
RIDGE_MARGIN_DAYS = 0.1


# ---------- candidates ----------

def blend_raw(history: list, k: float, p: float) -> float:
    """Unrounded blend of her mean of the last <=6 valid lengths with the population value P."""
    recent = history[-MAX_HISTORY:]
    n = len(recent)
    if n == 0:
        return p
    return (n * (sum(recent) / n) + k * p) / (n + k)


def predict_blend(examples: pd.DataFrame, k: float, p: float) -> np.ndarray:
    return np.array([round_half_up(blend_raw(h, k, p)) for h in examples["history"]])


def ridge_features(examples: pd.DataFrame, n_lags: int) -> np.ndarray:
    return examples[HISTORY_COLUMNS[:n_lags]].to_numpy(dtype=float)


def fit_ridge(examples: pd.DataFrame, n_lags: int, alpha: float) -> Ridge:
    usable = examples[examples["n_history"] >= n_lags]
    return Ridge(alpha=alpha, fit_intercept=True).fit(ridge_features(usable, n_lags), usable["target"])


def predict_ridge_or_blend(examples, model, n_lags, k, p) -> np.ndarray:
    """C: ridge where n >= N, blend B elsewhere."""
    pred = predict_blend(examples, k, p)
    use = (examples["n_history"] >= n_lags).to_numpy()
    if use.any():
        raw = model.predict(ridge_features(examples[use], n_lags))
        pred[use] = [round_half_up(x) for x in raw]
    return pred


def population_value(examples: pd.DataFrame) -> float:
    """P = median cycle length of the given people (every valid cycle is one example's target)."""
    return float(examples["target"].median())


# ---------- tuning (train people only) ----------

def tune(train: pd.DataFrame) -> dict:
    folds = list(GroupKFold(n_splits=N_FOLDS).split(train, groups=train["person_id"]))

    # k for the blend: pooled out-of-fold MAE over every prediction point.
    k_errors = {k: [] for k in K_GRID}
    ridge_errors = {(n, a): [] for n in RIDGE_N_GRID for a in ALPHA_GRID}
    for fit_idx, val_idx in folds:
        fit, val = train.iloc[fit_idx], train.iloc[val_idx]
        p = population_value(fit)
        for k in K_GRID:
            k_errors[k].extend(np.abs(predict_blend(val, k, p) - val["target"].to_numpy()))
        for n_lags in RIDGE_N_GRID:
            val_n = val[val["n_history"] >= n_lags]
            for alpha in ALPHA_GRID:
                model = fit_ridge(fit, n_lags, alpha)
                pred = [round_half_up(x) for x in model.predict(ridge_features(val_n, n_lags))]
                ridge_errors[(n_lags, alpha)].extend(np.abs(np.array(pred) - val_n["target"].to_numpy()))

    k_curve = {float(k): float(np.mean(e)) for k, e in k_errors.items()}
    best_k = min(k_curve, key=lambda k: (k_curve[k], k))
    ridge_cv = [
        {"N": n, "alpha": float(a), "cv_mae_days": float(np.mean(e)), "n_points": len(e)}
        for (n, a), e in ridge_errors.items()
    ]
    best_ridge = min(ridge_cv, key=lambda r: r["cv_mae_days"])
    return {"k_curve": k_curve, "k": best_k, "ridge_cv": ridge_cv, "ridge": best_ridge}


# ---------- scoring ----------

BUCKETS = [("0", 0), ("1", 1), ("2", 2), ("3", 3), ("4", 4), ("5", 5), ("6+", 6)]


def metrics(pred: np.ndarray, examples: pd.DataFrame) -> dict:
    target = examples["target"].to_numpy()
    error = pred - target
    abs_error = np.abs(error)

    # App range rule around this prediction: +/-3 with < 3 cycles, else her SD clamped to [min, 7].
    half_min2 = np.array([average_predict(h)[1] for h in examples["history"]])
    half_min3 = np.maximum(half_min2, 3)   # clamp(sd, 3, 7) == max(clamp(sd, 2, 7), 3)

    def coverage(half):
        inside = (target >= pred - half) & (target <= pred + half)
        return {"coverage_pct": num(100 * inside.mean()), "mean_width_days": num((2 * half).mean())}

    by_history = []
    n_hist = examples["n_history"].to_numpy()
    for label, n in BUCKETS:
        mask = n_hist == n
        by_history.append({
            "n_history": label,
            "count": int(mask.sum()),
            "mae_days": num(abs_error[mask].mean()) if mask.any() else None,
        })

    return {
        "n_points": int(len(target)),
        "mae_days": num(abs_error.mean()),
        "median_abs_error_days": num(np.median(abs_error)),
        "pct_within_2_days": num(100 * (abs_error <= CLOSE_DAYS).mean()),
        "bias_days": num(error.mean()),
        "by_n_history": by_history,
        "range_min2": coverage(half_min2),
        "range_min3": coverage(half_min3),
    }


def mae_where(pred, examples, mask) -> float:
    return float(np.abs(pred[mask] - examples["target"].to_numpy()[mask]).mean())


# ---------- defaults (train people only) ----------

def describe(values: pd.Series) -> dict:
    return {"median": num(values.median()), "mean": num(values.mean()), "n": int(len(values))}


def population_defaults(cycles: pd.DataFrame, train_ids: set) -> dict:
    train = cycles[cycles["valid"] & cycles["person_id"].isin(train_ids)]

    period = train["period_length"].dropna()

    luteal_all = train["luteal_length"]
    luteal_present = luteal_all.dropna()
    luteal_kept = luteal_present[luteal_present.between(LUTEAL_MIN, LUTEAL_MAX)]

    return {
        "cycle_length": describe(train["cycle_length"]),
        "period_length": {**describe(period), "dropped_missing": int(train["period_length"].isna().sum())},
        "luteal_length": {
            **describe(luteal_kept),
            "kept_range_days": [LUTEAL_MIN, LUTEAL_MAX],
            "dropped_missing": int(luteal_all.isna().sum()),
            "dropped_below_range": int((luteal_present < LUTEAL_MIN).sum()),
            "dropped_above_range": int((luteal_present > LUTEAL_MAX).sum()),
        },
    }


def variance_ratio(cycles: pd.DataFrame, ids: set) -> dict:
    """Rough guide for k: within-person SD^2 / between-person SD^2 (same SDs as explore.py)."""
    lengths = cycles[cycles["valid"] & cycles["person_id"].isin(ids)].groupby("person_id")["cycle_length"]
    within, between = lengths.std().median(), lengths.mean().std()
    return {"within_sd": num(within), "between_sd": num(between), "ratio": num(within ** 2 / between ** 2)}


# ---------- printing ----------

def fmt(x, digits=2, suffix=""):
    return "n/a" if x is None else f"{x:.{digits}f}{suffix}"


def print_results(results: dict) -> None:
    names = list(results)
    print(f"\n{'':24}" + "".join(f"{n:>14}" for n in names))
    rows = [
        ("MAE (days)", "mae_days", 2, ""),
        ("Median |error| (days)", "median_abs_error_days", 2, ""),
        (f"Within {CLOSE_DAYS} days", "pct_within_2_days", 1, "%"),
        ("Bias (pred - actual)", "bias_days", 2, ""),
    ]
    for label, key, d, s in rows:
        print(f"{label:24}" + "".join(f"{fmt(results[n][key], d, s):>14}" for n in names))

    print("\nMAE by history length:")
    print(f"{'n_history':>9} {'count':>6}" + "".join(f"{n:>14}" for n in names))
    for i, (label, _) in enumerate(BUCKETS):
        count = results[names[0]]["by_n_history"][i]["count"]
        print(f"{label:>9} {count:>6}" + "".join(
            f"{fmt(results[n]['by_n_history'][i]['mae_days']):>14}" for n in names))

    print("\nRange coverage (app rule around each prediction):")
    for rng, label in [("range_min2", "min 2"), ("range_min3", "min 3")]:
        print(f"  {label}: " + "   ".join(
            f"{n}: {fmt(results[n][rng]['coverage_pct'], 1, '%')} "
            f"(width {fmt(results[n][rng]['mean_width_days'])})" for n in names))


def main() -> None:
    cycles = pd.read_csv(PROCESSED / "cycles_clean.csv")
    split = pd.read_csv(PROCESSED / "split.csv")

    # Same split as session 10: the saved file must equal what examples.split_people() gives.
    pd.testing.assert_frame_equal(split, split_people(cycles))
    counts = split["split"].value_counts()
    print(f"split: {counts['train']} train / {counts['test']} test people (same as session 10)")

    examples = build_examples(cycles, split)
    examples["history"] = examples.apply(history_of, axis=1)   # most recent last
    train = examples[examples["split"] == "train"].reset_index(drop=True)
    test = examples[examples["split"] == "test"].reset_index(drop=True)
    train_ids = set(split.loc[split["split"] == "train", "person_id"])

    # ---- tuning on train people only ----
    ratio = variance_ratio(cycles, train_ids)
    tuned = tune(train)
    k = tuned["k"]
    n_lags, alpha = tuned["ridge"]["N"], tuned["ridge"]["alpha"]

    print(f"\nCV curve for k ({N_FOLDS}-fold GroupKFold, train people, all points):")
    for kk, mae in tuned["k_curve"].items():
        print(f"  k={kk:4.2f}  MAE {mae:.4f}" + ("   <- best" if kk == k else ""))
    print(f"chosen k = {k}  (guide: within SD^2 / between SD^2 = "
          f"{ratio['within_sd']:.2f}^2 / {ratio['between_sd']:.2f}^2 = {ratio['ratio']:.2f})")

    print("\nRidge CV (best alpha per N, points with n >= N):")
    for n in RIDGE_N_GRID:
        best = min((r for r in tuned["ridge_cv"] if r["N"] == n), key=lambda r: r["cv_mae_days"])
        print(f"  N={n}: alpha={best['alpha']:g}  MAE {best['cv_mae_days']:.4f}  ({best['n_points']} points)")
    print(f"chosen N = {n_lags}, alpha = {alpha:g}")

    # ---- refit on all train people ----
    p = population_value(train)
    ridge = fit_ridge(train, n_lags, alpha)
    weights = {f"h{i + 1}": float(w) for i, w in enumerate(ridge.coef_)}
    print(f"\nP (train median cycle length) = {p:g}")
    print("Ridge weights: " + ", ".join(f"{name}={w:.4f}" for name, w in weights.items())
          + f", intercept={ridge.intercept_:.4f} (sum of weights {sum(weights.values()):.4f})")

    # ---- score once on the test people ----
    preds = {
        "A_average": np.array([average_predict(h)[0] for h in test["history"]]),
        "B_blend": predict_blend(test, k, p),
        "C_ridge": predict_ridge_or_blend(test, ridge, n_lags, k, p),
    }
    results = {name: metrics(pred, test) for name, pred in preds.items()}
    print(f"\n=== TEST people: {test['person_id'].nunique()} people, {len(test)} points ===")
    print_results(results)

    ridge_points = (test["n_history"] >= n_lags).to_numpy()
    mae_b_ridge_pts = mae_where(preds["B_blend"], test, ridge_points)
    mae_c_ridge_pts = mae_where(preds["C_ridge"], test, ridge_points)
    print(f"\nOn test points with n >= {n_lags} ({int(ridge_points.sum())}): "
          f"B {mae_b_ridge_pts:.3f}, C {mae_c_ridge_pts:.3f}")

    # ---- decision rule ----
    mae_a, mae_b, mae_c = (results[n]["mae_days"] for n in ["A_average", "B_blend", "C_ridge"])
    if mae_b_ridge_pts - mae_c_ridge_pts >= RIDGE_MARGIN_DAYS and mae_c <= mae_b:
        ship = "C_ridge"
        reason = (f"C beats B by {mae_b_ridge_pts - mae_c_ridge_pts:.3f} days on n >= {n_lags} points "
                  f"(>= {RIDGE_MARGIN_DAYS}) and is not worse overall ({mae_c:.3f} vs {mae_b:.3f})")
    elif mae_b < mae_a:
        ship = "B_blend"
        reason = (f"C beats B by only {mae_b_ridge_pts - mae_c_ridge_pts:.3f} days on n >= {n_lags} points "
                  f"(needs {RIDGE_MARGIN_DAYS}) or is worse overall, and B beats A overall "
                  f"({mae_b:.3f} vs {mae_a:.3f})")
    else:
        ship = "A_average"
        reason = f"neither C nor B clears its bar; B {mae_b:.3f} vs A {mae_a:.3f} overall"
    print(f"\nDECISION: ship {ship} - {reason}")

    # ---- population defaults ----
    defaults = population_defaults(cycles, train_ids)
    print("\nPopulation defaults (train people):")
    for name, d in defaults.items():
        print(f"  {name}: median {fmt(d['median'], 1)}, mean {fmt(d['mean'], 2)} (n={d['n']})")
    lut = defaults["luteal_length"]
    print(f"  luteal dropped: {lut['dropped_missing']} missing, {lut['dropped_below_range']} below "
          f"{LUTEAL_MIN}, {lut['dropped_above_range']} above {LUTEAL_MAX}")
    print(f"  period dropped: {defaults['period_length']['dropped_missing']} missing")

    out = {
        "split": {"train_people": int(counts["train"]), "test_people": int(counts["test"])},
        "rounding": "every prediction rounded with floor(x + 0.5), matching Kotlin roundToInt",
        "tuning": {
            "method": f"{N_FOLDS}-fold GroupKFold by person, train people only, MAE",
            "k_grid": [float(x) for x in K_GRID],
            "k_cv_curve": tuned["k_curve"],
            "chosen_k": k,
            "variance_ratio_guide": ratio,
            "ridge_cv": tuned["ridge_cv"],
            "chosen_N": n_lags,
            "chosen_alpha": alpha,
        },
        "P_train_median_cycle_length": p,
        "ridge": {"N": n_lags, "alpha": alpha, "weights": weights, "intercept": float(ridge.intercept_),
                  "features": "h1 = most recent previous valid cycle length, h2 the one before, ..."},
        "test_metrics": results,
        "test_mae_on_points_n_ge_N": {"N": n_lags, "n_points": int(ridge_points.sum()),
                                      "B_blend": mae_b_ridge_pts, "C_ridge": mae_c_ridge_pts},
        "decision": {
            "rule": f"Ship C if it beats B by >= {RIDGE_MARGIN_DAYS} day MAE on test points with n >= N "
                    "and is not worse than B overall; else B if it beats A overall; else A.",
            "ship": ship,
            "reason": reason,
        },
        "population_defaults_train": defaults,
    }
    out_path = PROCESSED / "model_results.json"
    with open(out_path, "w") as f:
        json.dump(out, f, indent=2)
    print("\nsaved", out_path)


if __name__ == "__main__":
    main()

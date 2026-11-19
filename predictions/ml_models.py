"""Session 11b: linear ML models for cycle length, period length, ovulation day and the range.

For every cycle, features come only from the person's earlier cycles (what the app knows then).
Alphas are tuned with 5-fold GroupKFold over the 127 train people; the 32 test people are
scored once against the app's rules. The models are exported to
app/src/main/assets/cycled_model.json, with test cases in app/src/test/resources/ml_cases.json.
Inference lives in ml_inference.py and uses only the exported JSON.

Needs resources/processed/cycles_clean.csv and split.csv (run load_data.py, then examples.py).
Run from the repo root or from inside predictions/:
    python predictions/ml_models.py
"""

import json
import math

import numpy as np
import pandas as pd
from sklearn.linear_model import QuantileRegressor, Ridge
from sklearn.model_selection import GroupKFold
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler

from baseline import CLOSE_DAYS, average_predict, num
from examples import MAX_HISTORY, split_people
from load_data import PROCESSED, ROOT, VALID_MAX, VALID_MIN
from ml_inference import compute_features, predict, round_half_up

MODEL_JSON = ROOT / "app" / "src" / "main" / "assets" / "cycled_model.json"
CASES_JSON = ROOT / "app" / "src" / "test" / "resources" / "ml_cases.json"
RESULTS_JSON = PROCESSED / "ml_results.json"
MODEL_VERSION = 1

FEATURES = ["n_cycles", "mean_cycle", "last_cycle", "sd_cycle", "mean_period", "last_period"]

# The app's rules (CycleDefaults after session 12).
RULE_CYCLE_DAYS = 29
RULE_PERIOD_DAYS = 5
RULE_LUTEAL_DAYS = 13        # app: ovulation date = next start - 13
RULE_MIN_HALF_WIDTH = 3

N_FOLDS = 5
RIDGE_ALPHAS = np.logspace(-3, 3, 13)
QUANTILE_ALPHAS = [0.0, 1e-4, 1e-3, 1e-2, 1e-1]
QUANTILES = {"range_low": 0.1, "range_high": 0.9}

# Ovulation/luteal models train only on rows with a plausible luteal phase (as in session 11's
# luteal default); every test row with an ovulation day is still scored.
LUTEAL_TRAIN_RANGE = (7, 20)
CLAMPS = {"cycle_days": [VALID_MIN, VALID_MAX], "period_days": [1, 10], "ovulation_min_days_before_next_start": 2}
RANGE_MIN_HALF_WIDTH = 3
SHIP_MARGIN_DAYS = 0.05
COVERAGE_TARGET = (80.0, 85.0)

BUCKETS = [("0", [0]), ("1", [1]), ("2", [2]), ("3-5", [3, 4, 5]), ("6", [6])]


# ---------- data ----------

def population_fallbacks(cycles: pd.DataFrame, train_ids: set) -> dict:
    """Train-people medians: cycle length, period length, and within-person SD (divide by n, as in features)."""
    train = cycles[cycles["valid"] & cycles["person_id"].isin(train_ids)]
    sds = train.groupby("person_id")["cycle_length"].agg(lambda s: s.std(ddof=0) if len(s) >= 2 else np.nan)
    return {
        "cycle_length": float(train["cycle_length"].median()),
        "period_length": float(train["period_length"].median()),
        "sd_cycle": float(sds.dropna().median()),
    }


def build_rows(cycles: pd.DataFrame, split: pd.DataFrame, skeleton: dict) -> pd.DataFrame:
    """One row per valid cycle, with features from earlier cycles only, targets and the app's rule outputs.

    A jump of more than 1 in cycle_number resets the history, as in examples.build_examples.
    """
    split_of = dict(zip(split["person_id"], split["split"]))
    rows = []
    ordered = cycles.sort_values(["person_id", "cycle_number"], kind="stable")
    for person_id, person in ordered.groupby("person_id", sort=True):
        history = []          # (cycle_length, period_length or None), oldest first
        previous = None
        for c in person.itertuples(index=False):
            if previous is not None and c.cycle_number - previous > 1:
                history = []
            previous = c.cycle_number
            if not c.valid:
                continue

            features = compute_features(history, skeleton)
            used = [(cl, pl) for cl, pl in history if VALID_MIN <= cl <= VALID_MAX][-MAX_HISTORY:]
            lengths = [cl for cl, _ in used]
            periods = [pl for _, pl in used if pl is not None]
            rule_cycle = average_predict(lengths)[0] if lengths else RULE_CYCLE_DAYS
            rule_half = max(average_predict(lengths)[1], RULE_MIN_HALF_WIDTH)

            period = None if pd.isna(c.period_length) else float(c.period_length)
            ovulation = None if pd.isna(c.ovulation_day) else float(c.ovulation_day)
            rows.append({
                "person_id": person_id,
                "split": split_of[person_id],
                "cycle_number": c.cycle_number,
                **features,
                "history": list(history),
                "cycle_length": float(c.cycle_length),
                "period_length": period,
                "ovulation_day": ovulation,
                "luteal_gap": None if ovulation is None else c.cycle_length - ovulation,
                "rule_cycle": rule_cycle,
                "rule_period": round_half_up(sum(periods) / len(periods)) if periods else RULE_PERIOD_DAYS,
                # App: ovulation date = next start - 13 = start + rule_cycle - 13 = cycle day rule_cycle - 12.
                "rule_ovulation": rule_cycle + 1 - RULE_LUTEAL_DAYS,
                "rule_low": rule_cycle - rule_half,
                "rule_high": rule_cycle + rule_half,
            })
            history.append((int(c.cycle_length), None if period is None else int(period)))
    rows = pd.DataFrame(rows)
    for col in ["period_length", "ovulation_day", "luteal_gap"]:
        rows[col] = rows[col].astype(float)
    return rows


def plausible_luteal(rows: pd.DataFrame) -> pd.Series:
    return rows["luteal_gap"].between(*LUTEAL_TRAIN_RANGE)


# ---------- fitting ----------

def ridge(alpha):
    return make_pipeline(StandardScaler(), Ridge(alpha=alpha))


def quantile(q, alpha):
    return make_pipeline(StandardScaler(), QuantileRegressor(quantile=q, alpha=alpha, solver="highs"))


def fit(make, rows: pd.DataFrame, target: str):
    return make().fit(rows[FEATURES].to_numpy(), rows[target].to_numpy())


def raw_space(pipeline) -> dict:
    """Undo the standardisation: intercept + sum(w * raw_feature)."""
    scaler, model = pipeline[0], pipeline[-1]
    weights = model.coef_ / scaler.scale_
    intercept = float(model.intercept_ - np.sum(weights * scaler.mean_))
    return {"intercept": intercept, "weights": [float(w) for w in weights]}


def pinball(y, pred, q):
    diff = y - pred
    return float(np.mean(np.maximum(q * diff, (q - 1) * diff)))


def tune(train: pd.DataFrame) -> dict:
    folds = list(GroupKFold(n_splits=N_FOLDS).split(train, groups=train["person_id"]))
    has_period = train["period_length"].notna()
    has_ov = train["ovulation_day"].notna()
    ov_fit = has_ov & plausible_luteal(train)

    def ridge_cv(target, fit_mask, val_mask, predict_fn=None):
        curve = {}
        for alpha in RIDGE_ALPHAS:
            errors = []
            for fit_idx, val_idx in folds:
                fit_rows = train.iloc[fit_idx][fit_mask.iloc[fit_idx].to_numpy()]
                val_rows = train.iloc[val_idx][val_mask.iloc[val_idx].to_numpy()]
                if predict_fn is None:
                    pred = fit(lambda: ridge(alpha), fit_rows, target).predict(val_rows[FEATURES].to_numpy())
                else:
                    pred = predict_fn(alpha, train.iloc[fit_idx], fit_rows, val_rows)
                rounded = np.array([round_half_up(x) for x in pred])
                errors.extend(np.abs(rounded - val_rows[val_target(target)].to_numpy()))
            curve[float(alpha)] = float(np.mean(errors))
        best = min(curve, key=curve.get)
        return {"cv_mae_curve": curve, "alpha": best, "cv_mae_days": curve[best]}

    def val_target(target):
        return "ovulation_day" if target == "luteal_gap" else target

    everything = pd.Series(True, index=train.index)
    out = {
        "cycle_length": ridge_cv("cycle_length", everything, everything),
        "period_length": ridge_cv("period_length", has_period, has_period),
        "ovulation_direct": ridge_cv("ovulation_day", ov_fit, has_ov),
    }
    cycle_alpha = out["cycle_length"]["alpha"]

    def via_luteal(alpha, fold_all, fold_ov, val_rows):
        cycle_model = fit(lambda: ridge(cycle_alpha), fold_all, "cycle_length")
        luteal_model = fit(lambda: ridge(alpha), fold_ov, "luteal_gap")
        x = val_rows[FEATURES].to_numpy()
        return cycle_model.predict(x) - luteal_model.predict(x)

    out["ovulation_via_luteal"] = ridge_cv("luteal_gap", ov_fit, has_ov, via_luteal)

    for name, q in QUANTILES.items():
        curve = {}
        for alpha in QUANTILE_ALPHAS:
            losses = []
            for fit_idx, val_idx in folds:
                model = fit(lambda: quantile(q, alpha), train.iloc[fit_idx], "cycle_length")
                val = train.iloc[val_idx]
                losses.append(pinball(val["cycle_length"].to_numpy(), model.predict(val[FEATURES].to_numpy()), q) * len(val))
            curve[float(alpha)] = float(sum(losses) / len(train))
        best = min(curve, key=curve.get)
        out[name] = {"quantile": q, "cv_pinball_curve": curve, "alpha": best, "cv_pinball": curve[best]}
    return out


# ---------- scoring ----------

def error_metrics(pred, target, n_cycles) -> dict:
    pred, target, n_cycles = np.asarray(pred, float), np.asarray(target, float), np.asarray(n_cycles)
    err = pred - target
    out = {
        "n": int(len(err)),
        "mae_days": num(np.abs(err).mean()),
        "median_abs_error_days": num(np.median(np.abs(err))),
        "pct_within_2_days": num(100 * (np.abs(err) <= CLOSE_DAYS).mean()),
        "bias_days": num(err.mean()),
        "by_n_cycles": [],
    }
    for label, values in BUCKETS:
        mask = np.isin(n_cycles, values)
        out["by_n_cycles"].append({
            "n_cycles": label, "count": int(mask.sum()),
            "mae_days": num(np.abs(err[mask]).mean()) if mask.any() else None,
        })
    return out


def range_metrics(low, high, target, n_cycles) -> dict:
    low, high, target, n_cycles = (np.asarray(a, float) for a in (low, high, target, n_cycles))
    inside = (target >= low) & (target <= high)
    width = high - low
    out = {"coverage_pct": num(100 * inside.mean()), "mean_width_days": num(width.mean()), "by_n_cycles": []}
    for label, values in BUCKETS:
        mask = np.isin(n_cycles, values)
        out["by_n_cycles"].append({
            "n_cycles": label, "count": int(mask.sum()),
            "coverage_pct": num(100 * inside[mask].mean()) if mask.any() else None,
            "mean_width_days": num(width[mask].mean()) if mask.any() else None,
        })
    return out


def fmt(x, d=2, s=""):
    return "n/a" if x is None else f"{x:.{d}f}{s}"


def print_comparison(title: str, model: dict, rule: dict) -> None:
    print(f"\n--- {title} ({model['n']} test points) ---")
    print(f"{'':22}{'model':>9}{'rule':>9}")
    for label, key, d, s in [("MAE", "mae_days", 2, ""), ("Median |error|", "median_abs_error_days", 2, ""),
                             ("Within 2 days", "pct_within_2_days", 1, "%"), ("Bias", "bias_days", 2, "")]:
        print(f"{label:22}{fmt(model[key], d, s):>9}{fmt(rule[key], d, s):>9}")
    print(f"{'n_cycles':>9}{'count':>7}{'model':>9}{'rule':>9}")
    for m, r in zip(model["by_n_cycles"], rule["by_n_cycles"]):
        print(f"{m['n_cycles']:>9}{m['count']:>7}{fmt(m['mae_days']):>9}{fmt(r['mae_days']):>9}")


# ---------- main ----------

def main() -> None:
    cycles = pd.read_csv(PROCESSED / "cycles_clean.csv")
    split = pd.read_csv(PROCESSED / "split.csv")
    pd.testing.assert_frame_equal(split, split_people(cycles))
    train_ids = set(split.loc[split["split"] == "train", "person_id"])
    print(f"split: {len(train_ids)} train / {len(split) - len(train_ids)} test people (same as sessions 10-11)")

    fallbacks = population_fallbacks(cycles, train_ids)
    print(f"population fallbacks (train people): cycle {fallbacks['cycle_length']:g}, "
          f"period {fallbacks['period_length']:g}, sd_cycle {fallbacks['sd_cycle']:.4f} "
          "(median within-person SD, divide by n)")
    skeleton = {"valid_cycle_days": [VALID_MIN, VALID_MAX], "max_cycles_used": MAX_HISTORY, "fallbacks": fallbacks}

    rows = build_rows(cycles, split, skeleton)
    train = rows[rows["split"] == "train"].reset_index(drop=True)
    test = rows[rows["split"] == "test"].reset_index(drop=True)

    # ---- data notes ----
    valid = cycles[cycles["valid"]]
    luteal_mismatch = valid.dropna(subset=["ovulation_day", "luteal_length"])
    luteal_mismatch = int((luteal_mismatch["cycle_length"] - luteal_mismatch["ovulation_day"]
                           != luteal_mismatch["luteal_length"]).sum())
    has_ov = rows["ovulation_day"].notna()
    data_notes = {
        "rows": int(len(rows)), "train_rows": int(len(train)), "test_rows": int(len(test)),
        "missing_period_length": int(rows["period_length"].isna().sum()),
        "missing_ovulation_day": int((~has_ov).sum()),
        "luteal_length_not_equal_cycle_minus_ovulation_day": luteal_mismatch,
        "luteal_gap_outside_7_20": int((has_ov & ~plausible_luteal(rows)).sum()),
        "train_ovulation_rows_used": int((train["ovulation_day"].notna() & plausible_luteal(train)).sum()),
        "train_ovulation_rows_excluded_implausible_luteal":
            int((train["ovulation_day"].notna() & ~plausible_luteal(train)).sum()),
    }
    print("data notes:", data_notes)

    # ---- tuning ----
    tuned = tune(train)
    ovulation_method = ("luteal" if tuned["ovulation_via_luteal"]["cv_mae_days"]
                        < tuned["ovulation_direct"]["cv_mae_days"] else "direct")
    print("\nCV (train people, 5-fold GroupKFold):")
    for name in ["cycle_length", "period_length", "ovulation_direct", "ovulation_via_luteal"]:
        print(f"  {name:22} alpha={tuned[name]['alpha']:<8g} CV MAE {tuned[name]['cv_mae_days']:.4f}")
    for name in QUANTILES:
        print(f"  {name:22} alpha={tuned[name]['alpha']:<8g} CV pinball {tuned[name]['cv_pinball']:.4f}")
    print(f"ovulation method: {ovulation_method}")

    # ---- final fit on all train people ----
    ov_rows = train[train["ovulation_day"].notna() & plausible_luteal(train)]
    pipes = {
        "cycle_length": fit(lambda: ridge(tuned["cycle_length"]["alpha"]), train, "cycle_length"),
        "period_length": fit(lambda: ridge(tuned["period_length"]["alpha"]),
                             train[train["period_length"].notna()], "period_length"),
    }
    if ovulation_method == "luteal":
        pipes["luteal_length"] = fit(lambda: ridge(tuned["ovulation_via_luteal"]["alpha"]), ov_rows, "luteal_gap")
    else:
        pipes["ovulation_day"] = fit(lambda: ridge(tuned["ovulation_direct"]["alpha"]), ov_rows, "ovulation_day")
    for name, q in QUANTILES.items():
        pipes[name] = fit(lambda: quantile(q, tuned[name]["alpha"]), train, "cycle_length")

    def export(min_half_width: int) -> dict:
        return {
            "version": MODEL_VERSION,
            "day_convention": "cycle day 1 = first day of the period; ovulation date = start + (ovulation_day - 1); "
                              "next start = start + cycle_days; range days are offsets from the start",
            "features": FEATURES,
            "valid_cycle_days": [VALID_MIN, VALID_MAX],
            "max_cycles_used": MAX_HISTORY,
            "fallbacks": fallbacks,
            "ovulation_method": ovulation_method,
            "models": {name: raw_space(p) for name, p in pipes.items()},
            "clamps": CLAMPS,
            "range_min_half_width_days": min_half_width,
        }

    variants = {"quantile_raw": export(0), "quantile_min3": export(RANGE_MIN_HALF_WIDTH)}

    # ---- inference check: JSON-only inference reproduces sklearn on every row ----
    x_all = rows[FEATURES].to_numpy()
    feats = rows[FEATURES].to_dict("records")
    outputs = {v: [predict(f, m) for f in feats] for v, m in variants.items()}
    ref = outputs["quantile_min3"]
    sk_name = {"cycle_raw": "cycle_length", "period_raw": "period_length",
               "range_low_raw": "range_low", "range_high_raw": "range_high"}
    if ovulation_method == "luteal":
        sk_name["luteal_raw"] = "luteal_length"
    else:
        sk_name["ovulation_raw"] = "ovulation_day"
    max_diff = {k: float(np.max(np.abs(np.array([o[k] for o in ref]) - pipes[p].predict(x_all))))
                for k, p in sk_name.items()}
    print("\nJSON inference vs sklearn, max |difference| over all rows:", max_diff)
    assert max(max_diff.values()) < 1e-9, "exported inference does not reproduce sklearn"

    # ---- score once on the test people ----
    is_test = (rows["split"] == "test").to_numpy()
    out_test = {v: [o for o, t in zip(outs, is_test) if t] for v, outs in outputs.items()}
    n_c = test["n_cycles"].to_numpy()
    m3 = out_test["quantile_min3"]
    p_mask = test["period_length"].notna().to_numpy()
    o_mask = test["ovulation_day"].notna().to_numpy()

    results = {
        "cycle_length": {
            "model": error_metrics([o["cycle_days"] for o in m3], test["cycle_length"], n_c),
            "rule": error_metrics(test["rule_cycle"], test["cycle_length"], n_c),
        },
        "period_length": {
            "model": error_metrics(np.array([o["period_days"] for o in m3])[p_mask],
                                   test["period_length"][p_mask], n_c[p_mask]),
            "rule": error_metrics(test["rule_period"][p_mask], test["period_length"][p_mask], n_c[p_mask]),
        },
        "ovulation_day": {
            "model": error_metrics(np.array([o["ovulation_day"] for o in m3])[o_mask],
                                   test["ovulation_day"][o_mask], n_c[o_mask]),
            "rule": error_metrics(test["rule_ovulation"][o_mask], test["ovulation_day"][o_mask], n_c[o_mask]),
            # For reference only: the rule with next start - 14, i.e. Marquette's median luteal of 13 days
            # counted in the app's convention.
            "rule_next_start_minus_14": error_metrics(test["rule_ovulation"][o_mask] - 1,
                                                      test["ovulation_day"][o_mask], n_c[o_mask]),
        },
    }
    ranges = {
        v: range_metrics([o["range_low_days"] for o in out_test[v]], [o["range_high_days"] for o in out_test[v]],
                         test["cycle_length"], n_c)
        for v in variants
    }
    ranges["rule"] = range_metrics(test["rule_low"], test["rule_high"], test["cycle_length"], n_c)

    print(f"\n=== TEST people: {test['person_id'].nunique()} people ===")
    for name, r in results.items():
        print_comparison(name, r["model"], r["rule"])
    r14 = results["ovulation_day"]["rule_next_start_minus_14"]
    print(f"  (reference: rule with next start - 14: MAE {r14['mae_days']:.2f}, bias {r14['bias_days']:.2f})")

    print("\n--- range (cycle length) ---")
    print(f"{'':16}{'coverage':>10}{'width':>8}   by n_cycles (coverage / width)")
    for v, r in ranges.items():
        detail = "  ".join(f"{b['n_cycles']}: {fmt(b['coverage_pct'], 0, '%')}/{fmt(b['mean_width_days'], 1)}"
                           for b in r["by_n_cycles"])
        print(f"{v:16}{fmt(r['coverage_pct'], 1, '%'):>10}{fmt(r['mean_width_days'], 2):>8}   {detail}")

    # ---- ship decisions ----
    ships = {}
    for name, r in results.items():
        model_mae, rule_mae = r["model"]["mae_days"], r["rule"]["mae_days"]
        ships[name] = {"ships": model_mae <= rule_mae + SHIP_MARGIN_DAYS,
                       "model_mae": model_mae, "rule_mae": rule_mae, "margin": SHIP_MARGIN_DAYS}
    in_target = [v for v in variants if COVERAGE_TARGET[0] <= ranges[v]["coverage_pct"] <= COVERAGE_TARGET[1]]
    candidates = in_target or [v for v in variants if ranges[v]["coverage_pct"] >= COVERAGE_TARGET[0]] or list(variants)
    range_choice = min(candidates, key=lambda v: (ranges[v]["mean_width_days"], -ranges[v]["coverage_pct"]))
    print("\nShip (model MAE not worse than the rule by more than "
          f"{SHIP_MARGIN_DAYS} days):")
    for name, s in ships.items():
        print(f"  {name:14} {'SHIP' if s['ships'] else 'no'}  (model {s['model_mae']:.3f} vs rule {s['rule_mae']:.3f})")
    print(f"  range: recommend {range_choice} "
          f"({ranges[range_choice]['coverage_pct']:.1f}% coverage, width {ranges[range_choice]['mean_width_days']:.2f})")

    # ---- export ----
    model_json = variants[range_choice]
    MODEL_JSON.parent.mkdir(parents=True, exist_ok=True)
    with open(MODEL_JSON, "w") as f:
        json.dump(model_json, f, indent=2)
        f.write("\n")
    print("\nsaved", MODEL_JSON)

    cases = build_cases(model_json)
    CASES_JSON.parent.mkdir(parents=True, exist_ok=True)
    with open(CASES_JSON, "w") as f:
        json.dump({"model_version": MODEL_VERSION,
                   "note": "history = earlier completed cycles, oldest first: [cycle_length, period_length or null]. "
                           "Expected values come from predictions/ml_inference.py with cycled_model.json.",
                   "cases": cases}, f, indent=2)
        f.write("\n")
    print(f"saved {CASES_JSON} ({len(cases)} cases)")

    with open(RESULTS_JSON, "w") as f:
        json.dump({
            "split": {"train_people": len(train_ids), "test_people": int(len(split) - len(train_ids))},
            "population_fallbacks": fallbacks,
            "day_convention": model_json["day_convention"],
            "rules": {"cycle_default": RULE_CYCLE_DAYS, "period_default": RULE_PERIOD_DAYS,
                      "ovulation": "next start - 13 (cycle day rule_cycle - 12)",
                      "range": "±3 under 3 cycles, else population SD clamped 3-7"},
            "data_notes": data_notes,
            "tuning": tuned,
            "ovulation_method": ovulation_method,
            "inference_vs_sklearn_max_abs_diff": max_diff,
            "test_metrics": results,
            "test_range": ranges,
            "ships": ships,
            "range_choice": range_choice,
            "exported_model": model_json,
        }, f, indent=2)
    print("saved", RESULTS_JSON)


# ---------- test cases for the Kotlin port ----------

CASE_HISTORIES = [
    ("no_history", []),
    ("one_cycle", [(28, 5)]),
    ("one_cycle_period_unknown", [(31, None)]),
    ("one_long_cycle", [(45, 6)]),
    ("two_cycles", [(27, 4), (29, 5)]),
    ("two_short_cycles", [(21, 3), (22, 3)]),
    ("three_identical", [(28, 5), (28, 5), (28, 5)]),
    ("three_irregular", [(20, 4), (45, 7), (20, 4)]),
    ("four_varying", [(24, 5), (32, 5), (28, 6), (36, 5)]),
    ("six_regular", [(29, 5), (29, 5), (29, 6), (29, 5), (29, 5), (29, 6)]),
    ("six_short", [(23, 4), (24, 4), (22, 4), (25, 4), (23, 4), (24, 4)]),
    ("six_long", [(35, 6), (38, 7), (34, 6), (36, 7), (37, 6), (35, 7)]),
    ("six_very_irregular_some_periods_unknown", [(22, 4), (40, None), (25, 5), (38, None), (21, 4), (44, 6)]),
    ("ten_regular", [(28, 5)] * 10),
    ("ten_mixed_last_six_used", [(40, 7), (22, 3), (30, 5), (31, 5), (29, 4), (28, 5),
                                 (33, 6), (27, 5), (30, 5), (29, 5)]),
    ("invalid_lengths_dropped", [(90, 5), (27, 5), (10, 2), (30, 6), (29, 5)]),
]


def build_cases(model_json: dict) -> list:
    cases = []
    for name, history in CASE_HISTORIES:
        features = compute_features(history, model_json)
        cases.append({
            "name": name,
            "history": [[c, p] for c, p in history],
            "features": features,
            "expected": predict(features, model_json),
        })
    return cases


if __name__ == "__main__":
    main()

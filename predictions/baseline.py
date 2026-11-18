"""Score the app's current AverageCyclePredictor on real cycle data.

Needs resources/processed/cycles_clean.csv, split.csv and examples.csv
(run load_data.py, then examples.py, first).
Run from the repo root or from inside predictions/:
    python predictions/baseline.py
"""

import json
import math
import sys

import numpy as np
import pandas as pd

from examples import HISTORY_COLUMNS, MAX_HISTORY
from load_data import PROCESSED, VALID_MAX, VALID_MIN

PLACEHOLDER_LENGTH = 28   # CycleDefaults.CYCLE_LENGTH_DAYS
MAX_CYCLES_USED = 6       # CycleDefaults.MAX_CYCLES_USED
CLOSE_DAYS = 2            # "close" = |error| <= 2 days


def round_half_up(x: float) -> int:
    """Kotlin's Double.roundToInt(): ties round toward positive infinity (Python's round() goes to even)."""
    return math.floor(x + 0.5)


# Python copy of AverageCyclePredictor in
# app/src/main/java/com/example/cycled/domain/CyclePredictor.kt (lines 52-84),
# with constants from app/src/main/java/com/example/cycled/domain/CycleDefaults.kt (lines 8-13).
# The Kotlin takes start dates; here history is the list of cycle lengths between them,
# most recent last. predicted start - last start = predicted length, so errors in days match.
#   1. Keep lengths in MIN_VALID_CYCLE_DAYS..MAX_VALID_CYCLE_DAYS = 15..60, inclusive (line 59).
#   2. Then keep only the last MAX_CYCLES_USED = 6 of those (line 60).
#   3. No lengths left: predict CYCLE_LENGTH_DAYS = 28 (lines 62-63).
#      Otherwise predict mean(lengths).roundToInt(), so .5 rounds up (line 65).
#   4. Half-width: 3 if fewer than 3 lengths (lines 68-69). Otherwise the POPULATION SD
#      (divide by n, line 72), rounded with roundToInt() and clamped to 2..7 (line 73).
#   5. The range is symmetric: earliest = predicted - halfWidth, latest = predicted + halfWidth
#      (lines 79-80), so earliest_offset == latest_offset == halfWidth.
#   6. based_on = number of lengths used after steps 1-2 (line 81).
def average_predict(history: list) -> tuple:
    """Return (predicted_length, earliest_offset, latest_offset, based_on) for a list of past lengths."""
    lengths = [int(x) for x in history if VALID_MIN <= x <= VALID_MAX]
    lengths = lengths[-MAX_CYCLES_USED:]

    if not lengths:
        predicted = PLACEHOLDER_LENGTH
    else:
        predicted = round_half_up(sum(lengths) / len(lengths))

    if len(lengths) < 3:
        half_width = 3
    else:
        mean = sum(lengths) / len(lengths)
        variance = sum((x - mean) * (x - mean) for x in lengths) / len(lengths)
        half_width = min(max(round_half_up(math.sqrt(variance)), 2), 7)

    return predicted, half_width, half_width, len(lengths)


def self_test() -> None:
    """Cases from app/src/test/java/com/example/cycled/domain/AverageCyclePredictorTest.kt, in lengths.

    The Kotlin tests end every history on Nov 1, 2026, so an expected date converts to an offset
    from Nov 1 (Nov 29 -> 28, Dec 1 -> 30, Dec 2 -> 31, and so on).
    """
    cases = [
        # (name, history oldest first, expected (predicted, earliest_offset, latest_offset, based_on))
        # oneStart_usesPlaceholderLength: Nov 29 / Nov 26 / Dec 2, basedOn 0
        ("oneStart_usesPlaceholderLength", [], (28, 3, 3, 0)),
        # twoStarts_usesTheOneLength: Oct 3 -> Nov 1 is 29 days; Nov 30 / Nov 27 / Dec 3, basedOn 1
        ("twoStarts_usesTheOneLength", [29], (29, 3, 3, 1)),
        # identicalLengths_rangeClampedToMinimumOfTwo: Nov 29 / Nov 27 / Dec 1, basedOn 3
        ("identicalLengths_rangeClampedToMinimumOfTwo", [28, 28, 28], (28, 2, 2, 3)),
        # varyingLengths_rangeIsRoundedStdDev: Dec 1 / Nov 27 / Dec 5, basedOn 4
        # (population SD = sqrt(20) = 4.47 -> 4; sample SD would give 5)
        ("varyingLengths_rangeIsRoundedStdDev", [24, 32, 28, 36], (30, 4, 4, 4)),
        # outlierLength_isIgnored: Nov 29 / Nov 26 / Dec 2, basedOn 2
        ("outlierLength_isIgnored", [28, 90, 28], (28, 3, 3, 2)),
        # onlyOutlierLength_fallsBackToPlaceholder: Nov 29 / Nov 26 / Dec 2, basedOn 0
        ("onlyOutlierLength_fallsBackToPlaceholder", [90], (28, 3, 3, 0)),
        # manyCycles_onlyMostRecentSixUsed: Nov 29 / Nov 27 / Dec 1, basedOn 6
        ("manyCycles_onlyMostRecentSixUsed", [40, 40, 28, 28, 28, 28, 28, 28], (28, 2, 2, 6)),
        # Added: mean 28.5 must round up to 29 (Python's round() would give 28).
        ("added_meanHalfRoundsUp", [28, 29], (29, 3, 3, 2)),
        # Added: population SD exactly 2.5 must round up to 3; mean 27.5 rounds up to 28.
        ("added_sdHalfRoundsUp", [25, 30, 25, 30], (28, 3, 3, 4)),
    ]

    failures = []
    for name, history, expected in cases:
        got = average_predict(history)
        if got != expected:
            failures.append(f"{name}: history={history} expected={expected} got={got}")

    # veryIrregularLengths_rangeClampedToAtMostSeven: the Kotlin only asserts halfWidth <= 7.
    _, _, latest, _ = average_predict([20, 45, 20, 45])
    if not latest <= 7:
        failures.append(f"veryIrregularLengths_rangeClampedToAtMostSeven: halfWidth was {latest}")

    if failures:
        print("SELF-TEST FAILED: average_predict does not match the Kotlin AverageCyclePredictor.")
        for failure in failures:
            print("  -", failure)
        sys.exit(1)
    print(f"self-test passed ({len(cases) + 1} cases)")


def history_of(row) -> list:
    """Earlier cycle lengths for one example row, most recent last (h1 is the most recent)."""
    n = int(row["n_history"])
    return [int(row[f"h{i}"]) for i in range(n, 0, -1)]


def predict_all(examples: pd.DataFrame) -> pd.DataFrame:
    """Add the average predictor's output and error columns to the examples."""
    out = examples.copy()
    results = [average_predict(history_of(row)) for _, row in out.iterrows()]
    out["predicted"] = [r[0] for r in results]
    out["earliest_offset"] = [r[1] for r in results]
    out["latest_offset"] = [r[2] for r in results]
    out["based_on"] = [r[3] for r in results]
    out["error"] = out["predicted"] - out["target"]
    out["abs_error"] = out["error"].abs()
    out["low"] = out["predicted"] - out["earliest_offset"]
    out["high"] = out["predicted"] + out["latest_offset"]
    out["covered"] = (out["target"] >= out["low"]) & (out["target"] <= out["high"])
    out["width"] = out["high"] - out["low"]
    return out


def num(x):
    """Plain float for JSON (None for NaN/missing)."""
    if x is None:
        return None
    x = float(x)
    return None if math.isnan(x) else x


def pct(mask: pd.Series):
    return num(100 * mask.mean()) if len(mask) else None


def score(scored: pd.DataFrame) -> dict:
    """Headline metrics, a by-n_history table, and the last-cycle-only comparison."""
    result = {
        "n_examples": int(len(scored)),
        "n_people": int(scored["person_id"].nunique()),
        "mae_days": num(scored["abs_error"].mean()),
        "median_abs_error_days": num(scored["abs_error"].median()),
        "pct_within_2_days": pct(scored["abs_error"] <= CLOSE_DAYS),
        "mean_error_days": num(scored["error"].mean()),
        "range_coverage_pct": pct(scored["covered"]),
        "mean_range_width_days": num(scored["width"].mean()),
    }

    by_history = []
    for n in range(0, MAX_HISTORY + 1):
        part = scored[scored["n_history"] == n]
        by_history.append({
            "n_history": n,
            "count": int(len(part)),
            "mae_days": num(part["abs_error"].mean()) if len(part) else None,
            "range_coverage_pct": pct(part["covered"]),
        })
    result["by_n_history"] = by_history

    with_history = scored[scored["n_history"] >= 1]
    result["mae_days_excluding_n_history_0"] = num(with_history["abs_error"].mean())

    # Last-cycle-only: predict h1, on exactly the examples that have one.
    last_only_error = (with_history["h1"] - with_history["target"]).abs()
    result["last_cycle_only"] = {
        "n_examples": int(len(with_history)),
        "mae_days_last_cycle_only": num(last_only_error.mean()),
        "mae_days_average_same_examples": num(with_history["abs_error"].mean()),
    }
    return result


def placeholder_preview(scored: pd.DataFrame, train_median: float) -> dict:
    """For n_history = 0 examples: MAE predicting 28 vs predicting the train median."""
    first = scored[scored["n_history"] == 0]
    return {
        "n_examples": int(len(first)),
        "mae_days_placeholder_28": num((PLACEHOLDER_LENGTH - first["target"]).abs().mean()),
        "mae_days_train_median": num((train_median - first["target"]).abs().mean()),
    }


def deviation_correlation(cycles: pd.DataFrame) -> dict:
    """Correlation between consecutive within-person deviations from each person's mean (all people)."""
    valid = cycles[cycles["valid"]].sort_values(["person_id", "cycle_number"], kind="stable").copy()
    valid["deviation"] = valid["cycle_length"] - valid.groupby("person_id")["cycle_length"].transform("mean")
    previous = valid.groupby("person_id")["deviation"].shift(1)
    step = valid.groupby("person_id")["cycle_number"].diff()
    pairs = pd.DataFrame({"current": valid["deviation"], "previous": previous})[step == 1]
    return {
        "description": "descriptive statistic, all people: correlation of consecutive deviations "
                       "from each person's own mean cycle length (consecutive cycle_numbers only)",
        "n_pairs": int(len(pairs)),
        "correlation": num(pairs["current"].corr(pairs["previous"])),
    }


def fmt(x, digits=2, suffix=""):
    return "n/a" if x is None else f"{x:.{digits}f}{suffix}"


def print_score(label: str, s: dict) -> None:
    print(f"\n=== {label}: {s['n_people']} people, {s['n_examples']} examples ===")
    print(f"MAE:                        {fmt(s['mae_days'])} days")
    print(f"Median absolute error:      {fmt(s['median_abs_error_days'])} days")
    print(f"Within {CLOSE_DAYS} days:              {fmt(s['pct_within_2_days'], 1, '%')}")
    print(f"Mean error (pred - actual): {fmt(s['mean_error_days'])} days")
    print(f"Range coverage:             {fmt(s['range_coverage_pct'], 1, '%')}")
    print(f"Mean range width:           {fmt(s['mean_range_width_days'])} days")
    print(f"MAE excluding n_history=0:  {fmt(s['mae_days_excluding_n_history_0'])} days")

    print("\nBy n_history:")
    print(f"{'n_history':>9} {'count':>6} {'MAE':>6} {'coverage':>9}")
    for row in s["by_n_history"]:
        print(f"{row['n_history']:>9} {row['count']:>6} {fmt(row['mae_days']):>6} "
              f"{fmt(row['range_coverage_pct'], 1, '%'):>9}")

    lc = s["last_cycle_only"]
    print(f"\nLast-cycle-only vs average ({lc['n_examples']} examples with n_history >= 1):")
    print(f"  average MAE:          {fmt(lc['mae_days_average_same_examples'])} days")
    print(f"  last-cycle-only MAE:  {fmt(lc['mae_days_last_cycle_only'])} days")


def print_preview(label: str, p: dict, train_median: float) -> None:
    print(f"\nPlaceholder preview, {label} ({p['n_examples']} examples with n_history = 0):")
    print(f"  predict {PLACEHOLDER_LENGTH}:                 MAE {fmt(p['mae_days_placeholder_28'])} days")
    print(f"  predict train median ({train_median:g}): MAE {fmt(p['mae_days_train_median'])} days")


def main() -> None:
    self_test()

    cycles = pd.read_csv(PROCESSED / "cycles_clean.csv")
    split = pd.read_csv(PROCESSED / "split.csv")
    examples = pd.read_csv(PROCESSED / "examples.csv")
    scored = predict_all(examples)

    test = scored[scored["split"] == "test"]
    score_test = score(test)
    score_all = score(scored)

    train_ids = set(split.loc[split["split"] == "train", "person_id"])
    train_cycles = cycles[cycles["valid"] & cycles["person_id"].isin(train_ids)]
    train_median = float(train_cycles["cycle_length"].median())
    preview_test = placeholder_preview(test, train_median)
    preview_all = placeholder_preview(scored, train_median)

    correlation = deviation_correlation(cycles)

    print_score("TEST people (headline)", score_test)
    print_preview("TEST people", preview_test, train_median)
    print_score("ALL people (train + test, for reference)", score_all)
    print_preview("ALL people (includes train people, so the median is not held out)",
                  preview_all, train_median)
    print(f"\nDeviation correlation (descriptive, all people, {correlation['n_pairs']} pairs): "
          f"{fmt(correlation['correlation'], 3)}")

    results = {
        "predictor": "AverageCyclePredictor (Python copy, app/src/main/java/com/example/cycled/domain/CyclePredictor.kt)",
        "error_definition": "predicted - target, in days",
        "test": score_test,
        "all_people": score_all,
        "placeholder_preview": {
            "train_median_cycle_length": train_median,
            "placeholder_length": PLACEHOLDER_LENGTH,
            "test": preview_test,
            "all_people": preview_all,
        },
        "deviation_correlation": correlation,
    }
    out_path = PROCESSED / "baseline_results.json"
    with open(out_path, "w") as f:
        json.dump(results, f, indent=2)
    print("\nsaved", out_path)


if __name__ == "__main__":
    main()

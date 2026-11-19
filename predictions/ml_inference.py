"""Inference for the app's ML models, using only the exported JSON (app/src/main/assets/cycled_model.json).

This is the reference that session 12b ports to Kotlin. It uses only the standard library.

Day convention (Marquette, and this file): cycle day 1 is the first day of the period.
  ovulation date = period start + (ovulation_day - 1)
  next start     = period start + cycle_days
  earliest       = period start + range_low_days, latest = period start + range_high_days

Input history: the person's earlier, completed cycles, oldest first, each as
(cycle_length, period_length) with period_length None when unknown.
"""

import json
import math


def round_half_up(x: float) -> int:
    """Kotlin's Double.roundToInt(): ties round up."""
    return math.floor(x + 0.5)


def clamp(x: int, low: int, high: int) -> int:
    return max(low, min(x, high))


def load_model(path) -> dict:
    with open(path) as f:
        return json.load(f)


def compute_features(history: list, model: dict) -> dict:
    """Features from the last `max_cycles_used` valid cycles, with population fallbacks.

    - n_cycles: valid earlier cycles used (0..max_cycles_used)
    - mean_cycle, last_cycle: of those cycles, else the fallback cycle length
    - sd_cycle: population SD (divide by n) when n >= 2, else the fallback SD
    - mean_period, last_period: of the known period lengths in those cycles, else the fallback
    """
    low, high = model["valid_cycle_days"]
    fb = model["fallbacks"]
    used = [(c, p) for c, p in history if low <= c <= high][-model["max_cycles_used"]:]
    cycles = [c for c, _ in used]
    periods = [p for _, p in used if p is not None]
    n = len(cycles)

    if n:
        mean_cycle = sum(cycles) / n
        last_cycle = float(cycles[-1])
    else:
        mean_cycle = last_cycle = fb["cycle_length"]
    if n >= 2:
        sd_cycle = math.sqrt(sum((c - mean_cycle) ** 2 for c in cycles) / n)
    else:
        sd_cycle = fb["sd_cycle"]
    if periods:
        mean_period = sum(periods) / len(periods)
        last_period = float(periods[-1])
    else:
        mean_period = last_period = fb["period_length"]

    return {
        "n_cycles": float(n),
        "mean_cycle": mean_cycle,
        "last_cycle": last_cycle,
        "sd_cycle": sd_cycle,
        "mean_period": mean_period,
        "last_period": last_period,
    }


def linear(model_part: dict, features: dict, order: list) -> float:
    """intercept + sum(weight * feature), in the exported feature order."""
    return model_part["intercept"] + sum(w * features[name] for w, name in zip(model_part["weights"], order))


def predict(features: dict, model: dict) -> dict:
    """Raw model outputs and the whole-day values the app uses."""
    order = model["features"]
    m = model["models"]
    clamps = model["clamps"]

    cycle_raw = linear(m["cycle_length"], features, order)
    cycle_days = clamp(round_half_up(cycle_raw), *clamps["cycle_days"])

    period_raw = linear(m["period_length"], features, order)
    period_days = clamp(round_half_up(period_raw), *clamps["period_days"])

    if model["ovulation_method"] == "luteal":
        luteal_raw = linear(m["luteal_length"], features, order)
        ovulation_raw = cycle_raw - luteal_raw
    else:
        luteal_raw = None
        ovulation_raw = linear(m["ovulation_day"], features, order)
    # Ovulation at least `min_days_before_next_start` before the next start (day cycle_days + 1).
    latest_ovulation = cycle_days + 1 - clamps["ovulation_min_days_before_next_start"]
    ovulation_day = clamp(round_half_up(ovulation_raw), 1, latest_ovulation)

    low_raw = linear(m["range_low"], features, order)
    high_raw = linear(m["range_high"], features, order)
    half = model["range_min_half_width_days"]
    range_low_days = min(round_half_up(low_raw), cycle_days - half)
    range_high_days = max(round_half_up(high_raw), cycle_days + half)

    return {
        "cycle_raw": cycle_raw,
        "period_raw": period_raw,
        "luteal_raw": luteal_raw,
        "ovulation_raw": ovulation_raw,
        "range_low_raw": low_raw,
        "range_high_raw": high_raw,
        "cycle_days": cycle_days,
        "period_days": period_days,
        "ovulation_day": ovulation_day,
        "range_low_days": range_low_days,
        "range_high_days": range_high_days,
    }

"""Generate cross-check cases for the app's AverageCyclePredictor with the session 12 defaults.

Uses the Python copy of the predictor in baseline.py, with the new CycleDefaults passed in as
parameters (baseline.py's own defaults still reproduce session 10). Prints each case and the
Kotlin list used by app/src/test/.../domain/AverageCyclePredictorCrossCheckTest.kt.
It writes no files.

Run from the repo root or from inside predictions/:
    python predictions/kotlin_cases.py
"""

from datetime import date, timedelta

from baseline import average_predict

# Session 12 CycleDefaults (app/src/main/java/com/example/cycled/domain/CycleDefaults.kt).
CYCLE_LENGTH_DAYS = 29
MIN_RANGE_HALF_WIDTH_DAYS = 3

LAST_START = date(2026, 11, 1)


def starts_from_lengths(*lengths: int) -> list:
    """Start dates ending on LAST_START, counting back through lengths (given oldest first)."""
    starts = [LAST_START]
    for length in reversed(lengths):
        starts.insert(0, starts[0] - timedelta(days=length))
    return starts


# (name, period start dates as the app would receive them)
CASES = [
    ("oneStart_placeholder", [LAST_START]),
    ("oneLength_plusMinusThree", starts_from_lengths(29)),
    ("identicalLengths_clampedToMinimum", starts_from_lengths(28, 28, 28)),
    ("varyingLengths_sdFour", starts_from_lengths(24, 32, 28, 36)),
    ("meanHalf_roundsUp", starts_from_lengths(28, 29)),
    ("sdHalf_roundsUp", starts_from_lengths(24, 31, 24, 31)),
    ("tooShortLength_dropped", starts_from_lengths(27, 10, 30, 29)),
    ("moreThanSix_lastSixUsed", starts_from_lengths(40, 22, 30, 31, 29, 28, 33, 27)),
    ("veryIrregular_clampedToSeven", starts_from_lengths(20, 45, 20, 45)),
    ("unsortedWithDuplicate", [date(2026, 11, 1), date(2026, 9, 6), date(2026, 10, 3), date(2026, 11, 1)]),
]


def app_predict(starts: list) -> tuple:
    """(nextStart, earliest, latest, basedOnCycles) the way the Kotlin builds them from start dates."""
    ordered = sorted(set(starts))
    lengths = [(b - a).days for a, b in zip(ordered, ordered[1:])]
    predicted, earliest, latest, based_on = average_predict(
        lengths, placeholder_length=CYCLE_LENGTH_DAYS, min_half_width=MIN_RANGE_HALF_WIDTH_DAYS,
    )
    next_start = ordered[-1] + timedelta(days=predicted)
    return (next_start, next_start - timedelta(days=earliest), next_start + timedelta(days=latest), based_on)


def kt_date(d: date) -> str:
    return f"LocalDate.of({d.year}, {d.month}, {d.day})"


def main() -> None:
    print(f"Defaults: CYCLE_LENGTH_DAYS={CYCLE_LENGTH_DAYS}, MIN_RANGE_HALF_WIDTH_DAYS={MIN_RANGE_HALF_WIDTH_DAYS}\n")
    kotlin = []
    for name, starts in CASES:
        next_start, earliest, latest, based_on = app_predict(starts)
        print(f"{name}: starts {[s.isoformat() for s in starts]}")
        print(f"    -> next {next_start}, earliest {earliest}, latest {latest}, basedOn {based_on}")
        kotlin.append(
            f'        Case(\n'
            f'            "{name}",\n'
            f'            listOf({", ".join(kt_date(s) for s in starts)}),\n'
            f'            Prediction({kt_date(next_start)}, {kt_date(earliest)}, {kt_date(latest)}, '
            f'basedOnCycles = {based_on}),\n'
            f'        ),'
        )
    print("\nKotlin cases:\n")
    print("\n".join(kotlin))


if __name__ == "__main__":
    main()

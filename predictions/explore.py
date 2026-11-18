"""Describe the cleaned cycle data and save two figures to predictions/figures/.

Needs resources/processed/cycles_clean.csv (run load_data.py first).
Run from the repo root or from inside predictions/:
    python predictions/explore.py
"""

from pathlib import Path

import matplotlib

matplotlib.use("Agg")  # no display needed; figures are only saved to files
import matplotlib.pyplot as plt
import pandas as pd

from load_data import PROCESSED, VALID_MAX, VALID_MIN

FIGURES = Path(__file__).resolve().parent / "figures"


def main() -> None:
    df = pd.read_csv(PROCESSED / "cycles_clean.csv")
    df = df[df["valid"].astype(str) == "True"].copy()

    # 1. People and cycles
    per_person = df.groupby("person_id").size()
    print("People:", df["person_id"].nunique())
    print("Valid cycles:", len(df))
    print("\nValid cycles per person:")
    print(per_person.describe())
    print("People with 7 or more valid cycles:", int((per_person >= 7).sum()))

    # 2. Cycle length
    print("\nCycle length:")
    print(df["cycle_length"].describe())

    # 3. Variation
    grouped = df.groupby("person_id")["cycle_length"]
    print("\nWithin-person variation (median of per-person SD):", grouped.std().median())
    print("Between-person variation (SD of per-person mean):", grouped.mean().std())

    # 4. Lag-1 correlation, consecutive cycle numbers only
    df = df.sort_values(["person_id", "cycle_number"])
    prev_length = df.groupby("person_id")["cycle_length"].shift(1)
    step = df.groupby("person_id")["cycle_number"].diff()
    pairs = pd.DataFrame({"current": df["cycle_length"], "previous": prev_length})[step == 1]
    print("\nConsecutive pairs:", len(pairs))
    print("Correlation with previous cycle length:", pairs["current"].corr(pairs["previous"]))

    # 5. Period and luteal lengths
    print("\nPeriod length:")
    print(df["period_length"].describe())
    print("\nLuteal length:")
    print(df["luteal_length"].describe())

    FIGURES.mkdir(parents=True, exist_ok=True)

    # Figure 1: cycle length distribution, 1-day bins
    fig, ax = plt.subplots()
    ax.hist(df["cycle_length"], bins=range(VALID_MIN, VALID_MAX + 2), edgecolor="black")
    ax.set_xlabel("Cycle length (days)")
    ax.set_ylabel("Number of cycles")
    ax.set_title("Cycle length distribution")
    fig.savefig(FIGURES / "cycle_length_hist.png", dpi=150, bbox_inches="tight")
    plt.close(fig)

    # Figure 2: valid cycles per person
    fig, ax = plt.subplots()
    ax.hist(per_person, bins=range(1, int(per_person.max()) + 2), edgecolor="black")
    ax.set_xlabel("Valid cycles per person")
    ax.set_ylabel("Number of people")
    ax.set_title("Cycles per person")
    fig.savefig(FIGURES / "cycles_per_person.png", dpi=150, bbox_inches="tight")
    plt.close(fig)

    print("\nFigures saved to:", FIGURES)


if __name__ == "__main__":
    main()

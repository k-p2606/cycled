"""Split people into train and test groups for the cycle predictor.

Needs resources/processed/cycles_clean.csv (run load_data.py first).
Run from the repo root or from inside predictions/:
    python predictions/examples.py
"""

import numpy as np
import pandas as pd

from load_data import PROCESSED

SEED = 42             # fixed, so the split is the same every time
TEST_FRACTION = 0.2   # about 32 of 159 people


def split_people(df: pd.DataFrame) -> pd.DataFrame:
    """Assign each person to 'train' or 'test'. A person's cycles are never split."""
    people = np.sort(df["person_id"].unique())   # sort first so file order can't change the result
    rng = np.random.default_rng(SEED)
    rng.shuffle(people)
    n_test = round(len(people) * TEST_FRACTION)
    test = set(people[:n_test])
    split = pd.DataFrame({
        "person_id": people,
        "split": ["test" if p in test else "train" for p in people],
    })
    return split.sort_values("person_id").reset_index(drop=True)


def summarize_split(df: pd.DataFrame, split: pd.DataFrame) -> None:
    """Check that train and test look alike and that nobody is in both."""
    merged = df[df["valid"]].merge(split, on="person_id")
    summary = merged.groupby("split").agg(
        people=("person_id", "nunique"),
        cycles=("cycle_length", "size"),
        mean_length=("cycle_length", "mean"),
        sd_length=("cycle_length", "std"),
    )
    print(summary.round(2))

    train_ids = set(split.loc[split["split"] == "train", "person_id"])
    test_ids = set(split.loc[split["split"] == "test", "person_id"])
    print("people in both groups:", len(train_ids & test_ids))   # must be 0


# Step 2: build prediction examples

# Matches the app's "last 6 cycles" (CycleDefaults.MAX_CYCLES_USED = 6).
MAX_HISTORY = 6

HISTORY_COLUMNS = [f"h{i}" for i in range(1, MAX_HISTORY + 1)]


def build_examples(df: pd.DataFrame, split: pd.DataFrame) -> pd.DataFrame:
    """One example per valid cycle: target = its length, history = the person's earlier valid lengths.

    h1 is the most recent previous cycle, h2 the one before, and so on; missing slots are NaN.
    n_history counts the earlier valid cycles, capped at MAX_HISTORY. Invalid cycles are neither
    examples nor history (the app drops them too), but they don't reset the history. A jump of
    more than 1 in cycle_number resets the history, since cycles are missing in between.
    """
    split_of = dict(zip(split["person_id"], split["split"]))
    rows = []
    ordered = df.sort_values(["person_id", "cycle_number"], kind="stable")
    for person_id, cycles in ordered.groupby("person_id", sort=True):
        history = []   # earlier valid cycle lengths, oldest first
        previous_number = None
        for cycle in cycles.itertuples(index=False):
            if previous_number is not None and cycle.cycle_number - previous_number > 1:
                history = []
            previous_number = cycle.cycle_number
            if not cycle.valid:
                continue

            recent = history[-MAX_HISTORY:][::-1]   # most recent first
            row = {
                "person_id": person_id,
                "split": split_of[person_id],
                "cycle_number": cycle.cycle_number,
                "target": cycle.cycle_length,
                "n_history": len(recent),
            }
            for i, column in enumerate(HISTORY_COLUMNS):
                row[column] = recent[i] if i < len(recent) else np.nan
            rows.append(row)
            history.append(cycle.cycle_length)

    columns = ["person_id", "split", "cycle_number", "target", "n_history"] + HISTORY_COLUMNS
    return pd.DataFrame(rows, columns=columns)


if __name__ == "__main__":
    df = pd.read_csv(PROCESSED / "cycles_clean.csv")
    split = split_people(df)
    summarize_split(df, split)
    split.to_csv(PROCESSED / "split.csv", index=False)
    print("saved", PROCESSED / "split.csv")

    examples = build_examples(df, split)
    examples.to_csv(PROCESSED / "examples.csv", index=False)
    print("\nExamples per split:")
    print(examples["split"].value_counts().sort_index().to_string())
    print("\nExamples by n_history:")
    print(pd.crosstab(examples["n_history"], examples["split"], margins=True, margins_name="total").to_string())
    print("saved", PROCESSED / "examples.csv")

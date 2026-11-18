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


if __name__ == "__main__":
    df = pd.read_csv(PROCESSED / "cycles_clean.csv")
    split = split_people(df)
    summarize_split(df, split)
    split.to_csv(PROCESSED / "split.csv", index=False)
    print("saved", PROCESSED / "split.csv")


# Step 2: build prediction examples (next)

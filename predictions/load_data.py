"""Load, clean and check the raw Marquette (FedCycleData) cycle dataset.

Run from the repo root or from inside predictions/:
    python predictions/load_data.py
"""

from pathlib import Path

import pandas as pd

ROOT = Path(__file__).resolve().parent.parent
RAW_CSV = ROOT / "resources" / "FedCycleData071012.csv"
PROCESSED = ROOT / "resources" / "processed"

# Raw column name -> new name
COLUMNS = {
    "ClientID": "person_id",
    "CycleNumber": "cycle_number",
    "LengthofCycle": "cycle_length",
    "LengthofMenses": "period_length",
    "LengthofLutealPhase": "luteal_length",
    "EstimatedDayofOvulation": "ovulation_day",
}

# Matches the app's AverageCyclePredictor (CycleDefaults.MIN/MAX_VALID_CYCLE_DAYS).
VALID_MIN = 15
VALID_MAX = 60

NUMERIC_COLUMNS = ["cycle_number", "cycle_length", "period_length", "luteal_length", "ovulation_day"]


def load_raw() -> pd.DataFrame:
    """Read the raw Marquette CSV."""
    return pd.read_csv(RAW_CSV)


def inspect(raw: pd.DataFrame) -> None:
    """Print shape, column list and first 10 rows, and flag expected columns that are missing."""
    print("Shape:", raw.shape)
    print("Columns:", raw.columns.tolist())
    print(raw.head(10).to_string())
    missing = [name for name in COLUMNS if name not in raw.columns]
    if missing:
        print("MISSING expected columns:", missing)
    else:
        print("All expected columns found.")


def clean(raw: pd.DataFrame) -> pd.DataFrame:
    """Keep and rename the needed columns, coerce numbers, drop duplicates, flag valid cycles."""
    df = raw[list(COLUMNS)].rename(columns=COLUMNS).copy()

    df["person_id"] = df["person_id"].astype(str).str.strip()
    df = df[(df["person_id"] != "") & (df["person_id"].str.lower() != "nan")]

    # Blanks are stored as spaces, so numeric columns load as text.
    for col in NUMERIC_COLUMNS:
        df[col] = pd.to_numeric(df[col], errors="coerce")

    df = df.dropna(subset=["cycle_number", "cycle_length"])
    df = df.astype({"cycle_number": int, "cycle_length": int})

    df = df.sort_values(["person_id", "cycle_number"], kind="stable")

    before = len(df)
    df = df.drop_duplicates(["person_id", "cycle_number"], keep="first")
    print(f"removed {before - len(df)} duplicate cycle rows")

    df = df.reset_index(drop=True)
    df["valid"] = df["cycle_length"].between(VALID_MIN, VALID_MAX)
    return df


def check(df: pd.DataFrame) -> None:
    """Print row/person counts, duplicates, numbering gaps and invalid cycle count."""
    print("Rows:", len(df))
    print("People:", df["person_id"].nunique())

    duplicates = df.duplicated(subset=["person_id", "cycle_number"]).sum()
    print("Duplicate (person_id, cycle_number) pairs:", duplicates)

    steps = df.groupby("person_id")["cycle_number"].diff()
    print("Gaps in cycle numbering (step > 1):", int((steps > 1).sum()))

    print(f"Invalid cycles (outside {VALID_MIN}-{VALID_MAX} days):", int((~df["valid"]).sum()))


def build_sequences(df: pd.DataFrame) -> pd.Series:
    """Return a Series of person_id -> ordered list of valid cycle lengths."""
    valid = df[df["valid"]].sort_values(["person_id", "cycle_number"])
    return valid.groupby("person_id")["cycle_length"].apply(list)


if __name__ == "__main__":
    raw = load_raw()
    inspect(raw)
    df = clean(raw)
    check(df)

    PROCESSED.mkdir(parents=True, exist_ok=True)
    df.to_csv(PROCESSED / "cycles_clean.csv", index=False)
    print("saved", PROCESSED / "cycles_clean.csv")

    print("\nFirst 5 sequences:")
    print(build_sequences(df).head(5))

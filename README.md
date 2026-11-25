# cycled 🌊

an offline Android period tracker that predicts your cycle with ML models trained on real data and draws it as a tide

---

## How it works

```
Cycled
│
├── ML pipeline (Python, offline)
│   ├── Marquette Menstrual Cycle Data (1,649 cycles, 159 people)
│   ├── Clean + build per-person cycle sequences
│   ├── Train 4 linear models (scikit-learn)
│   │   ├── Cycle length         → ridge regression
│   │   ├── Period length        → ridge regression
│   │   ├── Ovulation day        → ridge regression
│   │   └── Prediction range     → quantile regression (15th / 85th)
│   ├── Evaluate on held-out people vs rule-based baseline
│   └── Export → cycled_model.json + test cases
│
└── Android app (Kotlin)
    │
    ├── Data layer
    │   ├── Room (SQLite)          → period days, symptoms, mood
    │   ├── DataStore              → settings
    │   └── CycleRepository        → single source of data
    │
    ├── Domain layer
    │   ├── groupIntoPeriods()     → logged days → periods
    │   ├── MlCyclePredictor       → runs the exported models on your history
    │   │   └── AverageCyclePredictor (fallback)
    │   ├── cycleForecast()        → phases, fertile window, next period
    │   ├── TideModel              → moon + tide drawing maths
    │   ├── CycleStats             → averages and variation
    │   └── Recommendations        → tips by phase + logged symptoms
    │
    └── UI layer (Jetpack Compose)
        ├── CycleViewModel (StateFlow)
        ├── Home      → cycle day, moon, tide, coming-up dates, tips
        ├── Calendar  → month grid + daily logging
        ├── Logs      → cycle history + stats
        └── Profile   → settings, delete data
```

---

## Tech stack

| Area | Tools |
|---|---|
| App | Kotlin, Jetpack Compose, Navigation Compose |
| Data | Room (SQLite), Preferences DataStore |
| Architecture | MVVM, Repository pattern, StateFlow |
| ML | Python, pandas, NumPy, scikit-learn |
| Testing | JUnit (240+ unit tests), instrumented tests, Kotlin ↔ Python cross-checks |
| Build | Gradle, KSP, Android Studio |

---

## Learning outcomes

- Train in Python, run in Kotlin. Exported linear models as JSON and re-implemented inference on-device, with tests proving both sides give the same answers.
- Evaluate honestly. Split train/test by person, not by row, and compared every model against a simple baseline.
- Privacy by design. No server and no accounts which means health data never leaves the phone.
- Clean architecture pays off. Separating domain logic from UI let the whole app be redesigned without touching prediction code or its tests.
- Safe database changes. Schema updates go through Room migrations.
- Responsible health UX. Predictions are shown as date ranges, labelled as estimates, and never presented as contraception or medical advice.

---

*Data: [Marquette University Menstrual Cycle Data](https://epublications.marquette.edu/data_nfp/7/). Not medical advice.*

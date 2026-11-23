
## UI
UI work follows docs/design/handoff.md (screenshot: docs/design/screens.png).
Read the "Build decisions" section first. Where the design conflicts with
domain logic, the domain logic wins: dates, phases and predictions always
come from cycleForecast() / CycleViewModel. Never change prediction logic,
cycled_model.json, ml_cases.json or existing test expectations for UI work.
Every screen keeps the Route + stateless Screen + previews pattern.
Kotlin tests: ./gradlew testDebugUnitTest. Build: ./gradlew assembleDebug.

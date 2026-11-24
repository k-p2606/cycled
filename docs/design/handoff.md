# Cycled — Design Handoff

Nov 22, 2026 · @jiggle · Markdown copy for the repo (`docs/design/handoff.md`), with build decisions added.

The screenshot of all four screens is `docs/design/screens.png`. Where the screenshot and this doc disagree, this doc wins.

---

## Build decisions (read first)

The design was drawn with a fixed 28-day cycle and sample data. The app already has a working data and prediction layer (Room, `CycleRepository`, `cycleForecast()`, `MlCyclePredictor`, 111 unit tests). These rules decide every conflict between the two.

### D1. The domain layer wins over the design's cycle model

- Every date, phase, prediction and cycle day on screen comes from `cycleForecast()` / `CycleViewModel`, never from the "Cycle model" section's fixed 28/5 numbers.
- Do not change `domain/` prediction logic, `cycled_model.json`, `ml_cases.json` or existing test expectations to match the design. New code in `domain/` (pure helpers for the tide, moon, cycles list) is fine and needs unit tests.
- Screens keep the existing pattern: a `...Route` that collects from the ViewModel and a stateless `...Screen` with previews. Today comes from `CycleViewModel.today`, never `LocalDate.now()`.

### D2. Phase → tide name and colours

| App phase (`Phase`) | Tide name | Calendar style |
|---|---|---|
| Menstrual | Low water | Coral fill if logged; dashed coral ring if predicted |
| Follicular | Flood tide | Teal fill, white number |
| Ovulatory | High water | Cream fill, sky number |
| Luteal | Ebb tide | 1.5 pt lavender ring |
| No phase (on/after predicted start, or late) | Slack water | No phase style |

- Home subline under the tide name is the phase name ("Follicular phase"). When late, the subline is "Your period may be late" (from `CycleMessages`).
- Name the ovulation day **"High tide"** everywhere (Home row, Calendar legend). Replace "Ovulation" in the legend.

### D3. The tide and moon stretch to her predicted cycle

Let `L` = `cycleLengthDays` from the forecast and `O` = cycle day of predicted ovulation (`ovulation − cycle start + 1`).

- Tide x-axis spans days 1..L: `x(d) = 20 + (d − 1) / (L − 1) * 350`.
- Moon/tide phase:
  - `p(d) = 0.5 * (d − 1) / (O − 1)` for `d ≤ O`
  - `p(d) = 0.5 + 0.5 * (d − O) / (L + 1 − O)` for `d > O`
  - With L = 28 and O = 14 this matches the design's formula closely.
- Hormone curves keep their shapes but are placed relative to O: estrogen peaks at `O − 1.5` and `O + 7`, progesterone at `O + 7`, LH at `O − 0.4`. Widths unchanged.
- Phase labels sit at the middle of each phase range from the forecast, not at fixed days. Skip a label whose phase has fewer than 2 days.
- If cycle day > L (late), clamp the today marker at day L, show the moon as new (p = 0), and use "Slack water".
- Put this maths in a pure function (e.g. `domain/TideModel.kt`) with unit tests: p(1) = 0, p(O) = 0.5, p rises then keeps rising to < 1 at day L, x(1) = 20, x(L) = 370.

### D4. Fertile window and Home rows come from the forecast

- Fertile window = the forecast's window (ovulation − 5 to + 1), not days 10–15.
- "Next period" row: date = predicted start. Under the three rows add one 13 pt text-muted line: "Likely 26 Nov – 2 Dec. Estimates from your logged dates, not contraception or medical advice." (using the prediction's earliest–latest). This keeps the ±3-day range and the disclaimer on Home.
- "High tide" row: date = predicted ovulation.
- With no data (`HomeUiState.NoData`): show the date line, "Cycle day –", new moon, title "Log your first period", the tide drawing without the today marker, no coming-up rows, and the "Log today" button.

### D5. Calendar dot = logged, not fertile

Follow the design: the small dot under a day means symptoms or mood were logged. The fertile window is no longer dotted on the calendar; it stays in each day's screen-reader text ("estimated fertile window") and on Home. Only the current cycle and the next predicted period get phase styles, as now.

### D6. Logging moves into Calendar; the Log screen goes

- The Calendar's log panel replaces `ui/log/LogScreen.kt` and the "Recently logged" list. Remove the `log` route once Calendar logging works.
- Home's "Log today" opens Calendar with today selected.
- Future days can be selected and viewed but not logged (chips and period button disabled), matching the old Log screen's rule.

### D7. Flow "None" needs a database change

- Flow chips: None, Light, Medium, Heavy.
- "None" means "checked, not bleeding". Store it on `DailyLog` as a new `noFlow: Boolean = false` column, database version 2 → 3 with a Room `AutoMigration`, plus a migration test like `migrate1To2`. Never `fallbackToDestructiveMigration()`.
- Picking Light/Medium/Heavy upserts a `PeriodDay` and clears `noFlow`. Picking None deletes that day's `PeriodDay` and sets `noFlow`. Tapping the selected chip again clears both.
- Existing `Spotting` rows stay valid: they count as logged flow (coral) and show as "Spotting" in Logs; no chip is selected for them; picking a chip replaces it. Do not remove `Spotting` from `FlowLevel`.
- Symptoms and moods: check the existing `Symptom` and `Mood` enums against the design's lists. Add missing values; never rename or remove existing ones (they are stored by name). Only the design's values get chips.

### D8. Hormone lines stay, labelled

The "Typical pattern" label and the hormone key must always be visible next to the curves. They are textbook shapes, not her levels.

### D9. Profile

- Settings live in Preferences DataStore (`data/SettingsRepository.kt`): `name` (default empty → header shows "You" and initial "Y"), `avgCycle` 28 (20–45), `avgPeriod` 5 (2–10), `remindPeriod` true, `remindDaily` false.
- `avgCycle` / `avgPeriod` are saved only for now. Wiring them in as the models' 0-cycle input is a later item; do not change the predictor in this work.
- Reminder switches save the setting only; no notifications yet (reminders are a stretch item).
- Under the name: "N cycles logged" = completed cycles from `groupIntoPeriods()`.
- "Export my data": row shown disabled with "Coming soon".
- "Delete all my data": confirm dialog ("Delete all your logged days and settings? This can't be undone.") then clears both tables and DataStore.

### D10. Logs

- Cycles are derived from period starts (`groupIntoPeriods()`); a cycle runs from one start to the day before the next. The newest is "Current".
- Days that have flow, `noFlow`, symptoms or mood are listed. "None" flow days show "No flow".
- Tapping a day row opens Calendar on that month with that day selected.
- With no periods logged: tabs hidden, show the empty state.

### D11. Theme and fonts

- Dark only. `dynamicColor = false`. Map the tokens into a `darkColorScheme` and expose the full token set as `CycledColors` via a `CompositionLocal` (like `MaterialTheme.phaseColors`).
- Replace `PhaseColors.kt` values with the design's colours. Update `PhaseColorsTest` so every number colour passes 4.5:1 against its fill, and coral, lavender and the dashed ring pass 3:1 against sky. If a design colour fails, report it rather than silently changing it.
- Fonts bundled in `res/font/` (Bricolage Grotesque 500/600, Figtree 400/500/600, from the official google/fonts repo, OFL). No downloadable-fonts dependency. Numbers use tabular figures (`fontFeatureSettings = "tnum"`).
- Phone frame 390 × 844 is the design size; layouts must also work on narrower phones (360 wide) and with larger font scale. Use `dp`/`sp`; pt in this doc = dp/sp.

### D12. Sample data

The sample data below is for previews and a debug-only "Load sample data" action (debug builds only). It must never ship as real data.

---

## Overview

Cycled is a period and health tracker with four tabs (Home, Calendar, Logs, Profile) that draws the user's cycle as a tide rising and falling. The period is low water, ovulation is high water, and the moon on Home shows where the user is in *her* cycle, not the real sky.

- **Phone frame:** 390 × 844 pt, dark theme only, no drawn status bar (the device draws its own).
- **Navigation:** a fixed bottom tab bar on every screen. Home's "Log today" button also opens Calendar.
- **Sample data:** the user "Maya", today = Sun 8 Nov 2026, current cycle started 31 Oct 2026 (cycle day 9). All of it is placeholder.

## Design tokens

The palette is a deep-night sky above lighter water, with one cream highlight and four cycle colours.

| Token | Hex | Used for |
| --- | --- | --- |
| sky | #0A1C29 | Screen background, tab bar, dark text on light fills |
| water | #134257 | Lower panels, front wave layer |
| water-back | #0F3347 | Back wave layer behind the front one |
| moon-dark | #17303E | Unlit side of the moon (93% opacity) |
| cream | #F4EAD5 | Big numbers, primary button, active tab, ovulation days, selected chips |
| text | #EEF3F2 | Body text |
| text-muted | #C4D5DB | Secondary text on water |
| text-dim | #A9BFC7 | Secondary text on sky, inactive tabs |
| divider-sky | #24414F | Row lines on sky |
| divider-water | #2B5B70 | Row lines on water |
| outline | #6493A7 | Unselected chip borders |
| selected-cell | #1C4E64 | Selected calendar day background |
| period | #FF8A75 | Period days, Log period button, period dot |
| period-light | #FFB3A5 | Predicted-period number, "Period logged" text |
| follicular | #25767D | Follicular day fill (white text) |
| luteal | #B9A6E8 | Luteal day ring |
| fertile | #8BE0D6 | Fertile window dot, logged-day dot |
| estrogen | #FFC46B | Estrogen line |
| progesterone | #C9B8F5 | Progesterone line |

**Fonts:** Bricolage Grotesque for display and headings, weights 500–600; Figtree for everything else, weights 400/500/600. Numbers use tabular figures.

| Style | Font | Size / weight |
| --- | --- | --- |
| Cycle day number | Bricolage | 112 / 500, line height 0.95 |
| Screen title | Bricolage | 34 / 600 (Calendar month: 30 / 600) |
| Section title | Bricolage | 21–22 / 600 |
| Body, row labels | Figtree | 15–16 / 400–500 |
| Small labels, legends | Figtree | 13 / 400–500 |
| Tab labels | Figtree | 12 / 400 (active 600) |

**Layout:** screen side padding 24 (Calendar 16); top padding 52–60 below the device's status area; tab bar 76 tall. Every tappable thing is at least 44 × 44. Pills and buttons are fully rounded (radius = half the height); calendar day cells have radius 12; cycle tabs on Logs have radius 16.

## Cycle model (design reference only — see D1–D4)

The design assumes a fixed 28-day cycle with a 5-day period. **The app does not use this table for dates;** it is here so the drawing intent is clear.

| Cycle days | Phase | Tide name | Calendar style |
| --- | --- | --- | --- |
| 1–5 | Period | Low water | Coral fill if logged; dashed coral ring if predicted |
| 6–12 | Follicular | Flood tide | Teal #25767D fill, white number |
| 13–15 | Ovulation | High water | Cream fill, dark number |
| 16–28 | Luteal | Ebb tide | 1.5 pt lavender #B9A6E8 ring |

- **Predicted period** = any future day in the predicted period that has no flow logged. A day logged with flow "None" is not predicted.
- **Personal moon:** new moon on day 1, full moon at ovulation, back toward new by the end of the cycle. Design formula for 28 days: `p(d) = (d−1)/26` for d ≤ 14, `0.5 + (d−14)/30` for d > 14. The app uses the stretched version in D3. The same p(d) shapes the tide height, so the moon and the water always agree.

## Shared components

Five pieces repeat across screens; build them once (e.g. `ui/components/`).

- **Tab bar:** 76 tall, sky background, four equal columns: Home, Calendar, Logs, Profile. Each tab is a 24 pt line icon (1.8 stroke, round caps) over a 12 pt label, 3 pt apart, 8 pt top padding. Active tab: cream, label weight 600. Inactive: text-dim. Icons: Home = two stacked wavy lines; Calendar = rounded rectangle with two tabs on top; Logs = three lines with a dot before each; Profile = head and shoulders. Profile also has a 1 pt divider-sky line above the bar. Draw the icons as vector drawables or `ImageVector`s of your own.
- **Wave divider:** a 390 × 16 water-coloured shape that turns the sky into the lower water panel on Calendar and Logs. SVG path: `M0 16V9C33 1 65 1 98 8S163 15 195 8 260 1 293 8 358 15 390 8V16Z` (scale horizontally to the screen width).
- **Choice chip:** 44 tall, fully rounded, Figtree 15/500. Off: transparent with a 1 pt outline #6493A7 and text colour. On: cream fill and border, sky text. Single-choice groups (Flow, Mood) sit in a 4-column grid with 6 pt gaps; multi-choice groups (Symptoms) wrap with 14 pt side padding per chip. Screen readers hear selected state.
- **List row:** a 1 pt divider on top, 50–52 tall, label on the left, value or control on the right.
- **Primary button:** 52 tall, full width, fully rounded, Figtree 17/600. Home's "Log today" is cream with sky text; Calendar's period button is described in the Calendar section.

## Home

Home answers "where am I in my cycle?" at a glance: today's cycle day, the moon and the tide name, the whole cycle drawn as water, and three dates coming up.

Top to bottom:

- **Date line:** "Sunday, 8 Nov", Figtree 15, text-dim. Top padding 60, sides 24.
- **Hero row:** "Cycle day" (15, text-dim) over the day number (Bricolage 112/500, cream) on the left; the moon (164 × 164) on the right.
- **Tide name:** Bricolage 34/600, e.g. "Flood tide". Under it the phase alone, Figtree 16 text-muted, e.g. "Follicular phase". No moon-phase names anywhere.
- **Tide drawing:** 390 × 172, full width (details below).
- **Water panel:** fills the rest above the tab bar, water background, sides 24, bottom 18, items spaced evenly: the hormone key, three "coming up" rows, the range/disclaimer line (D4), then the cream "Log today" button linking to Calendar. Home scrolls if the screen is too short.

**Moon (164 × 164, centre 82,82):** a thin 1 pt divider-sky ring at r 79; a soft glow ring at r 72 (cream at 7% opacity, 6 pt stroke); the full disc in cream at r 68. On the disc: three slightly darker patches (#E7DBC2 ellipses at 70,70 19×14; 104,94 15×10; #EADFC8 at 84,122 13×7) and about ten craters (#DCCBAA circles, r 3–10; the three largest have a lighter #E6D8BC inner circle offset by +1.5,+1.5). Then the shadow: the moon shape for phase p + 0.5, filled moon-dark at 93% opacity, so craters faintly show through. The moon is decorative for screen readers; the hero row reads "Cycle day 9, Flood tide, follicular phase".

Moon shape for phase p (0–1) at centre (cx, cy), radius r:

```
rx = r * |cos(2πp)|
waxing = p < 0.5
limb = waxing ? 1 : 0
term = waxing ? (p < 0.25 ? 0 : 1) : (p < 0.75 ? 0 : 1)
path = M cx (cy-r)  A r r 0 0 limb cx (cy+r)  A rx r 0 0 term cx (cy-r) Z
```

(Compose `Path` has no SVG arc command; build the two half-ellipses with `arcTo` on bounding rects, or use `PathParser` with the SVG string.)

**Tide drawing (390 × 172):**

- x for cycle day d: see D3 (design: `x(d) = 20 + (d − 1) / 27 * 350`).
- Tide height at x: `tide(x) = 72 + 40 * cos(2π * p(day at x))` — low at day 1, highest at ovulation.
- The water edge rides small sharp-crested ripples (a trochoid): for t from −7 in steps of 0.15, `X = 4.4t − 3.4 sin t`, `Y = tide(X) − 4 cos t`, until X passes the right edge. Fill from that edge down to y 172.
- Two layers: back layer #0F3347 shifted +11 in x and 9 up; front layer #134257 on top.
- **Hormone lines** (typical textbook shapes, not user data), drawn over the water with `y = 136 − 104 · v(d)`, where `g(d, m, s) = exp(−(d−m)² / 2s²)`. Design values for O = 14 (shift the means with O, see D3):
  - Estrogen, #FFC46B 2 pt: `0.12 + 0.78 g(d,12.5,2.3) + 0.32 g(d,21,3.5)`
  - Progesterone, #C9B8F5 2 pt: `0.05 + 0.8 g(d,21,3.3)`
  - LH, #EEF3F2 1.75 pt, dashed 4 4: `0.06 + 0.88 g(d,13.6,0.75)`
- **Today marker** at the front-layer point nearest x(today): a cream 17 pt halo at 18% opacity; a 12 pt sky circle with a 2 pt cream border; a 7 pt cream dot; "Today" (12/600 cream) 24 pt above; a dotted cream line (60%, dash 2 4) from the marker down to y 146.
- **Phase labels** inside the water at y 164, Figtree 12 text-muted, centred at the middle of each phase (D3).
- No red, teal or white foam details on the wave.

**Hormone key:** a wrapping row, 13 pt text-muted: a 16 × 2 swatch + "Estrogen", "Progesterone", a dashed swatch + "LH", and "Typical pattern" pushed to the right.

**Coming-up rows** (50 tall, divider-water on top): a 9 pt dot, label, date, and a right-aligned "until" in cream 600.

| Row | Dot | Date shown | Until |
| --- | --- | --- | --- |
| Next period | period | predicted start, e.g. Sat 28 Nov | in 20 days |
| Fertile window | fertile | range, e.g. 9–14 Nov | now / tomorrow / in N days |
| High tide | cream | weekday + date of predicted ovulation | in N days |

Until text: 0 = "today", 1 = "tomorrow", otherwise "in N days". Fertile window shows "now" while today is inside it. If a date has passed in this cycle (e.g. high tide was last week), show the row with "passed" in text-muted.

## Calendar

Calendar is also the logging screen: the month grid sits on the sky, the selected day's log sits in the water below, and a period button is pinned above the tab bar.

- **Header** (top padding 52, sides 16): month and year (Bricolage 30/600) on the left; previous and next month as 44 × 44 chevron buttons on the right ("Previous month" / "Next month" for screen readers).
- **Weekday row:** 22 tall, 13 pt text-dim, centred. Week start follows the phone's locale as now (`monthGrid()`).
- **Month grid:** 7 equal columns, each row 48 tall. Blank cells before day 1. Each day is a button (radius 12) holding a 32 × 32 circle with the number (15 pt, 500; today 700) and below it a 5 pt slot for a 4 pt fertile-colour dot when symptoms or mood were logged (D5). No moon icons.
- **Legend** (wraps, 13 pt text-muted, 11 pt swatches): Period (coral fill), Predicted (dashed coral ring), Follicular (teal fill), High tide (cream fill), Luteal (lavender ring), Logged (small dot). Late message under it when late.
- **Wave divider**, then the **log panel** (water, scrolls, sides 16, 10 pt gaps).
- **Period button bar** (water, padding 10/16/12, 1 pt divider-water on top), then the tab bar.

**Day styling, first match wins:**

- Flow logged (anything but None, including Spotting) → coral fill, sky number.
- Predicted period → 1.5 pt dashed coral ring, #FFB3A5 number.
- Ovulatory phase → cream fill, sky number.
- Follicular phase → teal fill, white number.
- Luteal phase → 1.5 pt lavender ring.

Then: today gets a 2 pt cream ring (replacing any ring above); the selected day gets a #1C4E64 cell background. Screen readers hear e.g. "8 November, today, follicular phase, estimated fertile window, symptoms logged".

**Log panel** for the selected day (defaults to today):

- Title row: "Sun 8 Nov" (Bricolage 21/600) and, right-aligned, "Day 9 · Flood tide" (14 text-muted). Omit the right side for days outside the current cycle.
- **Flow** (pick one, tap again to clear): None, Light, Medium, Heavy.
- **Symptoms** (pick any): Cramps, Headache, Bloating, Tired, Tender, Acne.
- **Mood** (pick one, tap again to clear): Calm, Low, Irritable, Bright.
- Each group has a 13 pt text-muted label 4 pt above it. Changes save instantly and update the grid.

**Period button** (full width, 52 tall, fully rounded, 1.5 pt coral border):

| State | Fill | Text | Label |
| --- | --- | --- | --- |
| No flow on selected day | coral | sky | Log period today / Log period on 5 Nov |
| Flow logged | transparent | #FFB3A5 | ✓ Period logged today (check icon, not a character) |

Tapping when off sets flow to Medium; tapping when on clears the flow. The label says "today" when the selected day is today, otherwise "on <day> <month>". Disabled for future days.

## Logs

Logs lists every logged day, one cycle at a time, newest first. View-only, except that tapping a row opens it on Calendar (D10).

- **Title:** "Logs", Bricolage 34/600, top padding 60, sides 24.
- **Cycle tabs:** a sideways-scrolling row 14 pt below the title, 8 pt gaps, bleeding to the screen edges. Each tab is 84 × 56, radius 16, with a 15/600 label over a 12 pt subline at 80% opacity. The first tab is "Current" / "Day 9"; older tabs show the start date and length, e.g. "3 Oct" / "28 days". Selected tab is cream with sky text; others are outlined #6493A7.
- **Wave divider** 16 pt below the tabs, then the water panel (scrolls, sides 24).
- **Cycle heading:** the date range in Bricolage 22/600 ("31 Oct – today", "3 Oct – 30 Oct") and under it, 14 text-muted: "Day 9 so far · period 5 days · 5 days logged" or "28-day cycle · …".
- **Day rows**, newest first, only days with something logged. Each row: divider-water on top, at least 64 tall, three columns:
  - 44 wide: date number (Bricolage 22/600) over the weekday (12, text-muted).
  - Middle: a coral dot + "Heavy flow" (15/500) if flow was logged ("No flow" without the dot for None); under it symptoms and mood joined by " · ", e.g. "Cramps · Tired · Mood: low" (14 text-muted).
  - Right: "Day 2" (13 text-muted).
- **Empty state:** "Nothing logged in this cycle yet." in 15 text-muted.

Period days in the heading count days with flow other than None.

## Profile

Profile is a plain settings page on the sky background, with no cycle summary or wave drawing.

- **Header** (top padding 60, sides 24, 14 pt gap): a 60 pt cream circle with the initial (Bricolage 28/600, sky), then the name (Bricolage 28/600) over "N cycles logged" (15 text-muted). See D9.
- **Sections** (sides 24), each a 13 pt text-muted heading over rows 52 tall with a divider-sky line on top:

| Section | Row | Control | Default | Range |
| --- | --- | --- | --- | --- |
| My cycle | Average cycle | − / value / + stepper | 28 days | 20–45 |
| My cycle | Average period | − / value / + stepper | 5 days | 2–10 |
| Reminders | Period coming up | Switch | On | — |
| Reminders | Daily log nudge | Switch | Off | — |
| Your data | Export my data | Text button, disabled, "Coming soon" | — | — |
| Your data | Delete all my data | Text button in coral, 48 tall, confirm dialog | — | — |

- **Stepper:** 44 × 44 "−" and "+" buttons in cream (24/500) around a 64-wide value, "28 days" (15/600). Buttons disable at the range ends.
- **Switch:** 52 × 30 track inside a 56 × 44 tap area, 24 pt knob, 3 pt inset. On: cream track, sky knob on the right. Off: divider-sky track, text-muted knob on the left.
- The page scrolls if needed; the tab bar stays fixed.

## Data model (design) vs the app

Design:

```
// DayLog, keyed by "YYYY-MM-DD"
{
  flow: null | "None" | "Light" | "Medium" | "Heavy",
  sym:  ["Cramps" | "Headache" | "Bloating" | "Tired" | "Tender" | "Acne"],
  mood: null | "Calm" | "Low" | "Irritable" | "Bright"
}
// Settings
{ name: "Maya", avgCycle: 28, avgPeriod: 5, remindPeriod: true, remindDaily: false }
```

In the app: flow Light/Medium/Heavy (and old Spotting) → `period_days`; "None" → `daily_logs.noFlow` (D7); symptoms and mood → `daily_logs`; settings → DataStore (D9). Cycles are derived from period starts with `groupIntoPeriods()`.

- A **period day** is any day with flow other than None.
- **Logged** (the small dot on Calendar) means the day has a symptom or a mood.

## Sample data (previews and debug "Load sample data" only)

Today = 8 Nov 2026. Cycles: 31 Oct (current, day 9); 3 Oct – 30 Oct (28 days); 7 Sep – 2 Oct (26 days); 9 Aug – 6 Sep (29 days).

```
date,flow,symptoms,mood
2026-11-04,Light,,Calm
2026-11-03,Medium,,
2026-11-02,Heavy,,
2026-11-01,Heavy,Cramps;Tired,
2026-10-31,Medium,Cramps,Low
2026-10-27,,Tender,Low
2026-10-22,,Bloating,Irritable
2026-10-15,,Headache,
2026-10-07,Light,,
2026-10-06,Light,,
2026-10-05,Medium,,
2026-10-04,Heavy,Cramps;Tired,Low
2026-10-03,Medium,Cramps,
2026-09-28,,Bloating;Acne,
2026-09-20,,,Bright
2026-09-11,Light,,
2026-09-10,Light,,
2026-09-09,Medium,Tired,
2026-09-08,Heavy,,
2026-09-07,Heavy,Cramps,
2026-08-30,,Headache,Irritable
2026-08-13,Light,,
2026-08-12,Light,,
2026-08-11,Medium,,
2026-08-10,Heavy,Cramps,
2026-08-09,Medium,,
```

With the ML predictor the app's dates for this history will differ slightly from the screenshot (e.g. next period, high tide). That is expected (D1).

## Still open (not needed for this build)

- Profile averages as the models' 0-cycle input (stretch).
- App name, icon, onboarding, and where the name and first period date are entered.
- What "Export my data" produces.
- Reminders (stretch).

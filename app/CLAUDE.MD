# Personal + Business Finance Tracker — App Scope

**Platform:** Android, native, fully offline
**Build tool:** Claude Code

---

## 1. Recommended Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose (modern, less boilerplate than XML views)
- **Local database:** Room (SQLite wrapper) — fully offline, no server needed
- **Architecture:** MVVM (ViewModel + Repository pattern) — keeps UI and data logic separate, easier for Claude Code to generate/edit incrementally
- **Charts:** Vico or MPAndroidChart (both work well with Compose)
- **Min SDK:** API 26 (Android 8.0) is a safe floor unless you need an older device supported

No backend, no auth, no internet permission needed — everything lives in a local SQLite DB via Room.

---

## 2. Data Model

**Transaction**
- `id` (auto)
- `amount` (Double)
- `type` (enum: PERSONAL / BUSINESS)
- `category` (FK → Category)
- `date` (LocalDate)
- `note` (String, optional)
- `isIncome` (Boolean)
- `recurringId` (FK → RecurringRule, nullable — set if generated from a recurring rule)

**Category**
- `id`
- `name`
- `type` (PERSONAL / BUSINESS / BOTH)
- `budgetLimit` (Double, nullable — monthly limit for this category)
- `icon`/`color` (optional, for UI)

**RecurringRule**
- `id`
- `label`
- `amount`
- `category`
- `type` (PERSONAL / BUSINESS)
- `isIncome`
- `frequency` (weekly / monthly / custom interval)
- `nextDueDate`
- `active` (Boolean)

This single-table-with-a-type-tag design is what lets you view personal-only, business-only, or combined — no data duplication.

---

## 3. Core Screens (v1)

1. **Home / Dashboard**
    - Toggle: Personal | Business | Combined
    - This month's total income vs expenses
    - Quick "+ Add transaction" button
    - Recent transactions list

2. **Add/Edit Transaction**
    - Amount, category picker, date, note, income/expense toggle, personal/business toggle

3. **Transactions List**
    - Filterable by type, category, date range
    - Search by note

4. **Budgets**
    - List of categories with budget limits
    - Progress bar per category (spent vs limit), red state if over

5. **Reports**
    - Pie chart: spending by category (filtered by personal/business/combined)
    - Bar chart: monthly totals over time
    - Personal vs business comparison view

6. **Recurring Rules**
    - List of active recurring income/expenses
    - Add/edit/pause a rule
    - On app open, check for due recurring rules and auto-generate transactions

7. **Categories (settings)**
    - Add/edit/delete custom categories, set budget limits, assign type

---

## 4. Feature Priority

**Must-have (v1):**
- Add/edit/delete transactions + categories
- Personal/business/combined filtering
- Budget limits with visual warnings
- Recurring transactions with auto-generation
- Basic reports (category breakdown, monthly totals)

**Nice-to-have (v2+):**
- Export to CSV
- Multiple accounts (e.g., separate bank accounts within business)
- Attach receipt photos
- Dark mode / theming
- Backup/restore (local file export, since fully offline)

---

## 5. Design & Styling

**General principles:**
- Clear visual hierarchy — amount and category should be the largest, most scannable elements in any list
- Color-code income (green-ish) vs expense (red/neutral) throughout
- Keep the "add transaction" flow to 2 taps max — this is what makes logging actually stick
- Consistent icon per category so lists are scannable without reading text

**Minimalist flat design (as of the 2026-08-23 pass):**
- Solid colors only — no transparency, no blur, no shadows anywhere in the UI.
- Light/dark palette follows the system setting (`isSystemInDarkTheme()`); see
  `ui/theme/Color.kt`/`ui/theme/Theme.kt`/`ui/theme/ExtendedColors.kt` for both palettes.
- Component policy is "background XOR border, zero shadow" — which one applies is decided by
  component *type*: list rows and dialogs get a Surface fill and no border; buttons/FAB/selected
  chips/selected segments get a solid Primary fill and no border; unselected chips and text
  fields are border-only, no fill. Featured cards (Home's summary, Budgets' cards) get an Accent
  fill instead of Surface — everything else that used to read as "glass" now reads as flat
  Surface.
- `GlassCard`/`GlassRow`/`GlassButton`/`GlassTextField`/`GlassSegmentedControl`/`GlassDialog`/
  `GlassFilterChip`/`GlassFab` keep their names from the prior glass-era pass (renaming them
  would touch every call site for no behavioral benefit) but are now flat solid-color
  implementations — none of them draw a gradient border, blur, or shadow.
- `ConfirmButton` (`ui/components/ConfirmButton.kt`) is the one shared primary/affirmative action
  for every dialog and form — always labeled "Confirm", a pill shape, Income-green when enabled.
- `CalendarDialog` (`ui/components/CalendarDialog.kt`) replaces M3's stock `DatePickerDialog` for
  single-date fields (`DateField`); the Transactions screen's date *range* picker still uses M3's
  stock `DateRangePicker`, which has no flat custom replacement in this codebase.

---

## 6. Reports & Breakdowns

Add a time-period selector to the Reports screen: **Daily / Weekly / Monthly / Yearly**. Each respects the existing Personal/Business/Combined toggle, so views like "Business income, by month, this year" are just a filter + group-by combination — no new database tables needed, just query logic grouping `Transaction.date`.

- **Daily** — simple list view: today's transactions + running total (a chart isn't useful at this granularity)
- **Weekly** — bar chart, spending per day within the week
- **Monthly** — bar chart, spending per week (or per day) within the month; this is the main/default view
- **Yearly** — bar chart, totals per month across the year — good for spotting seasonal patterns, especially useful for business income

Implementation note: build one reusable "breakdown query" function that takes `(type: Personal/Business/Combined, period: Daily/Weekly/Monthly/Yearly, referenceDate)` and returns grouped totals — all four charts and the category pie chart can share this rather than writing separate queries per screen.

---

## 8. Localization

- **Languages:** Albanian (default) and English
- Use Android's standard string resources setup: `res/values/strings.xml` (default — Albanian) and `res/values-en/strings.xml` (English override)
- All user-facing text (labels, category names you ship as defaults, buttons, chart labels, date/month names) goes through string resources — no hardcoded text in Compose code
- Number/currency formatting should respect locale too (decimal separator, currency symbol) — use `NumberFormat.getCurrencyInstance(locale)` rather than manual string formatting
- Add a language toggle in Settings so the user can switch to English regardless of phone system language, rather than relying only on system locale
- Date formatting (day/week/month/year breakdowns in Reports) should also be locale-aware — Albanian month/day names differ from English

---

## 9. Notes for Claude Code

- Start with the Room database schema + DAOs first, then build ViewModels, then Compose screens — this order lets you test data logic before UI exists.
- Since it's fully offline, no need for auth, network permissions, or cloud sync — keep the manifest minimal.
- Recurring transaction generation: check `nextDueDate <= today` on app launch (or via WorkManager for background checks), generate the transaction, then advance `nextDueDate`.
- Consider a local export/backup feature early (e.g., export DB to a JSON/CSV file in Downloads) since there's no cloud sync — it's your only safety net against data loss on phone reset/loss.
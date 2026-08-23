# Multi-Currency Support

Status: approved in chat 2026-08-22 (initial proposal) + 2026-08-22 (SQL aggregation approach and
rate-timing decision, resolved during planning). Implementing now.

## Currencies

`MKD, EUR, USD, CHF` as a new `Currency` enum in `data/Currency.kt`. `MKD` is the app-wide default
(display currency default, `Category.budgetLimitCurrency` default).

## Rate source

`open.er-api.com/v6/latest/USD` — free, no API key, updates daily, explicitly permits caching.
Confirmed by fetching the live endpoint that it returns `EUR`, `MKD`, `CHF`, and `USD` rates.
Frankfurter (the other no-key candidate) was rejected: its docs page claims MKD support but its
live `/latest` endpoint does not actually return an MKD rate (confirmed by querying it directly).

Attribution required by the API's terms: a visible "Rates by Exchange Rate API" link, placed in
Settings next to the last-updated timestamp.

## Rate timing (resolved during planning)

Every conversion — for a transaction from today or three years ago — uses the latest cached rate.
No per-transaction rate-locking. This is simpler, matches what most personal-finance apps do, and
means historical reports can shift slightly if rates move, which is an accepted tradeoff.

## Data model

- `Transaction.currency: Currency = Currency.MKD` (new field, defaulted so no existing call site
  breaks)
- `RecurringRule.currency: Currency = Currency.MKD` (new field; generated transactions inherit it)
- `Category.budgetLimitCurrency: Currency = Currency.MKD` (new field; only meaningful when
  `budgetLimit != null`)
- `AppDatabase` version 1 → 2, `fallbackToDestructiveMigration()` — no shipped migration path
  exists yet and the app hasn't released, so destructive is acceptable; flagged if real on-device
  data needs preserving.
- New Room entity `ExchangeRateCache` (single row, id fixed at 0): `rateEur, rateMkd, rateChf:
  Double` (rate = units of that currency per 1 USD; USD's own rate is implicitly 1.0 and not
  stored), `lastUpdatedEpochMillis: Long?` (null = never fetched — defaults to 1.0 for every
  currency until the first successful fetch, i.e. a neutral fallback rather than a crash or a
  blocked UI).

## Conversion

`ExchangeRateRepository.convert(amount, from, to, cache)` — pivots through USD:
`amount / rateOf(from) * rateOf(to)`. Pure, no I/O, unit-tested directly.

`ExchangeRateRepository.refreshIfStale()`: if cache is missing or older than 24h (matching the
API's own refresh cadence) AND the device is online (`ConnectivityManager`), fetch and replace the
cache row. Called on app launch. A manual "Refresh" action in Settings calls a `forceRefresh()`
variant that skips the staleness check but still requires connectivity.

## SQL aggregation (resolved during planning)

`TransactionDao`'s report queries currently do `SUM(amount) ... GROUP BY <bucket>` directly in
SQL. That's incorrect once `amount` values can be in different currencies — SQLite has no idea
about exchange rates. Fix: add `currency` as a second `GROUP BY` dimension in
`getCategoryTotals`/`getDailyTotals`/`getWeeklyTotals`/`getMonthlyTotals`/`getYearlyTotals`, so SQL
still does what it's good at (date bucketing, filtering) and returns one row per
`(bucket, currency)` pair instead of one row per bucket. `ReportsViewModel` then folds those rows
down to a single converted total per bucket in Kotlin, where the rates live. Same treatment for
`BudgetViewModel.spentByCategory`, which currently sums raw `amount` grouped by category in
Kotlin — it gains a `displayCurrency`/`rates` parameter and converts each transaction before
summing (already grouped in Kotlin, not SQL, so no query change needed there, just a signature
change).

`Category.budgetLimit` is compared against this converted `spent` figure — since `budgetLimit` now
carries its own `budgetLimitCurrency`, the limit is converted to display currency too, so both
sides of the comparison end up in the same currency before `BudgetsScreen` ever sees them (that
screen needs zero changes — it already just displays two already-comparable numbers).

Home's income/expense summary (currently computed inline in `HomeScreen.kt` via
`transactions.filter{...}.sumOf{it.amount}`) moves into `TransactionViewModel` as proper converted
`StateFlow`s, matching how `BudgetViewModel`/`ReportsViewModel` already own their derived state —
`HomeScreen` becomes a pure consumer instead of doing money math itself.

## Formatting (required fix, not optional)

`rememberCurrencyFormat()` currently derives the currency symbol from the *language* locale
(Albanian → Lek, English → USD) via `NumberFormat.getCurrencyInstance(locale)`. That's actively
wrong once real per-transaction currencies exist. Fix: take an explicit `Currency` parameter and
set `.currency = java.util.Currency.getInstance(currency.name)` on the returned `NumberFormat`,
while still using the locale for digit grouping / decimal separator conventions.

## UI

- Add/Edit Transaction: currency picker (segmented buttons, same visual pattern as the existing
  Personal/Business toggle), defaulting to the current display currency for new transactions, or
  the transaction's own currency when editing.
- Add/Edit Recurring Rule: same picker, same default logic.
- Settings: display-currency picker (default MKD), last-updated timestamp, manual "Refresh"
  action, attribution link.
- Transaction rows: a small original-currency tag when a transaction's currency differs from the
  display currency, so the original entry isn't invisible day-to-day.
- `INTERNET` permission is the only manifest change — the only network access in the app, and only
  for this feature.

## Out of scope

- No per-transaction rate-locking (see Rate timing above).
- No additional currencies beyond the four requested.
- No changes to the backup/restore file *format* beyond adding the three new currency fields —
  existing backups without those fields aren't expected to exist (app hasn't shipped).

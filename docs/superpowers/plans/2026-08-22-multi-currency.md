# Multi-Currency Support Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Every transaction and recurring rule stores its own currency (MKD/EUR/USD/CHF); a display-currency setting (default MKD) converts all totals, budgets, and reports for display using cached exchange rates, without ever altering the stored original amount/currency.

**Architecture:** New `Currency` enum threaded through `Transaction`, `RecurringRule`, and `Category.budgetLimitCurrency`. A new Room-backed `ExchangeRateCache` (single row) holds the last-fetched rates; `ExchangeRateRepository` owns fetching (plain `HttpURLConnection`, no new HTTP dependency), staleness/connectivity checks, and the pure `convert()`/`rateOf()` math. SQL aggregation queries (`TransactionDao`'s category/period totals) gain `currency` as a second `GROUP BY` dimension since SQLite can't sum mixed currencies; `ReportsViewModel`/`BudgetViewModel` fold those per-currency rows into display-currency totals in Kotlin, where the rates live. UI screens become currency-aware consumers of already-converted ViewModel state; only the Add/Edit screens and Settings do their own currency-picker UI work.

**Tech Stack:** Room (existing), plain `HttpURLConnection` + `org.json` (already a dependency) for the network call, Kotlin coroutines/Flow (existing patterns throughout).

**Spec:** `docs/superpowers/specs/2026-08-22-multi-currency-design.md`

## Global Constraints

- No new Gradle dependencies (no Retrofit/OkHttp/DataStore) — `HttpURLConnection` + `org.json` for the network call, Room for the cache, `SharedPreferences` for the display-currency setting.
- `INTERNET` is the only new manifest permission, and the only network access anywhere in the app.
- Every conversion uses the *latest* cached rate — no per-transaction rate-locking (confirmed decision, see spec).
- `AppDatabase` version bumps to 2 with `fallbackToDestructiveMigration(dropAllTables = true)` — no shipped migration path exists yet and the app hasn't released.
- Every task must leave the project compiling (`./gradlew compileDebugKotlin`) and the JVM unit test suite green (`./gradlew testDebugUnitTest`). Instrumented (`androidTest`) changes are written for correctness but can't be executed in this environment (no `adb`/emulator) — note this at the relevant steps rather than skipping the test code.

---

### Task 1: Currency enum, entity fields, converters, backup serialization

**Files:**
- Create: `app/src/main/java/com/example/financeflow/data/Currency.kt`
- Modify: `app/src/main/java/com/example/financeflow/data/Transaction.kt`
- Modify: `app/src/main/java/com/example/financeflow/data/RecurringRule.kt`
- Modify: `app/src/main/java/com/example/financeflow/data/Category.kt`
- Modify: `app/src/main/java/com/example/financeflow/data/Converters.kt`
- Modify: `app/src/main/java/com/example/financeflow/data/backup/BackupSerializer.kt`
- Modify: `app/src/test/java/com/example/financeflow/data/ConvertersTest.kt`
- Modify: `app/src/test/java/com/example/financeflow/data/backup/BackupSerializerTest.kt`

**Interfaces:**
- Produces: `enum class Currency { MKD, EUR, USD, CHF }`; `Transaction.currency: Currency`, `RecurringRule.currency: Currency`, `Category.budgetLimitCurrency: Currency` — all defaulted to `Currency.MKD` so no existing call site breaks.

- [ ] **Step 1: Create the enum**

```kotlin
package com.example.financeflow.data

enum class Currency { MKD, EUR, USD, CHF }
```

- [ ] **Step 2: Add the field to `Transaction`**

In `Transaction.kt`, add `val currency: Currency = Currency.MKD,` immediately after `val amount: Double,`.

- [ ] **Step 3: Add the field to `RecurringRule`**

In `RecurringRule.kt`, add `val currency: Currency = Currency.MKD,` immediately after `val amount: Double,`.

- [ ] **Step 4: Add the field to `Category`**

In `Category.kt`, add `val budgetLimitCurrency: Currency = Currency.MKD,` immediately after `val budgetLimit: Double? = null,`.

- [ ] **Step 5: Add the type converter**

In `Converters.kt`, add:

```kotlin
@TypeConverter
fun fromCurrency(value: String?): Currency? = value?.let { Currency.valueOf(it) }

@TypeConverter
fun toCurrency(currency: Currency?): String? = currency?.name
```

- [ ] **Step 6: Update `ConvertersTest`**

Add to the `enumsRoundTrip` test body:

```kotlin
assertEquals(Currency.EUR, converters.fromCurrency(converters.toCurrency(Currency.EUR)))
```

- [ ] **Step 7: Update `BackupSerializer` to carry the new fields**

In `Transaction.toJson()`, add `put("currency", currency.name)`. In `JSONObject.toTransaction()`, add `currency = Currency.valueOf(getString("currency")),`.

In `RecurringRule.toJson()`, add `put("currency", currency.name)`. In `JSONObject.toRecurringRule()`, add `currency = Currency.valueOf(getString("currency")),`.

In `Category.toJson()`, add `put("budgetLimitCurrency", budgetLimitCurrency.name)`. In `JSONObject.toCategory()`, add `budgetLimitCurrency = Currency.valueOf(getString("budgetLimitCurrency")),`.

Add the import: `import com.example.financeflow.data.Currency`.

- [ ] **Step 8: Update `BackupSerializerTest` to exercise non-default currencies**

Change the first test's `Category(id = 1, ...)` to add `budgetLimitCurrency = Currency.EUR`, the first `RecurringRule` to add `currency = Currency.USD`, and the first `Transaction` to add `currency = Currency.CHF`. Add `import com.example.financeflow.data.Currency`. This proves the round-trip actually carries the new fields instead of just passing by coincidence via shared defaults.

- [ ] **Step 9: Compile and test**

Run: `./gradlew compileDebugKotlin testDebugUnitTest -q`
Expected: BUILD SUCCESSFUL, all tests pass including the updated `ConvertersTest` and `BackupSerializerTest`.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/example/financeflow/data/Currency.kt app/src/main/java/com/example/financeflow/data/Transaction.kt app/src/main/java/com/example/financeflow/data/RecurringRule.kt app/src/main/java/com/example/financeflow/data/Category.kt app/src/main/java/com/example/financeflow/data/Converters.kt app/src/main/java/com/example/financeflow/data/backup/BackupSerializer.kt app/src/test/java/com/example/financeflow/data/ConvertersTest.kt app/src/test/java/com/example/financeflow/data/backup/BackupSerializerTest.kt
git commit -m "Add Currency enum and thread it through Transaction/RecurringRule/Category"
```

---

### Task 2: Exchange rate cache, repository, display-currency preference

**Files:**
- Create: `app/src/main/java/com/example/financeflow/data/ExchangeRateCache.kt`
- Create: `app/src/main/java/com/example/financeflow/data/ExchangeRateDao.kt`
- Create: `app/src/main/java/com/example/financeflow/data/repository/ExchangeRateRepository.kt`
- Create: `app/src/main/java/com/example/financeflow/locale/CurrencyPreferences.kt`
- Create: `app/src/test/java/com/example/financeflow/data/repository/ExchangeRateRepositoryTest.kt`
- Modify: `app/src/main/java/com/example/financeflow/data/AppDatabase.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: `Currency` from Task 1.
- Produces: `ExchangeRateRepository.convert(amount, from, to, cache): Double` and `ExchangeRateRepository.rateOf(currency, cache): Double` (pure, static — used by Task 3's ViewModel folding logic); `ExchangeRateRepository(dao, context).rates: Flow<ExchangeRateCache>`, `.refreshIfStale()`, `.forceRefresh()` (used by Task 3's ViewModel wiring and Task 6's Settings screen); `CurrencyPreferences.flow(context): StateFlow<Currency>` and `CurrencyPreferences.set(context, currency)` (used by Tasks 3, 4, 5, 6).

- [ ] **Step 1: Create the cache entity**

```kotlin
package com.example.financeflow.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Single-row cache (id is always 0) of the last successfully fetched USD-pivoted rates. Rates
// default to 1.0 (a neutral fallback, not a crash or a blocked UI) until the first fetch succeeds.
@Entity(tableName = "exchange_rate_cache")
data class ExchangeRateCache(
    @PrimaryKey val id: Int = 0,
    val rateEur: Double = 1.0,
    val rateMkd: Double = 1.0,
    val rateChf: Double = 1.0,
    val lastUpdatedEpochMillis: Long? = null
)
```

- [ ] **Step 2: Create the DAO**

```kotlin
package com.example.financeflow.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {
    @Query("SELECT * FROM exchange_rate_cache WHERE id = 0")
    fun observe(): Flow<ExchangeRateCache?>

    @Query("SELECT * FROM exchange_rate_cache WHERE id = 0")
    suspend fun getOnce(): ExchangeRateCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cache: ExchangeRateCache)
}
```

- [ ] **Step 3: Wire it into `AppDatabase`**

```kotlin
@Database(
    entities = [Transaction::class, Category::class, RecurringRule::class, ExchangeRateCache::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun exchangeRateDao(): ExchangeRateDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "financeflow.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { INSTANCE = it }
            }
    }
}
```

If `fallbackToDestructiveMigration(dropAllTables = true)` doesn't match the Room 2.8.4 API surface when compiling, use whichever single-argument-free or named-argument overload the compiler error names — the requirement is destructive fallback, not a specific overload spelling.

- [ ] **Step 4: Create `ExchangeRateRepository`**

```kotlin
package com.example.financeflow.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.financeflow.data.Currency
import com.example.financeflow.data.ExchangeRateCache
import com.example.financeflow.data.ExchangeRateDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class ExchangeRateRepository(
    private val dao: ExchangeRateDao,
    private val context: Context
) {
    val rates: Flow<ExchangeRateCache> = dao.observe().map { it ?: ExchangeRateCache() }

    // Called on app launch — matches the API's own ~24h refresh cadence, so there's no point
    // fetching more often even if the app is opened repeatedly in a day.
    suspend fun refreshIfStale() {
        val current = dao.getOnce()
        val staleMillis = TimeUnit.HOURS.toMillis(24)
        val isStale = current?.lastUpdatedEpochMillis == null ||
            System.currentTimeMillis() - current.lastUpdatedEpochMillis > staleMillis
        if (isStale) refresh()
    }

    // Called from Settings' manual "Refresh" action — skips the staleness check but still
    // requires connectivity, so tapping it while offline is a silent no-op, not an error state.
    suspend fun forceRefresh() = refresh()

    private suspend fun refresh() {
        if (!isOnline()) return
        val fetched = fetchUsdRates() ?: return
        dao.upsert(
            ExchangeRateCache(
                rateEur = fetched.eur,
                rateMkd = fetched.mkd,
                rateChf = fetched.chf,
                lastUpdatedEpochMillis = System.currentTimeMillis()
            )
        )
    }

    private fun isOnline(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private data class FetchedRates(val eur: Double, val mkd: Double, val chf: Double)

    private suspend fun fetchUsdRates(): FetchedRates? = withContext(Dispatchers.IO) {
        try {
            val connection = URL("https://open.er-api.com/v6/latest/USD").openConnection() as HttpURLConnection
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            val body = connection.inputStream.use { it.bufferedReader().readText() }
            val rates = JSONObject(body).getJSONObject("rates")
            FetchedRates(
                eur = rates.getDouble("EUR"),
                mkd = rates.getDouble("MKD"),
                chf = rates.getDouble("CHF")
            )
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        fun rateOf(currency: Currency, cache: ExchangeRateCache): Double = when (currency) {
            Currency.USD -> 1.0
            Currency.EUR -> cache.rateEur
            Currency.MKD -> cache.rateMkd
            Currency.CHF -> cache.rateChf
        }

        fun convert(amount: Double, from: Currency, to: Currency, cache: ExchangeRateCache): Double =
            if (from == to) amount else amount / rateOf(from, cache) * rateOf(to, cache)
    }
}
```

- [ ] **Step 5: Write the unit test for the pure conversion math**

```kotlin
package com.example.financeflow.data.repository

import com.example.financeflow.data.Currency
import com.example.financeflow.data.ExchangeRateCache
import org.junit.Assert.assertEquals
import org.junit.Test

class ExchangeRateRepositoryTest {
    private val cache = ExchangeRateCache(rateEur = 0.85, rateMkd = 51.0, rateChf = 0.80)

    @Test
    fun convert_sameCurrency_returnsAmountUnchanged() {
        assertEquals(100.0, ExchangeRateRepository.convert(100.0, Currency.EUR, Currency.EUR, cache), 0.0001)
    }

    @Test
    fun convert_pivotsThroughUsd() {
        // 85 EUR -> USD (85 / 0.85 = 100) -> MKD (100 * 51.0 = 5100)
        assertEquals(5100.0, ExchangeRateRepository.convert(85.0, Currency.EUR, Currency.MKD, cache), 0.01)
    }

    @Test
    fun convert_usdIsIdentityPivot() {
        assertEquals(0.85, ExchangeRateRepository.convert(1.0, Currency.USD, Currency.EUR, cache), 0.0001)
    }
}
```

Run: `./gradlew testDebugUnitTest --tests "com.example.financeflow.data.repository.ExchangeRateRepositoryTest" -q`
Expected: 3 tests pass.

- [ ] **Step 6: Create `CurrencyPreferences`**

```kotlin
package com.example.financeflow.locale

import android.content.Context
import androidx.core.content.edit
import com.example.financeflow.data.Currency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val PREFS_NAME = "settings"
private const val KEY_DISPLAY_CURRENCY = "display_currency"

/**
 * Unlike [LocalePreferences] (which needs an Activity recreate to re-resolve string resources),
 * currency display is just recomputed formatting — so this exposes a live StateFlow instead,
 * letting Home/Budgets/Reports react immediately when the user changes it in Settings.
 */
object CurrencyPreferences {
    private var state: MutableStateFlow<Currency>? = null

    fun flow(context: Context): StateFlow<Currency> =
        state ?: MutableStateFlow(read(context)).also { state = it }

    fun set(context: Context, currency: Currency) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_DISPLAY_CURRENCY, currency.name)
        }
        val flow = state ?: MutableStateFlow(currency).also { state = it }
        flow.value = currency
    }

    private fun read(context: Context): Currency =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DISPLAY_CURRENCY, null)
            ?.let { runCatching { Currency.valueOf(it) }.getOrNull() }
            ?: Currency.MKD
}
```

- [ ] **Step 7: Add the `INTERNET` permission**

In `AndroidManifest.xml`, add `<uses-permission android:name="android.permission.INTERNET" />` on its own line immediately before the `<application` tag.

- [ ] **Step 8: Compile and test**

Run: `./gradlew compileDebugKotlin testDebugUnitTest -q`
Expected: BUILD SUCCESSFUL, all tests pass (the new `ExchangeRateRepositoryTest` plus everything from Task 1).

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/example/financeflow/data/ExchangeRateCache.kt app/src/main/java/com/example/financeflow/data/ExchangeRateDao.kt app/src/main/java/com/example/financeflow/data/repository/ExchangeRateRepository.kt app/src/main/java/com/example/financeflow/locale/CurrencyPreferences.kt app/src/test/java/com/example/financeflow/data/repository/ExchangeRateRepositoryTest.kt app/src/main/java/com/example/financeflow/data/AppDatabase.kt app/src/main/AndroidManifest.xml
git commit -m "Add exchange rate cache, repository, and display-currency preference"
```

---

### Task 3: Currency-correct aggregation in the ViewModel layer

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/data/PeriodTotal.kt`
- Modify: `app/src/main/java/com/example/financeflow/data/TransactionDao.kt`
- Modify: `app/src/main/java/com/example/financeflow/data/repository/TransactionRepository.kt`
- Modify: `app/src/main/java/com/example/financeflow/viewmodel/BudgetViewModel.kt`
- Modify: `app/src/main/java/com/example/financeflow/viewmodel/ReportsViewModel.kt`
- Modify: `app/src/main/java/com/example/financeflow/viewmodel/TransactionViewModel.kt`
- Modify: `app/src/main/java/com/example/financeflow/viewmodel/ViewModelProviders.kt`
- Modify: `app/src/test/java/com/example/financeflow/viewmodel/BudgetViewModelTest.kt`
- Create: `app/src/test/java/com/example/financeflow/viewmodel/TransactionViewModelTest.kt`
- Create: `app/src/test/java/com/example/financeflow/viewmodel/ReportsViewModelFoldTest.kt`

**Interfaces:**
- Consumes: `ExchangeRateRepository.convert`/`rateOf`, `ExchangeRateCache`, `CurrencyPreferences.flow` from Task 2.
- Produces: `BudgetViewModel.budgets`, `ReportsViewModel.categoryBreakdown`/`barChartData`, `TransactionViewModel.monthSummary` all stay as their existing public `StateFlow` shapes but now emit already-display-currency-converted values — **no screen needs to change to consume correctly-converted numbers** (screens still need the currency-aware *format symbol*, handled in Task 4).

- [ ] **Step 1: Add the raw per-currency total types**

In `PeriodTotal.kt`, add two new data classes alongside the existing ones:

```kotlin
data class PeriodCurrencyTotal(
    val bucket: String,
    val currency: Currency,
    val income: Double,
    val expense: Double
)

data class CategoryCurrencyTotal(
    val categoryId: Long,
    val currency: Currency,
    val total: Double
)
```

- [ ] **Step 2: Rewrite `TransactionDao`'s aggregation queries to group by currency too**

Replace `getCategoryTotals`:

```kotlin
@Query(
    """
    SELECT categoryId, currency, SUM(amount) AS total
    FROM transactions
    WHERE isIncome = 0
      AND (:type IS NULL OR type = :type)
      AND date BETWEEN :start AND :end
    GROUP BY categoryId, currency
    """
)
fun getCategoryTotals(type: TransactionType?, start: LocalDate, end: LocalDate): Flow<List<CategoryCurrencyTotal>>
```

Replace `getDailyTotals`, `getWeeklyTotals`, `getMonthlyTotals`, `getYearlyTotals` — each gets `currency` added to the `SELECT` list and the `GROUP BY`, and its return type changes to `Flow<List<PeriodCurrencyTotal>>`. Example for `getDailyTotals` (apply the same `currency` addition to the other three, keeping their existing `strftime`/`date` bucket expressions untouched):

```kotlin
@Query(
    """
    SELECT date(date * 86400, 'unixepoch') AS bucket, currency,
           SUM(CASE WHEN isIncome = 1 THEN amount ELSE 0 END) AS income,
           SUM(CASE WHEN isIncome = 0 THEN amount ELSE 0 END) AS expense
    FROM transactions
    WHERE date BETWEEN :start AND :end AND (:type IS NULL OR type = :type)
    GROUP BY bucket, currency
    ORDER BY bucket ASC
    """
)
fun getDailyTotals(start: LocalDate, end: LocalDate, type: TransactionType?): Flow<List<PeriodCurrencyTotal>>
```

- [ ] **Step 3: Update `TransactionRepository`'s passthrough return types**

`getTotals` now returns `Flow<List<PeriodCurrencyTotal>>` and `getCategoryTotals` returns `Flow<List<CategoryCurrencyTotal>>` — same method bodies, just following the DAO's new return types (Kotlin will require the signature update; no logic changes in this file).

- [ ] **Step 4: Fold the raw per-currency rows into display-currency totals in `ReportsViewModel`**

```kotlin
package com.example.financeflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeflow.data.Category
import com.example.financeflow.data.CategoryCurrencyTotal
import com.example.financeflow.data.CategoryTotal
import com.example.financeflow.data.Currency
import com.example.financeflow.data.ExchangeRateCache
import com.example.financeflow.data.PeriodCurrencyTotal
import com.example.financeflow.data.PeriodTotal
import com.example.financeflow.data.ReportPeriod
import com.example.financeflow.data.Transaction
import com.example.financeflow.data.TransactionType
import com.example.financeflow.data.dateRangeFor
import com.example.financeflow.data.repository.CategoryRepository
import com.example.financeflow.data.repository.ExchangeRateRepository
import com.example.financeflow.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class CategorySlice(val category: Category, val total: Double)

@OptIn(ExperimentalCoroutinesApi::class)
class ReportsViewModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val displayCurrency: StateFlow<Currency>
) : ViewModel() {

    private val typeFilter = MutableStateFlow<TransactionType?>(null)
    private val period = MutableStateFlow(ReportPeriod.MONTH)

    val selectedType: StateFlow<TransactionType?> = typeFilter.asStateFlow()
    val selectedPeriod: StateFlow<ReportPeriod> = period.asStateFlow()

    private val selection = combine(typeFilter, period) { type, p -> type to p }
    private val currencyContext = combine(displayCurrency, exchangeRateRepository.rates) { display, rates -> display to rates }

    val dailyTransactions: StateFlow<List<Transaction>> = selection.flatMapLatest { (type, p) ->
        if (p != ReportPeriod.DAY) return@flatMapLatest flowOf(emptyList())
        val (start, end) = dateRangeFor(p, LocalDate.now())
        transactionRepository.getByTypeAndDateRange(type, start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val barChartData: StateFlow<List<PeriodTotal>> = combine(selection, currencyContext) { s, c -> s to c }
        .flatMapLatest { (selectionPair, currencyPair) ->
            val (type, p) = selectionPair
            val (display, rates) = currencyPair
            val (start, end) = dateRangeFor(p, LocalDate.now())
            val raw = when (p) {
                ReportPeriod.DAY -> flowOf(emptyList())
                ReportPeriod.WEEK, ReportPeriod.MONTH -> transactionRepository.getTotals(type, ReportPeriod.DAY, start, end)
                ReportPeriod.YEAR -> transactionRepository.getTotals(type, ReportPeriod.MONTH, start, end)
            }
            raw.map { foldPeriodTotals(it, display, rates) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val categoryBreakdown: StateFlow<List<CategorySlice>> = combine(selection, currencyContext) { s, c -> s to c }
        .flatMapLatest { (selectionPair, currencyPair) ->
            val (type, p) = selectionPair
            val (display, rates) = currencyPair
            val (start, end) = dateRangeFor(p, LocalDate.now())
            combine(
                transactionRepository.getCategoryTotals(type, start, end),
                categoryRepository.getAll()
            ) { rawTotals, categories ->
                val byId = categories.associateBy { it.id }
                foldCategoryTotals(rawTotals, display, rates).mapNotNull { t ->
                    byId[t.categoryId]?.let { category -> CategorySlice(category, t.total) }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTypeFilter(type: TransactionType?) {
        typeFilter.value = type
    }

    fun setPeriod(newPeriod: ReportPeriod) {
        period.value = newPeriod
    }

    companion object {
        fun foldPeriodTotals(raw: List<PeriodCurrencyTotal>, display: Currency, rates: ExchangeRateCache): List<PeriodTotal> =
            raw.groupBy { it.bucket }
                .map { (bucket, rows) ->
                    PeriodTotal(
                        bucket = bucket,
                        income = rows.sumOf { ExchangeRateRepository.convert(it.income, it.currency, display, rates) },
                        expense = rows.sumOf { ExchangeRateRepository.convert(it.expense, it.currency, display, rates) }
                    )
                }
                .sortedBy { it.bucket }

        fun foldCategoryTotals(raw: List<CategoryCurrencyTotal>, display: Currency, rates: ExchangeRateCache): List<CategoryTotal> =
            raw.groupBy { it.categoryId }
                .map { (categoryId, rows) ->
                    CategoryTotal(
                        categoryId = categoryId,
                        total = rows.sumOf { ExchangeRateRepository.convert(it.total, it.currency, display, rates) }
                    )
                }
                .sortedByDescending { it.total }
    }
}
```

- [ ] **Step 5: Write unit tests for the two fold functions**

```kotlin
package com.example.financeflow.viewmodel

import com.example.financeflow.data.CategoryCurrencyTotal
import com.example.financeflow.data.Currency
import com.example.financeflow.data.ExchangeRateCache
import com.example.financeflow.data.PeriodCurrencyTotal
import org.junit.Assert.assertEquals
import org.junit.Test

class ReportsViewModelFoldTest {
    private val cache = ExchangeRateCache(rateEur = 0.85, rateMkd = 51.0, rateChf = 0.80)

    @Test
    fun foldPeriodTotals_convertsAndSumsAcrossCurrenciesPerBucket() {
        val raw = listOf(
            PeriodCurrencyTotal(bucket = "2026-08-01", currency = Currency.USD, income = 100.0, expense = 0.0),
            PeriodCurrencyTotal(bucket = "2026-08-01", currency = Currency.EUR, income = 0.0, expense = 85.0)
        )

        val folded = ReportsViewModel.foldPeriodTotals(raw, Currency.MKD, cache)

        assertEquals(1, folded.size)
        // 100 USD -> MKD = 5100; 85 EUR -> USD (100) -> MKD = 5100
        assertEquals(5100.0, folded[0].income, 0.01)
        assertEquals(5100.0, folded[0].expense, 0.01)
    }

    @Test
    fun foldCategoryTotals_convertsAndSumsAcrossCurrenciesPerCategory() {
        val raw = listOf(
            CategoryCurrencyTotal(categoryId = 1, currency = Currency.USD, total = 100.0),
            CategoryCurrencyTotal(categoryId = 1, currency = Currency.EUR, total = 85.0)
        )

        val folded = ReportsViewModel.foldCategoryTotals(raw, Currency.MKD, cache)

        assertEquals(1, folded.size)
        assertEquals(5100.0, folded[0].total, 0.01)
    }
}
```

- [ ] **Step 6: Update `BudgetViewModel` to convert spent amounts and budget limits**

```kotlin
package com.example.financeflow.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.financeflow.data.Category
import com.example.financeflow.data.Currency
import com.example.financeflow.data.ExchangeRateCache
import com.example.financeflow.data.Transaction
import com.example.financeflow.data.TransactionType
import com.example.financeflow.data.categoryTypeFor
import com.example.financeflow.data.repository.CategoryRepository
import com.example.financeflow.data.repository.ExchangeRateRepository
import com.example.financeflow.data.repository.TransactionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class CategoryBudget(
    val category: Category,
    val spent: Double,
    val limit: Double
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val displayCurrency: StateFlow<Currency>
) : ViewModel() {

    private val typeFilter = MutableStateFlow<TransactionType?>(null)

    val currentTypeFilter: StateFlow<TransactionType?> = typeFilter.asStateFlow()

    private val currencyContext = combine(displayCurrency, exchangeRateRepository.rates) { display, rates -> display to rates }

    val budgets: StateFlow<List<CategoryBudget>> = combine(typeFilter, currencyContext) { type, currencyPair -> type to currencyPair }
        .flatMapLatest { (type, currencyPair) ->
            val (display, rates) = currencyPair
            val monthStart = LocalDate.now().withDayOfMonth(1)
            val monthEnd = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth())
            combine(
                categoryRepository.getByType(type?.let(::categoryTypeFor)),
                transactionRepository.getByTypeAndDateRange(type, monthStart, monthEnd)
            ) { categories, transactions ->
                val spent = spentByCategory(transactions, display, rates)
                categories.mapNotNull { category ->
                    category.budgetLimit?.let { limit ->
                        val convertedLimit = ExchangeRateRepository.convert(limit, category.budgetLimitCurrency, display, rates)
                        CategoryBudget(category, spent[category.id] ?: 0.0, convertedLimit)
                    }
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTypeFilter(type: TransactionType?) {
        typeFilter.value = type
    }

    companion object {
        fun spentByCategory(transactions: List<Transaction>, display: Currency, rates: ExchangeRateCache): Map<Long, Double> =
            transactions
                .filter { !it.isIncome }
                .groupBy { it.categoryId }
                .mapValues { (_, txns) -> txns.sumOf { ExchangeRateRepository.convert(it.amount, it.currency, display, rates) } }
    }
}
```

- [ ] **Step 7: Update `BudgetViewModelTest` for the new `spentByCategory` signature**

```kotlin
package com.example.financeflow.viewmodel

import com.example.financeflow.data.Currency
import com.example.financeflow.data.ExchangeRateCache
import com.example.financeflow.data.Transaction
import com.example.financeflow.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class BudgetViewModelTest {

    @Test
    fun spentByCategory_sumsExpensesOnlyAndGroupsByCategory() {
        val transactions = listOf(
            Transaction(amount = 20.0, type = TransactionType.PERSONAL, categoryId = 1, date = LocalDate.now(), isIncome = false),
            Transaction(amount = 30.0, type = TransactionType.PERSONAL, categoryId = 1, date = LocalDate.now(), isIncome = false),
            Transaction(amount = 15.0, type = TransactionType.PERSONAL, categoryId = 2, date = LocalDate.now(), isIncome = false),
            Transaction(amount = 500.0, type = TransactionType.PERSONAL, categoryId = 1, date = LocalDate.now(), isIncome = true)
        )

        val result = BudgetViewModel.spentByCategory(transactions, Currency.MKD, ExchangeRateCache())

        assertEquals(50.0, result[1L])
        assertEquals(15.0, result[2L])
    }

    @Test
    fun spentByCategory_convertsEachTransactionToDisplayCurrency() {
        val cache = ExchangeRateCache(rateEur = 0.85, rateMkd = 51.0, rateChf = 0.80)
        val transactions = listOf(
            Transaction(amount = 100.0, type = TransactionType.PERSONAL, categoryId = 1, date = LocalDate.now(), isIncome = false, currency = Currency.USD)
        )

        val result = BudgetViewModel.spentByCategory(transactions, Currency.MKD, cache)

        assertEquals(5100.0, result[1L]!!, 0.01)
    }
}
```

- [ ] **Step 8: Give `TransactionViewModel` a converted month-summary StateFlow**

Add to `TransactionViewModel.kt` — new constructor params, new data class, new `StateFlow`, and a testable companion function:

```kotlin
class TransactionViewModel(
    private val repository: TransactionRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val displayCurrency: StateFlow<Currency>
) : ViewModel() {
    // ... existing properties and functions unchanged ...

    data class MonthSummary(val income: Double, val expense: Double)

    val monthSummary: StateFlow<MonthSummary> = combine(filteredTransactions, exchangeRateRepository.rates, displayCurrency) { transactions, rates, display ->
        MonthSummary(
            income = sumConverted(transactions.filter { it.isIncome }, display, rates),
            expense = sumConverted(transactions.filter { !it.isIncome }, display, rates)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthSummary(0.0, 0.0))

    companion object {
        fun sumConverted(transactions: List<Transaction>, display: Currency, rates: ExchangeRateCache): Double =
            transactions.sumOf { ExchangeRateRepository.convert(it.amount, it.currency, display, rates) }
    }
}
```

Add imports: `com.example.financeflow.data.Currency`, `com.example.financeflow.data.ExchangeRateCache`, `com.example.financeflow.data.repository.ExchangeRateRepository`.

- [ ] **Step 9: Write the unit test for `sumConverted`**

```kotlin
package com.example.financeflow.viewmodel

import com.example.financeflow.data.Currency
import com.example.financeflow.data.ExchangeRateCache
import com.example.financeflow.data.Transaction
import com.example.financeflow.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TransactionViewModelTest {
    @Test
    fun sumConverted_convertsEachTransactionBeforeSumming() {
        val cache = ExchangeRateCache(rateEur = 0.85, rateMkd = 51.0, rateChf = 0.80)
        val transactions = listOf(
            Transaction(amount = 100.0, type = TransactionType.PERSONAL, categoryId = 1, date = LocalDate.now(), isIncome = true, currency = Currency.USD),
            Transaction(amount = 85.0, type = TransactionType.PERSONAL, categoryId = 1, date = LocalDate.now(), isIncome = true, currency = Currency.EUR)
        )

        val result = TransactionViewModel.sumConverted(transactions, Currency.MKD, cache)

        // 100 USD -> MKD (5100) + 85 EUR -> USD (100) -> MKD (5100) = 10200
        assertEquals(10200.0, result, 0.01)
    }
}
```

- [ ] **Step 10: Wire the new dependencies into `ViewModelProviders`**

```kotlin
package com.example.financeflow.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.financeflow.data.AppDatabase
import com.example.financeflow.data.repository.CategoryRepository
import com.example.financeflow.data.repository.ExchangeRateRepository
import com.example.financeflow.data.repository.RecurringRuleRepository
import com.example.financeflow.data.repository.TransactionRepository
import com.example.financeflow.locale.CurrencyPreferences

@Composable
fun rememberTransactionViewModel(): TransactionViewModel {
    val context = LocalContext.current.applicationContext
    val repository = remember { TransactionRepository(AppDatabase.getInstance(context).transactionDao()) }
    val exchangeRateRepository = remember { ExchangeRateRepository(AppDatabase.getInstance(context).exchangeRateDao(), context) }
    val displayCurrency = remember { CurrencyPreferences.flow(context) }
    return viewModel(factory = viewModelFactory { initializer { TransactionViewModel(repository, exchangeRateRepository, displayCurrency) } })
}

@Composable
fun rememberCategoryViewModel(): CategoryViewModel {
    val context = LocalContext.current.applicationContext
    val repository = remember { CategoryRepository(AppDatabase.getInstance(context).categoryDao()) }
    return viewModel(factory = viewModelFactory { initializer { CategoryViewModel(repository) } })
}

@Composable
fun rememberBudgetViewModel(): BudgetViewModel {
    val context = LocalContext.current.applicationContext
    val db = remember { AppDatabase.getInstance(context) }
    val categoryRepository = remember { CategoryRepository(db.categoryDao()) }
    val transactionRepository = remember { TransactionRepository(db.transactionDao()) }
    val exchangeRateRepository = remember { ExchangeRateRepository(db.exchangeRateDao(), context) }
    val displayCurrency = remember { CurrencyPreferences.flow(context) }
    return viewModel(factory = viewModelFactory { initializer { BudgetViewModel(categoryRepository, transactionRepository, exchangeRateRepository, displayCurrency) } })
}

@Composable
fun rememberRecurringRuleViewModel(): RecurringRuleViewModel {
    val context = LocalContext.current.applicationContext
    val repository = remember { RecurringRuleRepository(AppDatabase.getInstance(context).recurringRuleDao()) }
    return viewModel(factory = viewModelFactory { initializer { RecurringRuleViewModel(repository) } })
}

@Composable
fun rememberReportsViewModel(): ReportsViewModel {
    val context = LocalContext.current.applicationContext
    val db = remember { AppDatabase.getInstance(context) }
    val transactionRepository = remember { TransactionRepository(db.transactionDao()) }
    val categoryRepository = remember { CategoryRepository(db.categoryDao()) }
    val exchangeRateRepository = remember { ExchangeRateRepository(db.exchangeRateDao(), context) }
    val displayCurrency = remember { CurrencyPreferences.flow(context) }
    return viewModel(factory = viewModelFactory { initializer { ReportsViewModel(transactionRepository, categoryRepository, exchangeRateRepository, displayCurrency) } })
}
```

- [ ] **Step 11: Compile and test**

Run: `./gradlew compileDebugKotlin testDebugUnitTest -q`
Expected: BUILD SUCCESSFUL. All tests pass, including the two updated/new ones from this task. The app itself won't run correctly yet end-to-end — screens still call the old zero-arg `rememberCurrencyFormat()` (fixed in Task 4) — but everything compiles and the ViewModel layer's own tests prove the conversion math is right.

- [ ] **Step 12: Commit**

```bash
git add app/src/main/java/com/example/financeflow/data/PeriodTotal.kt app/src/main/java/com/example/financeflow/data/TransactionDao.kt app/src/main/java/com/example/financeflow/data/repository/TransactionRepository.kt app/src/main/java/com/example/financeflow/viewmodel/BudgetViewModel.kt app/src/main/java/com/example/financeflow/viewmodel/ReportsViewModel.kt app/src/main/java/com/example/financeflow/viewmodel/TransactionViewModel.kt app/src/main/java/com/example/financeflow/viewmodel/ViewModelProviders.kt app/src/test/java/com/example/financeflow/viewmodel/BudgetViewModelTest.kt app/src/test/java/com/example/financeflow/viewmodel/TransactionViewModelTest.kt app/src/test/java/com/example/financeflow/viewmodel/ReportsViewModelFoldTest.kt
git commit -m "Convert all report/budget/summary aggregation to display currency"
```

---

### Task 4: Currency-aware formatting, wired into every display screen

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/locale/LocaleFormats.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/components/TransactionRow.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/HomeScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/TransactionsScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/BudgetsScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/ReportsScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/RecurringRulesScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/CategoriesScreen.kt`

**Interfaces:**
- Consumes: `Currency`, `CurrencyPreferences.flow`, `ExchangeRateRepository.convert` from Tasks 1–2; the already-converted `StateFlow`s from Task 3.
- Produces: `rememberCurrencyFormat(currency: Currency): NumberFormat` (breaking signature change — every call site updated in this same task).

- [ ] **Step 1: Fix `rememberCurrencyFormat` to take an explicit currency**

```kotlin
@Composable
fun rememberCurrencyFormat(currency: Currency): NumberFormat {
    val locale = currentAppLocale()
    return remember(locale, currency) {
        NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = java.util.Currency.getInstance(currency.name)
        }
    }
}
```

Add `import com.example.financeflow.data.Currency` to `LocaleFormats.kt`.

- [ ] **Step 2: Give `TransactionRow` an original-currency tag and a converted main figure**

`TransactionRow` has no access to exchange rates itself, so its caller (Home/Transactions screens,
in Steps 3–4 below) passes in the already-converted amount to display as the main figure, while
the small tag below the date always shows the transaction's true stored amount/currency:

```kotlin
@Composable
fun TransactionRow(
    transaction: Transaction,
    categoryName: String,
    displayAmount: Double,
    currencyFormat: NumberFormat,
    dateFormat: DateTimeFormatter,
    displayCurrency: Currency,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = categoryName, style = MaterialTheme.typography.bodyLarge)
            Text(text = transaction.date.format(dateFormat), style = MaterialTheme.typography.bodyMedium)
            if (transaction.currency != displayCurrency) {
                Text(
                    text = "${transaction.currency.name} ${"%.2f".format(transaction.amount)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        val amountColor = if (transaction.isIncome) Income else Expense
        val sign = if (transaction.isIncome) "+" else "-"
        Text(
            text = "$sign${currencyFormat.format(displayAmount)}",
            style = MoneyFigure,
            color = amountColor
        )
    }
}
```

`displayAmount` is the pre-converted figure the caller (Home/Transactions screens) computes using `ExchangeRateRepository.convert`.

- [ ] **Step 3: Update `HomeScreen` to use the converted month summary and pass currency context down**

Replace the inline `income`/`expense` calculation and wire `displayCurrency`/`rates` through:

```kotlin
@Composable
fun HomeScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    transactionViewModel: TransactionViewModel = rememberTransactionViewModel(),
    categoryViewModel: CategoryViewModel = rememberCategoryViewModel()
) {
    val context = LocalContext.current
    val transactions by transactionViewModel.filteredTransactions.collectAsState()
    val categories by categoryViewModel.categories.collectAsState()
    val categoryNames = remember(categories) { categories.associate { it.id to it.name } }

    val typeFilter by transactionViewModel.currentTypeFilter.collectAsState()
    val displayCurrency by CurrencyPreferences.flow(context).collectAsState()
    val exchangeRateRepository = remember { ExchangeRateRepository(AppDatabase.getInstance(context).exchangeRateDao(), context) }
    val rates by exchangeRateRepository.rates.collectAsState(initial = ExchangeRateCache())
    val currencyFormat = rememberCurrencyFormat(displayCurrency)
    val dateFormat = rememberDateFormat("MMM d")
    val uncategorized = stringResource(R.string.category_uncategorized)

    val summary by transactionViewModel.monthSummary.collectAsState()

    Scaffold(
        floatingActionButton = {
            GlassFab(onClick = onAddTransaction, contentDescription = stringResource(R.string.home_add_transaction_content_description)) {
                Icon(Icons.Rounded.Add, contentDescription = null)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            TransactionTypeToggle(
                selected = typeFilter,
                onSelect = transactionViewModel::setTypeFilter
            )

            Spacer(Modifier.height(16.dp))

            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.home_this_month_title), style = MaterialTheme.typography.titleLarge)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.toggle_income), style = MaterialTheme.typography.bodyMedium)
                    Text(text = currencyFormat.format(summary.income), style = MoneyFigure, color = Income)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = stringResource(R.string.home_expenses_label), style = MaterialTheme.typography.bodyMedium)
                    Text(text = currencyFormat.format(summary.expense), style = MoneyFigure, color = Expense)
                }
            }

            Spacer(Modifier.height(24.dp))

            Text(text = stringResource(R.string.home_recent_transactions_title), style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(8.dp))

            if (transactions.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(text = stringResource(R.string.transactions_empty), style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(transactions.take(5), key = { it.id }) { transaction ->
                        TransactionRow(
                            transaction = transaction,
                            categoryName = categoryNames[transaction.categoryId] ?: uncategorized,
                            displayAmount = ExchangeRateRepository.convert(transaction.amount, transaction.currency, displayCurrency, rates),
                            currencyFormat = currencyFormat,
                            dateFormat = dateFormat,
                            displayCurrency = displayCurrency,
                            onClick = { onEditTransaction(transaction.id) }
                        )
                    }
                }
            }
        }
    }
}
```

Add imports: `androidx.compose.ui.platform.LocalContext`, `com.example.financeflow.data.AppDatabase`, `com.example.financeflow.data.ExchangeRateCache`, `com.example.financeflow.data.repository.ExchangeRateRepository`, `com.example.financeflow.locale.CurrencyPreferences`.

- [ ] **Step 4: Update `TransactionsScreen` the same way**

Add the same `context`/`exchangeRateRepository`/`rates`/`displayCurrency` `remember`/`collectAsState` block used in Home, change `rememberCurrencyFormat()` to `rememberCurrencyFormat(displayCurrency)`, and update its `TransactionRow(...)` call to add `displayAmount = ExchangeRateRepository.convert(transaction.amount, transaction.currency, displayCurrency, rates)` and `displayCurrency = displayCurrency`.

- [ ] **Step 5: Update `BudgetsScreen`**

`budget.spent`/`budget.limit` are already display-currency-converted by `BudgetViewModel` (Task 3) — only the format symbol needs fixing:

```kotlin
val displayCurrency by CurrencyPreferences.flow(LocalContext.current).collectAsState()
val currencyFormat = rememberCurrencyFormat(displayCurrency)
```

Replace the existing `val currencyFormat = rememberCurrencyFormat()` line with this (add `androidx.compose.ui.platform.LocalContext` and `com.example.financeflow.locale.CurrencyPreferences` imports).

- [ ] **Step 6: Update `ReportsScreen`**

Replace `val currencyFormat = rememberCurrencyFormat()` with the same `displayCurrency`-driven version. `barChartData`/`categoryBreakdown` are already converted (Task 3) so `ReportBarChart`/`CategoryPieChart` need no signature changes. `DailyReportList` operates on raw per-transaction `Transaction` objects though (the `dailyTransactions` StateFlow is intentionally left unconverted in Task 3, since Day view is a per-transaction list, not an aggregate) — it needs `rates`/`displayCurrency` to convert each row and the running total:

```kotlin
@Composable
private fun DailyReportList(
    transactions: List<Transaction>,
    categoryNames: Map<Long, String>,
    uncategorized: String,
    currencyFormat: NumberFormat,
    displayCurrency: Currency,
    rates: ExchangeRateCache
) {
    if (transactions.isEmpty()) {
        Text(text = stringResource(R.string.report_no_transactions_today), style = MaterialTheme.typography.bodyMedium)
        return
    }

    val ordered = remember(transactions) { transactions.sortedWith(compareBy({ it.date }, { it.id })) }
    var running = 0.0

    Column {
        ordered.forEach { transaction ->
            val convertedAmount = ExchangeRateRepository.convert(transaction.amount, transaction.currency, displayCurrency, rates)
            running += if (transaction.isIncome) convertedAmount else -convertedAmount
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = categoryNames[transaction.categoryId] ?: uncategorized, style = MaterialTheme.typography.bodyLarge)
                    transaction.note?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
                }
                Column(horizontalAlignment = Alignment.End) {
                    val sign = if (transaction.isIncome) "+" else "-"
                    Text(
                        text = "$sign${currencyFormat.format(convertedAmount)}",
                        style = MoneyFigure,
                        color = if (transaction.isIncome) Income else Expense
                    )
                    Text(text = stringResource(R.string.report_running_total, currencyFormat.format(running)), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
```

Update its call site in `ReportsScreen` (`DailyReportList(dailyTransactions, categoryNames, uncategorized, currencyFormat)`) to add `displayCurrency, rates`, and add the same `exchangeRateRepository`/`rates` `remember`/`collectAsState` block as Home/Transactions. Add imports: `com.example.financeflow.data.Currency`, `com.example.financeflow.data.ExchangeRateCache`, `com.example.financeflow.data.repository.ExchangeRateRepository`, `com.example.financeflow.data.AppDatabase`, `com.example.financeflow.locale.CurrencyPreferences`, `androidx.compose.ui.platform.LocalContext`.

- [ ] **Step 7: Update `RecurringRulesScreen` to format each rule in its own currency**

A recurring rule's amount is shown in *its own* currency (like a settings/management list), not converted — remove the screen-level `val currencyFormat = rememberCurrencyFormat()` and compute it per-row instead:

```kotlin
@Composable
private fun RecurringRuleRow(
    rule: RecurringRule,
    categoryName: String,
    dateFormat: DateTimeFormatter,
    onClick: () -> Unit,
    onToggleActive: (Boolean) -> Unit
) {
    val currencyFormat = rememberCurrencyFormat(rule.currency)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(text = rule.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.recurring_row_summary, categoryName, frequencyLabel(rule), rule.nextDueDate.format(dateFormat)),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val sign = if (rule.isIncome) "+" else "-"
            Text(
                text = "$sign${currencyFormat.format(rule.amount)}",
                style = MoneyFigure,
                color = if (rule.isIncome) Income else Expense
            )
            Switch(checked = rule.active, onCheckedChange = onToggleActive)
        }
    }
}
```

Remove the `currencyFormat` parameter from `RecurringRuleRow`'s call site in `RecurringRulesScreen` (the `RecurringRuleRow(rule = rule, categoryName = ..., currencyFormat = currencyFormat, dateFormat = ...)` call loses the `currencyFormat = currencyFormat` argument) and remove the now-unused screen-level `val currencyFormat = rememberCurrencyFormat()` line.

- [ ] **Step 8: Update `CategoriesScreen` to format each category's budget in its own currency**

Same reasoning as recurring rules — a category's budget limit is shown in its own `budgetLimitCurrency`, not converted. Remove `CategoryRow`'s `currencyFormat: NumberFormat` parameter and compute it inside using `category.budgetLimitCurrency`:

```kotlin
@Composable
private fun CategoryRow(
    category: Category,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val currencyFormat = rememberCurrencyFormat(category.budgetLimitCurrency)
    val swatch = category.color.toCategoryColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(swatch.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(CategoryIcons.resolve(category.icon), contentDescription = null, tint = swatch)
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = category.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = buildString {
                        append(categoryTypeLabel(category.type))
                        category.budgetLimit?.let {
                            append(" · ")
                            append(stringResource(R.string.categories_budget_suffix, currencyFormat.format(it)))
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.categories_delete_content_description, category.name)
            )
        }
    }
}
```

Update its call site in `CategoriesScreen` to drop the `currencyFormat = currencyFormat` argument, and remove the now-unused screen-level `val currencyFormat = rememberCurrencyFormat()` line and its `rememberCurrencyFormat`/`NumberFormat` imports if nothing else in the file uses them (check first — `AddEditCategoryDialog` doesn't format currency, so this is safe to remove).

- [ ] **Step 9: Compile and manually verify**

Run: `./gradlew compileDebugKotlin testDebugUnitTest -q`
Expected: BUILD SUCCESSFUL, all tests pass.

Run the app: create a transaction in EUR while display currency is MKD (default) — confirm Home's totals and the transaction row both show MKD-formatted converted amounts, with the row's small "EUR 12.00" tag beneath the date.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/example/financeflow/locale/LocaleFormats.kt app/src/main/java/com/example/financeflow/ui/components/TransactionRow.kt app/src/main/java/com/example/financeflow/ui/screens/HomeScreen.kt app/src/main/java/com/example/financeflow/ui/screens/TransactionsScreen.kt app/src/main/java/com/example/financeflow/ui/screens/BudgetsScreen.kt app/src/main/java/com/example/financeflow/ui/screens/ReportsScreen.kt app/src/main/java/com/example/financeflow/ui/screens/RecurringRulesScreen.kt app/src/main/java/com/example/financeflow/ui/screens/CategoriesScreen.kt
git commit -m "Wire currency-aware formatting into every screen that displays money"
```

---

### Task 5: Currency pickers on input — transactions, recurring rules, category budgets

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/AddEditTransactionScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/AddEditRecurringRuleScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/CategoriesScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/recurring/RecurringRuleProcessor.kt`

**Interfaces:**
- Consumes: `Currency`, `CurrencyPreferences.flow` from Tasks 1–2.

- [ ] **Step 1: Add a currency picker to `AddEditTransactionScreen`**

Add `import com.example.financeflow.data.Currency`, `import com.example.financeflow.locale.CurrencyPreferences`, `import androidx.compose.ui.platform.LocalContext`, `import androidx.compose.runtime.collectAsState`, `import androidx.compose.runtime.getValue` (if not already present — `getValue` already is).

Add state, defaulted to the current display currency for new transactions:

```kotlin
val context = LocalContext.current
val displayCurrency by CurrencyPreferences.flow(context).collectAsState()
var currency by remember { mutableStateOf(displayCurrency) }
```

In the existing `LaunchedEffect(existing)` block that seeds `amountText`/`type`/`isIncome`/`date`/`note` from an existing transaction, add `currency = existing.currency` alongside them.

Add the picker UI after the existing Personal/Business `SingleChoiceSegmentedButtonRow` (before the category section):

```kotlin
Spacer(Modifier.height(12.dp))

SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
    Currency.entries.forEachIndexed { index, option ->
        SegmentedButton(
            selected = currency == option,
            onClick = { currency = option },
            shape = SegmentedButtonDefaults.itemShape(index, Currency.entries.size)
        ) { Text(option.name) }
    }
}
```

In `save()`, add `currency = currency,` to the `Transaction(...)` constructor call.

- [ ] **Step 2: Same picker for `AddEditRecurringRuleScreen`**

Same imports, same `context`/`displayCurrency`/`currency` state (defaulted to display currency, overridden to `existing.currency` in the `LaunchedEffect(existing)` block), same picker UI placed after the Personal/Business toggle, and `currency = currency,` added to the `RecurringRule(...)` constructor call in `save()`.

- [ ] **Step 3: Add a currency picker to the category budget field**

In `CategoriesScreen.kt`'s `AddEditCategoryDialog`, add state (defaulted to the category's existing value when editing, `Currency.MKD` for a new category):

```kotlin
var budgetCurrency by remember { mutableStateOf(editing?.budgetLimitCurrency ?: Currency.MKD) }
```

Add the picker right after the existing budget `OutlinedTextField`:

```kotlin
Spacer(Modifier.height(8.dp))

SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
    Currency.entries.forEachIndexed { index, option ->
        SegmentedButton(
            selected = budgetCurrency == option,
            onClick = { budgetCurrency = option },
            shape = SegmentedButtonDefaults.itemShape(index, Currency.entries.size)
        ) { Text(option.name) }
    }
}
```

In the `onSave` callback's `Category(...)` construction, add `budgetLimitCurrency = budgetCurrency,`.

Add `import com.example.financeflow.data.Currency` to `CategoriesScreen.kt` if not already present from Task 4's edits (it isn't — Task 4 only added `rememberCurrencyFormat(category.budgetLimitCurrency)`, which doesn't require importing `Currency` by name).

- [ ] **Step 4: Carry the rule's currency into generated transactions**

In `RecurringRuleProcessor.kt`, add `currency = rule.currency,` to the `Transaction(...)` constructor call inside `generateDueTransactions`:

```kotlin
transactionRepository.insert(
    Transaction(
        amount = rule.amount,
        currency = rule.currency,
        type = rule.type,
        categoryId = rule.categoryId,
        date = occurrence.dueDate,
        note = rule.label,
        isIncome = rule.isIncome,
        recurringId = rule.id
    )
)
```

- [ ] **Step 5: Compile and manually verify**

Run: `./gradlew compileDebugKotlin testDebugUnitTest -q`
Expected: BUILD SUCCESSFUL, all tests pass.

Run the app: add a transaction, confirm the currency picker appears and defaults to the current display currency; edit an existing transaction and confirm its picker shows the currency it was actually saved with; set a category budget in a non-default currency and confirm the Categories list shows it formatted in that currency (Task 4's per-row formatting).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/AddEditTransactionScreen.kt app/src/main/java/com/example/financeflow/ui/screens/AddEditRecurringRuleScreen.kt app/src/main/java/com/example/financeflow/ui/screens/CategoriesScreen.kt app/src/main/java/com/example/financeflow/recurring/RecurringRuleProcessor.kt
git commit -m "Add currency pickers to transaction, recurring rule, and category budget input"
```

---

### Task 6: Settings UI and launch-time rate refresh

**Files:**
- Modify: `app/src/main/java/com/example/financeflow/ui/screens/SettingsScreen.kt`
- Modify: `app/src/main/java/com/example/financeflow/MainActivity.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-en/strings.xml`

**Interfaces:**
- Consumes: `ExchangeRateRepository`, `CurrencyPreferences`, `Currency` from Tasks 1–2.

- [ ] **Step 1: Add the new strings**

In `values/strings.xml`, add near the existing `settings_*` strings:

```xml
<string name="settings_currency_title">Monedha e shfaqjes</string>
<string name="settings_currency_last_updated">Përditësuar më %1$s</string>
<string name="settings_currency_never_updated">Ende pa u përditësuar</string>
<string name="settings_currency_refresh">Rifresko kurset</string>
<string name="settings_currency_attribution">Kurset nga Exchange Rate API</string>
```

In `values-en/strings.xml`:

```xml
<string name="settings_currency_title">Display currency</string>
<string name="settings_currency_last_updated">Updated %1$s</string>
<string name="settings_currency_never_updated">Not yet updated</string>
<string name="settings_currency_refresh">Refresh rates</string>
<string name="settings_currency_attribution">Rates by Exchange Rate API</string>
```

- [ ] **Step 2: Add the display-currency section to `SettingsScreen`**

Add imports: `com.example.financeflow.data.Currency`, `com.example.financeflow.data.ExchangeRateCache`, `com.example.financeflow.data.repository.ExchangeRateRepository`, `com.example.financeflow.locale.CurrencyPreferences`, `com.example.financeflow.locale.rememberDateFormat`, `androidx.compose.ui.platform.LocalUriHandler`, `androidx.compose.runtime.collectAsState`, `androidx.compose.runtime.mutableStateOf` (already present), `java.time.Instant`, `java.time.ZoneId`.

Add state near the top of `SettingsScreen` (it already has `val context = LocalContext.current` and `val scope = rememberCoroutineScope()`):

```kotlin
val exchangeRateRepository = remember { ExchangeRateRepository(AppDatabase.getInstance(context).exchangeRateDao(), context) }
val rates by exchangeRateRepository.rates.collectAsState(initial = ExchangeRateCache())
val displayCurrency by CurrencyPreferences.flow(context).collectAsState()
var isRefreshingRates by remember { mutableStateOf(false) }
val currencyDateFormat = rememberDateFormat("MMM d, yyyy")
val uriHandler = LocalUriHandler.current
```

Add the section after the existing backup section (after the `statusMessage?.let { ... }` block, still inside the outer `Column`):

```kotlin
Spacer(Modifier.height(24.dp))

Text(text = stringResource(R.string.settings_currency_title), style = MaterialTheme.typography.titleLarge)
Spacer(Modifier.height(8.dp))

SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
    Currency.entries.forEachIndexed { index, option ->
        SegmentedButton(
            selected = displayCurrency == option,
            onClick = { CurrencyPreferences.set(context, option) },
            shape = SegmentedButtonDefaults.itemShape(index, Currency.entries.size)
        ) { Text(option.name) }
    }
}

Spacer(Modifier.height(8.dp))

val lastUpdatedText = rates.lastUpdatedEpochMillis?.let { millis ->
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    stringResource(R.string.settings_currency_last_updated, date.format(currencyDateFormat))
} ?: stringResource(R.string.settings_currency_never_updated)

Text(text = lastUpdatedText, style = MaterialTheme.typography.bodySmall)

Spacer(Modifier.height(4.dp))

TextButton(
    enabled = !isRefreshingRates,
    onClick = {
        isRefreshingRates = true
        scope.launch {
            exchangeRateRepository.forceRefresh()
            isRefreshingRates = false
        }
    }
) { Text(stringResource(R.string.settings_currency_refresh)) }

Text(
    text = stringResource(R.string.settings_currency_attribution),
    style = MaterialTheme.typography.labelSmall,
    modifier = Modifier.clickable { uriHandler.openUri("https://www.exchangerate-api.com") }
)
```

`clickable` is already imported in `SettingsScreen.kt` (used by the Categories/Recurring rows).

- [ ] **Step 3: Trigger a refresh on app launch**

In `MainActivity.kt`, add the refresh call to the existing launch-time coroutine:

```kotlin
lifecycleScope.launch {
    val db = AppDatabase.getInstance(applicationContext)
    CategoryRepository(db.categoryDao()).seedDefaultsIfEmpty(defaultCategories())
    RecurringRuleProcessor(db).generateDueTransactions()
    ExchangeRateRepository(db.exchangeRateDao(), applicationContext).refreshIfStale()
}
```

Add `import com.example.financeflow.data.repository.ExchangeRateRepository`.

- [ ] **Step 4: Compile and manually verify**

Run: `./gradlew compileDebugKotlin testDebugUnitTest -q`
Expected: BUILD SUCCESSFUL, all tests pass.

Run the app with network access available: confirm Settings shows a display-currency picker, that after a moment the "last updated" text changes from "Not yet updated" to a real date (the launch-time `refreshIfStale()` succeeding), that changing the display-currency selection immediately changes Home's totals without restarting the app, and that the manual "Refresh rates" button works. Then disable network and confirm the app still works normally (using the last cached rate) with no crash.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/financeflow/ui/screens/SettingsScreen.kt app/src/main/java/com/example/financeflow/MainActivity.kt app/src/main/res/values/strings.xml app/src/main/res/values-en/strings.xml
git commit -m "Add display-currency Settings UI and launch-time rate refresh"
```

---

## Self-Review Notes

- **Spec coverage:** Rate source/attribution → Task 2/6. Data model → Task 1. Conversion math → Task 2. SQL aggregation restructuring → Task 3. Formatting fix → Task 4. UI pickers → Task 5. Settings display/refresh → Task 6. All spec sections covered.
- **Type consistency check:** `ExchangeRateRepository.convert`/`rateOf` signatures are identical everywhere they're called (Tasks 3, 4, 5). `CurrencyPreferences.flow(context): StateFlow<Currency>` is the one shared entry point for reading/observing the display currency — every screen and ViewModel factory uses this exact call, never a raw `SharedPreferences` read. `TransactionRow`'s new `displayAmount`/`displayCurrency` parameters are threaded consistently through both of its call sites (Home, Transactions) in Task 4.
- **Ordering rationale:** Task 1 (schema) has no dependents that don't need it first. Task 2 (rate infrastructure) is additive and doesn't touch any existing file's behavior, so it's safe before the riskier Task 3 rework. Task 3 (ViewModel aggregation) intentionally lands *before* Task 4 (screen wiring) so that by the time screens change their `rememberCurrencyFormat()` calls, the numbers flowing into them are already correct — avoids a window where a screen shows a correctly-symbolled but wrongly-converted figure. Task 5 (input pickers) comes after Task 4 so a newly-entered non-default-currency transaction immediately displays correctly. Task 6 (Settings) is last since it's the one place a user can change the display currency, and everything it needs (the picker's live reactivity via Task 3/4's `displayCurrency` wiring, the refresh button's `ExchangeRateRepository`) already exists by then.

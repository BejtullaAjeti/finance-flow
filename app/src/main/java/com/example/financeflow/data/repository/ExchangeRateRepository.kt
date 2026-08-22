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

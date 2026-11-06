package com.stillpoint.app.data

import android.content.Context
import android.content.SharedPreferences
import com.stillpoint.app.core.Codec
import com.stillpoint.app.core.Config
import com.stillpoint.app.core.ResetClock
import com.stillpoint.app.core.UsageLedger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDateTime

/** Shared state for the screens and the service, which run in the same process. */
object Store {
    private const val KEY_CONFIG = "config"
    private const val KEY_LEDGER = "ledger"

    private lateinit var prefs: SharedPreferences
    private val configFlow = MutableStateFlow(Config())
    private val ledgerFlow = MutableStateFlow(UsageLedger(today()))

    val config: StateFlow<Config> = configFlow.asStateFlow()
    val ledger: StateFlow<UsageLedger> = ledgerFlow.asStateFlow()

    @Synchronized
    fun init(context: Context) {
        if (::prefs.isInitialized) return
        prefs = context.applicationContext.getSharedPreferences("stillpoint", Context.MODE_PRIVATE)
        prefs.getString(KEY_CONFIG, null)?.let { configFlow.value = Codec.decodeConfig(it) }
        prefs.getString(KEY_LEDGER, null)?.let { ledgerFlow.value = Codec.decodeLedger(it) }
    }

    fun today(): String = ResetClock.dayKey(LocalDateTime.now(), configFlow.value.resetMinute)

    fun setResetMinute(minute: Int) {
        val oldToday = today()
        updateConfig { it.copy(resetMinute = minute) }
        ledgerFlow.update { it.relabelled(oldToday, today()) }
        saveUsage()
    }

    fun usageToday(): Map<String, Long> = ledgerFlow.value.forDay(today()).usageMs

    fun updateConfig(transform: (Config) -> Config) {
        configFlow.update(transform)
        prefs.edit().putString(KEY_CONFIG, Codec.encodeConfig(configFlow.value)).apply()
    }

    /** Held in memory; the service calls [saveUsage] regularly so a restart loses seconds at most. */
    fun addUsage(pkg: String, ms: Long) {
        ledgerFlow.update { it.add(pkg, ms, today()) }
    }

    fun saveUsage() {
        prefs.edit().putString(KEY_LEDGER, Codec.encodeLedger(ledgerFlow.value)).apply()
    }
}

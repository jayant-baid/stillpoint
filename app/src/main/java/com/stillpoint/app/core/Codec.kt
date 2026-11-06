package com.stillpoint.app.core

import org.json.JSONArray
import org.json.JSONObject

object Codec {
    fun encodeConfig(config: Config): String = JSONObject().apply {
        put("appLimits", JSONObject().apply { config.appLimits.forEach { (k, v) -> put(k, v) } })
        put("groups", JSONArray().apply {
            config.groups.forEach { group ->
                put(JSONObject().apply {
                    put("id", group.id)
                    put("name", group.name)
                    put("minutes", group.minutes)
                    put("packages", JSONArray(group.packages.toList()))
                })
            }
        })
        put("grayscaleApps", JSONArray(config.grayscaleApps.toList()))
        put("grayscaleEnabled", config.grayscaleEnabled)
        put("lockExhausted", config.lockExhausted)
        put("resetMinute", config.resetMinute)
    }.toString()

    fun decodeConfig(text: String): Config {
        val json = JSONObject(text)
        val limits = json.getJSONObject("appLimits")
        val groups = json.getJSONArray("groups")
        return Config(
            appLimits = limits.keys().asSequence().associateWith { limits.getInt(it) },
            groups = List(groups.length()) { i ->
                val group = groups.getJSONObject(i)
                LimitGroup(
                    id = group.getString("id"),
                    name = group.getString("name"),
                    minutes = group.getInt("minutes"),
                    packages = group.getJSONArray("packages").toStringSet(),
                )
            },
            grayscaleApps = json.getJSONArray("grayscaleApps").toStringSet(),
            grayscaleEnabled = json.getBoolean("grayscaleEnabled"),
            // Absent in settings saved by version 0.1.
            lockExhausted = json.optBoolean("lockExhausted", false),
            resetMinute = json.optInt("resetMinute", 0),
        )
    }

    fun encodeLedger(ledger: UsageLedger): String = JSONObject().apply {
        put("day", ledger.day)
        put("usageMs", JSONObject().apply { ledger.usageMs.forEach { (k, v) -> put(k, v) } })
    }.toString()

    fun decodeLedger(text: String): UsageLedger {
        val json = JSONObject(text)
        val usage = json.getJSONObject("usageMs")
        return UsageLedger(
            day = json.getString("day"),
            usageMs = usage.keys().asSequence().associateWith { usage.getLong(it) },
        )
    }

    private fun JSONArray.toStringSet(): Set<String> = List(length()) { getString(it) }.toSet()
}

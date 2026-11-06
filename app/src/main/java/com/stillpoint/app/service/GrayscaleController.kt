package com.stillpoint.app.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log

/**
 * Turns the system's colour correction filter to monochrome. If the person already uses colour
 * correction, their setting is saved first and put back when the grayscale app closes.
 */
class GrayscaleController(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = appContext.contentResolver
    private val prefs = appContext.getSharedPreferences("grayscale", Context.MODE_PRIVATE)

    // Survives a restart of the process, so the saved setting is never overwritten by our own gray.
    private var active = prefs.getBoolean(KEY_ACTIVE, false)

    fun set(gray: Boolean) {
        if (gray == active || !isPermitted(appContext)) return
        try {
            if (gray) turnOn() else restore()
        } catch (e: SecurityException) {
            Log.w(TAG, "Could not change the display filter", e)
        }
    }

    private fun turnOn() {
        prefs.edit()
            .putInt(KEY_SAVED_ENABLED, Settings.Secure.getInt(resolver, ENABLED, 0))
            .putInt(KEY_SAVED_MODE, Settings.Secure.getInt(resolver, MODE, MODE_UNSET))
            .putBoolean(KEY_ACTIVE, true)
            .apply()
        active = true
        Settings.Secure.putInt(resolver, MODE, MODE_MONOCHROME)
        Settings.Secure.putInt(resolver, ENABLED, 1)
    }

    private fun restore() {
        Settings.Secure.putInt(resolver, ENABLED, prefs.getInt(KEY_SAVED_ENABLED, 0))
        val mode = prefs.getInt(KEY_SAVED_MODE, MODE_UNSET)
        if (mode != MODE_UNSET) Settings.Secure.putInt(resolver, MODE, mode)
        prefs.edit().putBoolean(KEY_ACTIVE, false).apply()
        active = false
    }

    companion object {
        private const val TAG = "GrayscaleController"
        private const val ENABLED = "accessibility_display_daltonizer_enabled"
        private const val MODE = "accessibility_display_daltonizer"
        private const val MODE_MONOCHROME = 0
        private const val MODE_UNSET = -2
        private const val KEY_ACTIVE = "active"
        private const val KEY_SAVED_ENABLED = "saved_enabled"
        private const val KEY_SAVED_MODE = "saved_mode"

        fun isPermitted(context: Context): Boolean =
            context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) ==
                PackageManager.PERMISSION_GRANTED
    }
}

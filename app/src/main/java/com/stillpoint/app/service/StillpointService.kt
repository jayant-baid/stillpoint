package com.stillpoint.app.service

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.stillpoint.app.core.LimitEngine
import com.stillpoint.app.core.formatResetTime
import com.stillpoint.app.data.Apps
import com.stillpoint.app.data.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Tracks the foreground app, counts its time, and applies the block and grayscale rules to it. */
class StillpointService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var grayscale: GrayscaleController
    private var exempt: Set<String> = emptySet()
    private var receiverRegistered = false

    private var lastAccrual = 0L
    private var lastSave = 0L

    private val tick = object : Runnable {
        override fun run() {
            accrue()
            apply()
            val now = SystemClock.elapsedRealtime()
            if (now - lastSave >= SAVE_INTERVAL_MS) {
                Store.saveUsage()
                lastSave = now
            }
            handler.postDelayed(this, TICK_MS)
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                accrue()
                Store.saveUsage()
                handler.removeCallbacks(tick)
                apply()
            } else {
                exempt = Apps.exemptPackages(this@StillpointService)
                lastAccrual = SystemClock.elapsedRealtime()
                startTicking()
                apply()
            }
        }
    }

    override fun onServiceConnected() {
        Store.init(this)
        grayscale = GrayscaleController(this)
        exempt = Apps.exemptPackages(this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true

        scope.launch { Store.config.collect { apply() } }
        lastAccrual = SystemClock.elapsedRealtime()
        lastSave = lastAccrual
        startTicking()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        val cls = event.className?.toString() ?: return
        // Keyboards, toasts, popups, the notification shade and our own overlay also send this
        // event. Only a real activity means the person has moved to a different app.
        // Our stop screen isn't a move to another app: the blocked app is still the current one.
        if (cls == BlockActivity::class.java.name) return
        if (pkg == SYSTEM_UI || pkg == foreground || !isActivity(pkg, cls)) return
        accrue()
        foreground = pkg
        apply()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        scope.cancel()
        if (receiverRegistered) {
            unregisterReceiver(screenReceiver)
            accrue()
            Store.saveUsage()
            BlockGate.current.value = null
            grayscale.set(false)
        }
        super.onDestroy()
    }

    private fun startTicking() {
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, TICK_MS)
    }

    private fun isUnlockedAndOn(): Boolean =
        getSystemService(PowerManager::class.java).isInteractive &&
            !getSystemService(KeyguardManager::class.java).isKeyguardLocked

    private fun isActivity(pkg: String, cls: String): Boolean = try {
        packageManager.getActivityInfo(ComponentName(pkg, cls), 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    /** Adds the time since the last call to the foreground app. Blocked time doesn't count. */
    private fun accrue() {
        val now = SystemClock.elapsedRealtime()
        val elapsed = now - lastAccrual
        lastAccrual = now
        val pkg = foreground ?: return
        if (pkg in exempt || BlockGate.current.value != null || !isUnlockedAndOn()) return
        Store.addUsage(pkg, elapsed.coerceAtMost(MAX_ACCRUAL_MS))
    }

    private fun apply() {
        if (!isUnlockedAndOn()) {
            BlockGate.current.value = null
            grayscale.set(false)
            return
        }
        val pkg = foreground
        val decision = LimitEngine.decide(pkg, Store.config.value, Store.usageToday(), exempt)
        grayscale.set(decision.grayscale)
        val block = decision.block
        if (block != null && pkg != null) {
            BlockGate.current.value = BlockGate.Info(
                labelOf(pkg),
                block,
                formatResetTime(Store.config.value.resetMinute),
            )
            if (!BlockGate.screenAlive) {
                BlockGate.screenAlive = true
                startActivity(
                    Intent(this, BlockActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
                )
            }
        } else {
            BlockGate.current.value = null
        }
    }

    private fun labelOf(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        pkg
    }

    companion object {
        // Kept outside the instance: Android can unbind and rebind an accessibility service while
        // an app stays open, and no new event arrives to say which app that is.
        private var foreground: String? = null

        private const val SYSTEM_UI = "com.android.systemui"
        private const val TICK_MS = 1_000L
        private const val SAVE_INTERVAL_MS = 15_000L
        // A tick can run late, but a long gap means the device was asleep rather than in use.
        private const val MAX_ACCRUAL_MS = 5_000L

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ) ?: return false
            val self = ComponentName(context, StillpointService::class.java)
            return enabled.split(':').any { ComponentName.unflattenFromString(it) == self }
        }
    }
}

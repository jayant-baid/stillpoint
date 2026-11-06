package com.stillpoint.app.service

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.stillpoint.app.MainActivity
import com.stillpoint.app.R
import com.stillpoint.app.core.LimitEngine
import com.stillpoint.app.core.formatResetTime
import com.stillpoint.app.data.Apps
import com.stillpoint.app.data.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Finds the foreground app from Usage Access, counts its time, and applies the block and grayscale
 * rules to it. It deliberately doesn't use an accessibility service: banking and UPI apps treat that
 * permission as a fraud risk and refuse to run alongside it.
 */
class StillpointService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var grayscale: GrayscaleController
    private var exempt: Set<String> = emptySet()

    private var foreground: String? = null
    private var lastEventQuery = 0L
    private var lastAccrual = 0L
    private var lastSave = 0L
    private var lastScreenRequest = 0L

    private val tick = object : Runnable {
        override fun run() {
            detectForeground()
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
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Store.init(this)
        grayscale = GrayscaleController(this)
        exempt = Apps.exemptPackages(this)
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        scope.launch { Store.config.collect { apply() } }
        lastEventQuery = System.currentTimeMillis() - STARTUP_LOOKBACK_MS
        lastAccrual = SystemClock.elapsedRealtime()
        lastSave = lastAccrual
        startTicking()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacks(tick)
        scope.cancel()
        unregisterReceiver(screenReceiver)
        accrue()
        Store.saveUsage()
        BlockGate.current.value = null
        grayscale.set(false)
        super.onDestroy()
    }

    private fun startTicking() {
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_MIN,
            ),
        )
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.notification_title))
            .setContentIntent(open)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    private fun isUnlockedAndOn(): Boolean =
        getSystemService(PowerManager::class.java).isInteractive &&
            !getSystemService(KeyguardManager::class.java).isKeyguardLocked

    /** Reads usage events since the last check. The newest app to come to the front is current. */
    @Suppress("DEPRECATION") // MOVE_TO_FOREGROUND is the same event as ACTIVITY_RESUMED, on all versions.
    private fun detectForeground() {
        if (!hasUsageAccess(this)) {
            foreground = null
            return
        }
        val now = System.currentTimeMillis()
        val events = getSystemService(UsageStatsManager::class.java)
            .queryEvents(lastEventQuery - QUERY_OVERLAP_MS, now)
        lastEventQuery = now

        val event = UsageEvents.Event()
        var newest: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType != UsageEvents.Event.MOVE_TO_FOREGROUND) continue
            // Our stop screen isn't a move to another app: the blocked app is still the current one.
            if (event.className == BlockActivity::class.java.name) continue
            if (event.packageName == SYSTEM_UI) continue
            newest = event.packageName
        }
        if (newest != null && newest != foreground) {
            accrue()
            foreground = newest
        }
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
        if (block != null && pkg != null && Settings.canDrawOverlays(this)) {
            BlockGate.current.value = BlockGate.Info(
                labelOf(pkg),
                block,
                formatResetTime(Store.config.value.resetMinute),
            )
            showStopScreen()
        } else {
            BlockGate.current.value = null
        }
    }

    /** Retries if a phone's background rules quietly dropped the last request. */
    private fun showStopScreen() {
        val now = SystemClock.elapsedRealtime()
        if (BlockGate.screenAlive || now - lastScreenRequest < SCREEN_RETRY_MS) return
        lastScreenRequest = now
        startActivity(
            Intent(this, BlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION),
        )
    }

    private fun labelOf(pkg: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        pkg
    }

    companion object {
        private const val TAG = "StillpointService"
        private const val SYSTEM_UI = "com.android.systemui"
        private const val CHANNEL_ID = "running"
        private const val NOTIFICATION_ID = 1
        private const val TICK_MS = 1_000L
        private const val SAVE_INTERVAL_MS = 15_000L
        private const val STARTUP_LOOKBACK_MS = 60 * 60 * 1_000L
        private const val QUERY_OVERLAP_MS = 2_000L
        private const val SCREEN_RETRY_MS = 2_000L
        // A tick can run late, but a long gap means the device was asleep rather than in use.
        private const val MAX_ACCRUAL_MS = 5_000L

        fun start(context: Context) {
            if (!hasUsageAccess(context)) return
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, StillpointService::class.java),
                )
            } catch (e: IllegalStateException) {
                // Android can refuse from the background. It starts next time the app opens.
                Log.w(TAG, "Could not start the service", e)
            }
        }
    }
}

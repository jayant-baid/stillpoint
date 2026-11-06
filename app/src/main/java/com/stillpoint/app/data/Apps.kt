package com.stillpoint.app.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

data class InstalledApp(val pkg: String, val label: String, val icon: ImageBitmap)

object Apps {
    private const val ICON_PX = 96

    fun homePackages(pm: PackageManager): Set<String> =
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .map { it.activityInfo.packageName }
            .toSet()

    /** Apps that must keep working no matter what: this app, the home screen and the system UI. */
    fun exemptPackages(context: Context): Set<String> =
        homePackages(context.packageManager) + context.packageName + "com.android.systemui"

    /** Slow enough that it must not run on the main thread. */
    fun loadLaunchable(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val exempt = exemptPackages(context)
        return pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .distinctBy { it.activityInfo.packageName }
            .filter { it.activityInfo.packageName !in exempt }
            .map {
                InstalledApp(
                    pkg = it.activityInfo.packageName,
                    label = it.loadLabel(pm).toString(),
                    icon = it.loadIcon(pm).toBitmap(ICON_PX, ICON_PX).asImageBitmap(),
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}

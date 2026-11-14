package com.ondy.app.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.WorkerThread
import com.ondy.app.domain.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide cache of installed applications and their icons.
 *
 * Scanning the [PackageManager] is expensive, so it happens at most once per
 * [CACHE_TTL_MS] on a background dispatcher. All public accessors are cheap,
 * non-blocking snapshot reads and are safe to call from any thread (including
 * the notification listener binder thread).
 *
 * The cache automatically invalidates when packages are installed, removed,
 * or updated by listening to system broadcasts.
 */
@Singleton
class AppCache @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_PACKAGE_REMOVED -> {
                    // EXTRA_REPLACING = true means the app is being updated, not uninstalled
                    if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                        invalidate()
                    }
                }
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REPLACED -> invalidate()
            }
        }
    }

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_NOT_EXPORTED
        } else {
            Context.RECEIVER_EXPORTED
        }
        context.registerReceiver(packageReceiver, filter, flags)
    }

    data class Snapshot(
        val apps: List<AppInfo>,
        val names: Map<String, String>,
        val icons: Map<String, Int>,
        val loadedAtMs: Long
    )

    private val loadMutex = Mutex()

    @Volatile
    private var snapshot: Snapshot? = null

    /**
     * Loads the application list if it is missing or older than [CACHE_TTL_MS].
     * Safe to call concurrently; only one scan will run at a time.
     */
    suspend fun ensureLoaded(force: Boolean = false) {
        if (!force && isFresh(snapshot)) return
        loadMutex.withLock {
            if (!force && isFresh(snapshot)) return
            snapshot = withContext(Dispatchers.IO) { loadSnapshot() }
        }
    }

    /** Snapshot of every installed application, ordered by label. */
    fun getAllApps(): List<AppInfo> = snapshot?.apps.orEmpty()

    /** Snapshot of launchable user-facing applications. */
    fun getVisibleApps(): List<AppInfo> = getAllApps().filter { it.isUserApp }

    fun getIconResId(packageName: String): Int? = snapshot?.icons?.get(packageName)

    /** Coil model for an app icon, or null when the app has no icon resource. */
    fun getIconModel(packageName: String): String? {
        val resId = snapshot?.icons?.get(packageName) ?: return null
        return "android.resource://$packageName/$resId"
    }

    /**
     * Best-effort application label. Falls back to a single cheap package-manager
     * lookup when the cache has not been warmed yet, so notification interception
     * never has to wait for a full package scan.
     */
    fun getAppName(packageName: String): String {
        snapshot?.names?.get(packageName)?.let { return it }
        return runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
    }

    fun invalidate() {
        snapshot = null
    }

    private fun isFresh(value: Snapshot?): Boolean {
        return value != null && System.currentTimeMillis() - value.loadedAtMs <= CACHE_TTL_MS
    }

    @WorkerThread
    private fun loadSnapshot(): Snapshot {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(0)
            .mapNotNull { appInfo ->
                runCatching {
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isUpdatedSystem =
                        (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    AppInfo(
                        packageName = appInfo.packageName,
                        appName = label,
                        isUserApp = !isSystem || isUpdatedSystem,
                        iconResId = appInfo.icon
                    )
                }.getOrNull()
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.appName })

        return Snapshot(
            apps = apps,
            names = apps.associate { it.packageName to it.appName },
            icons = apps.mapNotNull { app ->
                app.iconResId.takeIf { it != 0 }?.let { app.packageName to it }
            }.toMap(),
            loadedAtMs = System.currentTimeMillis()
        )
    }

    companion object {
        private const val CACHE_TTL_MS = 10 * 60 * 1000L
    }
}

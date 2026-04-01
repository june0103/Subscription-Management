package com.management.subscription.services

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
    val searchKey: HangulSearchKey
)

class InstalledAppRepository private constructor(
    private val appContext: Context
) {

    @Volatile
    private var cachedLauncherApps: List<InstalledAppInfo>? = null

    suspend fun getLauncherApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        cachedLauncherApps ?: loadLauncherApps().also { cachedLauncherApps = it }
    }

    fun getCachedLauncherApps(): List<InstalledAppInfo> {
        return cachedLauncherApps.orEmpty()
    }

    fun getApplicationIcon(packageName: String): Drawable? {
        return runCatching {
            appContext.packageManager.getApplicationIcon(packageName)
        }.getOrNull()
    }

    fun isPackageInstalled(packageName: String): Boolean {
        return runCatching {
            appContext.packageManager.getApplicationInfo(packageName, 0)
        }.isSuccess
    }

    private fun loadLauncherApps(): List<InstalledAppInfo> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return appContext.packageManager
            .queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == appContext.packageName) return@mapNotNull null
                val label = resolveInfo.loadLabel(appContext.packageManager)?.toString()?.trim()
                    .orEmpty()
                if (label.isBlank()) return@mapNotNull null

                InstalledAppInfo(
                    packageName = packageName,
                    label = label,
                    searchKey = HangulSearchKey.from(label)
                )
            }
            .distinctBy(InstalledAppInfo::packageName)
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    companion object {
        @Volatile
        private var instance: InstalledAppRepository? = null

        fun getInstance(context: Context): InstalledAppRepository {
            return instance ?: synchronized(this) {
                instance ?: InstalledAppRepository(
                    context.applicationContext
                ).also { instance = it }
            }
        }
    }
}

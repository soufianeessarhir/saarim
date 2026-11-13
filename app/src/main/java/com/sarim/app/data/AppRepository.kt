package com.sarim.app.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import com.sarim.app.SarimApplication
import com.sarim.app.models.AppItem

object AppRepository {
    private val context = SarimApplication.getInstance()
    private val packageManager = context.packageManager
    private val dpm get() = DeviceAdmin.policyManager
    private val adminName get() = DeviceAdmin.component

    @Volatile
    private var cachedApps: List<AppItem>? = null
    private val cacheLock = Any()

    fun getApps(forceRefresh: Boolean = false): List<AppItem> {
        if (!forceRefresh) {
            cachedApps?.let { return it }
        }
        synchronized(cacheLock) {
            if (!forceRefresh) {
                cachedApps?.let { return it }
            }
            val fresh = loadAppsFromSystem()
            cachedApps = fresh
            return fresh
        }
    }

    private fun loadAppsFromSystem(): List<AppItem> {
        val installedApps = packageManager.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES)

        return installedApps.asSequence()
            .filter { it.packageName != context.packageName }
            .map { appInfo ->
                val packageName = appInfo.packageName
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                val isSuspended = try {
                    dpm.isPackageSuspended(adminName, packageName)
                } catch (_: Exception) {
                    false
                }

                AppItem(
                    name = appInfo.loadLabel(packageManager).toString(),
                    packageName = packageName,
                    icon = appInfo.loadIcon(packageManager),
                    isSystem = isSystem,
                    isSuspended = isSuspended
                )
            }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    fun invalidateCache() {
        synchronized(cacheLock) {
            cachedApps = null
        }
    }

    private fun updateCachedApp(packageName: String, transform: (AppItem) -> AppItem) {
        synchronized(cacheLock) {
            cachedApps = cachedApps?.map { if (it.packageName == packageName) transform(it) else it }
        }
    }

    fun setAppSuspended(packageName: String, suspended: Boolean) {
        dpm.setPackagesSuspended(adminName, arrayOf(packageName), suspended)
        updateCachedApp(packageName) { it.copy(isSuspended = suspended) }
    }

    private fun getApplicationRestrictions(packageName: String): Bundle {
        return dpm.getApplicationRestrictions(adminName, packageName)
    }

    fun setApplicationRestrictions(packageName: String, restrictions: Bundle) {
        val current = dpm.getApplicationRestrictions(adminName, packageName)
        current.putAll(restrictions)
        dpm.setApplicationRestrictions(adminName, packageName, current)
    }
}

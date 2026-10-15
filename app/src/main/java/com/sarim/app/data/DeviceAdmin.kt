package com.sarim.app.data

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import androidx.core.content.edit
import com.sarim.app.SarimApplication
import com.sarim.app.SarimDeviceAdminReceiver

/** Shared admin component + [DevicePolicyManager] handle, used by every class that talks to device policy. */
object DeviceAdmin {
    val component: ComponentName = ComponentName(
        SarimApplication.getInstance(), SarimDeviceAdminReceiver::class.java
    )

    val policyManager: DevicePolicyManager
        get() = SarimApplication.getInstance()
            .getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

    /**
     * Records the outcome of the device-admin / device-owner flow in the prefs the setup UI polls,
     * so [DeviceOwnerRepository.waitForAdminResult] can report the authoritative state instead of the
     * Shizuku process exit code.
     */
    fun persistResult(context: Context, packageName: String?) {
        val enabled = packageName == context.packageName && policyManager.isAdminActive(component)
        context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("admin_result_received", true)
            putBoolean("admin_enabled", enabled)
            putString("admin_result_package", packageName)
            putLong("admin_result_at", System.currentTimeMillis())
            putString("admin_result_component", component.flattenToString())
        }
    }
}

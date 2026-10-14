package com.sarim.app.data

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
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
}

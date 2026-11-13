package com.sarim.app.data

import android.os.UserManager
import com.sarim.app.security.MandatoryProtection

/**
 * The system restrictions Sarim manages. Only the ones that protect the content block are here:
 * the mandatory bypass routes (VPN, private DNS, sideloading, uninstall, app control, safe boot,
 * USB debugging, clock changes), plus the factory-reset block offered as extra hardening.
 *
 * Everything else Android exposes through `UserManager` — volume, Bluetooth, calls, SMS, camera
 * toggles and the rest — has nothing to do with blocking pornography, so it is deliberately not
 * offered.
 */
object SystemRestrictions {
    /**
     * Applied by [MandatoryProtection] and never listed as editable: each one either routes
     * around the content filters (VPN, private DNS), installs around them, removes the blocker,
     * or lets the user boot/debug/rewind their way past it.
     */
    val mandatoryKeys: List<String> = MandatoryProtection.RESTRICTIONS

    val recommendedKeys = listOf(UserManager.DISALLOW_FACTORY_RESET)

    val allKeys: List<String> by lazy {
        (mandatoryKeys + recommendedKeys).distinct()
    }

    /** Platform field name -> `UserManager` constant, used to resolve the translated label. */
    private val restrictionFieldNames: Map<String, String> by lazy {
        UserManager::class.java.fields
            .filter { field ->
                java.lang.reflect.Modifier.isStatic(field.modifiers) &&
                        java.lang.reflect.Modifier.isFinal(field.modifiers) &&
                        (field.name.startsWith("DISALLOW_") || field.name.startsWith("ALLOW_"))
            }
            .associate { (it.get(null) as String) to it.name.lowercase() }
    }

    fun resourceNameFor(key: String): String? = restrictionFieldNames[key]
}

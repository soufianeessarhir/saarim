package com.sarim.app

import android.app.admin.ActivitySettingPolicy
import android.app.admin.DeviceAdminInfo
import android.app.admin.DevicePolicyManager
import android.app.admin.DevicePolicyIdentifiers
import android.app.admin.PolicyUpdateReceiver
import android.app.admin.PolicyUpdateResult
import android.app.admin.TargetUser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.edit
import com.sarim.app.data.AppRepository
import com.sarim.app.data.DeviceAdmin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch

// Delivered on API 34+ (UPSIDE_DOWN_CAKE) for policy-change results, and on all
// API levels for the explicit intent the device admin activity broadcasts when the
// device-owner flow finishes (DEVICE_ADMIN_ENABLED / DEVICE_POLICY_SET_RESULT).
// Both feed the same local state machine below, so the setup UI can wait on an
// authoritative signal instead of guessing from the Shizuku process exit code.
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
class SarimPolicyUpdateReceiver : PolicyUpdateReceiver(), BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DEVICE_ADMIN_ENABLED,
            ACTION_DEVICE_POLICY_SET_RESULT -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME)
                Log.d(
                    "SarimPolicyUpdate",
                    "broadcast ${intent.action}: package=$packageName " +
                        "admin=${intent.getStringExtra(EXTRA_COMPONENT_NAME) ?: "?"}"
                )
                persistAdminResult(context, packageName)
            }
        }
    }

    override fun onPolicySetResult(
        context: Context,
        policyIdentifier: String,
        additionalPolicyParams: Bundle,
        targetUser: TargetUser,
        policyUpdateResult: PolicyUpdateResult
    ) {
        log("onPolicySetResult", policyIdentifier, additionalPolicyParams, targetUser, policyUpdateResult)
    }

    override fun onPolicyChanged(
        context: Context,
        policyIdentifier: String,
        additionalPolicyParams: Bundle,
        targetUser: TargetUser,
        policyUpdateResult: PolicyUpdateResult
    ) {
        log("onPolicyChanged", policyIdentifier, additionalPolicyParams, targetUser, policyUpdateResult)
    }

    private fun log(
        callback: String,
        policyIdentifier: String,
        additionalPolicyParams: Bundle,
        targetUser: TargetUser,
        policyUpdateResult: PolicyUpdateResult
    ) {
        val packageName = additionalPolicyParams.getString(EXTRA_PACKAGE_NAME)
        Log.d(
            "SarimPolicyUpdate",
            "$callback: policy=$policyIdentifier package=$packageName " +
                "user=${describeTargetUser(targetUser)} result=${describeResult(policyUpdateResult)}"
        )

        // The final/authoritative result can disagree with what setApplicationHidden()
        // returned synchronously (e.g. another admin overriding it later). Drop the
        // cache so the next read reflects the real PackageManager state instead of
        // whatever AppRepository optimistically assumed.
        if (policyIdentifier == DevicePolicyIdentifiers.APPLICATION_HIDDEN_POLICY) {
            AppRepository.invalidateCache()
        }

        // PolicyUpdateReceiver results are the authoritative confirmation for the
        // device-owner / managed-admin setup on API 34+, so record them the same
        // way the explicit intent does (the UI waits on this state).
        if (policyUpdateResult.resultCode == PolicyUpdateResult.RESULT_POLICY_SET) {
            val pkg = additionalPolicyParams.getString(EXTRA_PACKAGE_NAME)
            persistAdminResult(context, pkg)
        }
    }

    private fun persistAdminResult(context: Context, packageName: String?) {
        val ours = DeviceAdmin.receiverComponent.flattenToString()
        val enabled = packageName == context.packageName &&
            DeviceAdmin.policyManager.isAdminActive(DeviceAdmin.component)
        context.getSharedPreferences("security_prefs", Context.MODE_PRIVATE).edit {
            putBoolean("admin_result_received", true)
            putBoolean("admin_enabled", enabled)
            putString("admin_result_package", packageName)
            putLong("admin_result_at", System.currentTimeMillis())
            putString("admin_result_component", ours)
        }
    }

    companion object {
        private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        // Explicit intent extras that DevicePolicyManager / DeviceAdminReceiver emit
        // when the device-owner or managed-profile admin activation completes.
        private const val ACTION_DEVICE_ADMIN_ENABLED = "android.app.action.DEVICE_ADMIN_ENABLED"
        private const val ACTION_DEVICE_POLICY_SET_RESULT = "android.app.action.DEVICE_POLICY_SET_RESULT"
        private const val EXTRA_PACKAGE_NAME = "android.intent.extra.OPTION_PACKAGE_NAME"
        private const val EXTRA_COMPONENT_NAME = "android.intent.extra.EXTRA_COMPONENT_NAME"
    }

    private fun describeResult(result: PolicyUpdateResult): String = when (result.resultCode) {
        PolicyUpdateResult.RESULT_POLICY_SET -> "RESULT_POLICY_SET"
        PolicyUpdateResult.RESULT_POLICY_CLEARED -> "RESULT_POLICY_CLEARED"
        PolicyUpdateResult.RESULT_FAILURE_CONFLICTING_ADMIN_POLICY -> "RESULT_FAILURE_CONFLICTING_ADMIN_POLICY"
        PolicyUpdateResult.RESULT_FAILURE_STORAGE_LIMIT_REACHED -> "RESULT_FAILURE_STORAGE_LIMIT_REACHED"
        PolicyUpdateResult.RESULT_FAILURE_HARDWARE_LIMITATION -> "RESULT_FAILURE_HARDWARE_LIMITATION"
        else -> "RESULT_FAILURE_UNKNOWN(${result.resultCode})"
    }

    private fun describeTargetUser(targetUser: TargetUser): String = when (targetUser) {
        TargetUser.GLOBAL -> "GLOBAL"
        TargetUser.LOCAL_USER -> "LOCAL_USER"
        TargetUser.PARENT_USER -> "PARENT_USER"
        TargetUser.UNKNOWN_USER -> "UNKNOWN_USER"
        else -> targetUser.toString()
    }

}

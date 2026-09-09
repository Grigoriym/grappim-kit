package com.grappim.kit.appupdate

import android.app.Activity
import kotlinx.coroutines.flow.Flow

interface AppUpdateChecker {
    val updateState: Flow<UpdateState>

    fun checkAndRequestUpdate(activity: Activity)
    fun checkUpdateStateOnResume()
    fun registerUpdateListener()
    fun unregisterUpdateListener()
    fun completeUpdate()
}

sealed class UpdateState {
    data object UpdateDownloaded : UpdateState()
}

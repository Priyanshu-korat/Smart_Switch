package com.smarthome.core.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = coroutineScope {
        Timber.d("SyncWorker: Starting background sync")

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkerEntryPoint::class.java
        )

        val deviceResult = entryPoint.syncManager().syncDevices()
        val notifResult = entryPoint.notificationSyncManager().syncNotifications()

        val isDeviceSuccess = deviceResult is com.smarthome.core.error.DomainResult.Success
        val isNotifSuccess = notifResult is com.smarthome.core.error.DomainResult.Success

        if (isDeviceSuccess && isNotifSuccess) {
            Timber.d("SyncWorker: Background sync successful")
            Result.success()
        } else {
            Timber.w("SyncWorker: Background sync failed")
            Result.retry()
        }
    }
}

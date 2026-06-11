package com.smarthome.app.ui.notifications

import androidx.lifecycle.viewModelScope
import com.smarthome.core.common.BaseViewModel
import com.smarthome.core.common.UiEvent
import com.smarthome.core.database.dao.NotificationDao
import com.smarthome.core.network.NotificationSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationDao: NotificationDao,
    private val syncManager: NotificationSyncManager
) : BaseViewModel<NotificationsUiState>(NotificationsUiState()) {

    init {
        observeNotifications()
        syncFromServer()
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            notificationDao.observeAllNotifications().collectLatest { notifications ->
                setState {
                    it.copy(
                        isLoading = false,
                        notifications = notifications,
                        unreadCount = notifications.count { n -> !n.isRead }
                    )
                }
            }
        }
    }

    private fun syncFromServer() {
        viewModelScope.launch {
            syncManager.syncNotifications()
        }
    }

    fun onMarkRead(notificationId: String) {
        viewModelScope.launch {
            syncManager.markRead(notificationId)
        }
    }

    fun onMarkAllRead() {
        viewModelScope.launch {
            notificationDao.markAllAsRead()
        }
    }
}

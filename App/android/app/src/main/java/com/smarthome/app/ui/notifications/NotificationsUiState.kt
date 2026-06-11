package com.smarthome.app.ui.notifications

import com.smarthome.core.database.entity.NotificationEntity

data class NotificationsUiState(
    val isLoading: Boolean = true,
    val notifications: List<NotificationEntity> = emptyList(),
    val unreadCount: Int = 0
)

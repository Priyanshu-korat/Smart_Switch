package com.smarthome.app.ui.notifications

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smarthome.core.database.entity.NotificationEntity
import com.smarthome.core.designsystem.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    state: NotificationsUiState,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Neutral900, Neutral850)))
    ) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Notifications", style = MaterialTheme.typography.titleLarge, color = Neutral100)
                        if (state.unreadCount > 0) {
                            Badge(containerColor = Accent500, contentColor = Brand900) {
                                Text("${state.unreadCount}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Neutral300)
                    }
                },
                actions = {
                    if (state.unreadCount > 0) {
                        TextButton(onClick = onMarkAllRead) {
                            Text("Mark all read", color = Accent500, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )

            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Accent500)
                }
            } else if (state.notifications.isEmpty()) {
                EmptyNotificationsState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.notifications, key = { it.id }) { notification ->
                        NotificationItem(
                            notification = notification,
                            onMarkRead = { onMarkRead(notification.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationItem(
    notification: NotificationEntity,
    onMarkRead: () -> Unit
) {
    val isUnread = !notification.isRead
    val bgColor = if (isUnread) Accent500.copy(alpha = 0.08f) else Neutral800

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isUnread, onClick = onMarkRead)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Category icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        when (notification.category) {
                            "device" -> Brand600
                            "alert" -> Danger500.copy(alpha = 0.2f)
                            else -> Neutral700
                        },
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (notification.category) {
                        "device" -> Icons.Filled.Router
                        "alert" -> Icons.Filled.Warning
                        else -> Icons.Outlined.Notifications
                    },
                    contentDescription = null,
                    tint = when (notification.category) {
                        "device" -> Accent500
                        "alert" -> Danger500
                        else -> Neutral400
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isUnread) Neutral100 else Neutral300,
                        fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal
                    )
                    Text(
                        formatTimestamp(notification.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Neutral500
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    notification.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Neutral400,
                    maxLines = 2
                )
            }

            // Unread dot
            if (isUnread) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(Accent500, CircleShape)
                        .align(Alignment.CenterVertically)
                )
            }
        }
    }
}

@Composable
private fun EmptyNotificationsState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        LottieEmptyState(
            title = "No Notifications",
            message = "Physical switch presses and alerts will appear here."
        )
    }
}

private fun formatTimestamp(ts: Long): String {
    val diff = System.currentTimeMillis() - ts
    return when {
        diff < 60_000 -> "Now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(ts))
    }
}

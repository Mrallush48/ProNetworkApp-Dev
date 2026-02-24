package com.pronetwork.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronetwork.app.R
import com.pronetwork.app.network.ConnectivityObserver
import com.pronetwork.app.network.SyncEngine

/**
 * A compact status bar showing connectivity and sync state.
 * Designed as an independent, reusable Composable component.
 */
@Composable
fun SyncStatusBar(
    connectivityStatus: ConnectivityObserver.Status,
    syncState: SyncEngine.SyncState,
    modifier: Modifier = Modifier
) {
    val isOnline = connectivityStatus == ConnectivityObserver.Status.AVAILABLE
    val isSyncing = syncState.status == SyncEngine.SyncStatus.PUSHING ||
            syncState.status == SyncEngine.SyncStatus.PULLING

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSyncing -> Color(0xFFFFF3E0) // Orange light
            isOnline -> Color(0xFFE8F5E9)  // Green light
            else -> Color(0xFFFFEBEE)       // Red light
        },
        label = "syncBarColor"
    )

    val dotColor = when {
        isSyncing -> Color(0xFFFF9800) // Orange
        isOnline -> Color(0xFF4CAF50)  // Green
        else -> Color(0xFFF44336)       // Red
    }

    val statusText = when {
        isSyncing -> stringResource(R.string.sync_status_syncing)
        isOnline -> stringResource(R.string.sync_status_online)
        else -> stringResource(R.string.sync_status_offline)
    }

    // Pulsing animation for syncing dot
    val infiniteTransition = rememberInfiniteTransition(label = "syncPulse")
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isSyncing) 0.3f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Status dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .alpha(dotAlpha)
                .clip(CircleShape)
                .background(dotColor)
        )

        // Status text
        Text(
            text = statusText,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = dotColor,
            modifier = Modifier.padding(start = 6.dp)
        )

        // Pending count
        if (syncState.pendingCount > 0) {
            Text(
                text = " • " + stringResource(R.string.sync_pending_count, syncState.pendingCount),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

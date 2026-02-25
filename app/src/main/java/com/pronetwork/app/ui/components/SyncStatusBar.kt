package com.pronetwork.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronetwork.app.network.SyncEngine

@Composable
fun SyncStatusBar(
    syncState: SyncEngine.SyncState,
    modifier: Modifier = Modifier
) {
    val isPushing = syncState.status == SyncEngine.SyncStatus.PUSHING
    val isPulling = syncState.status == SyncEngine.SyncStatus.PULLING
    val isSyncing = isPushing || isPulling
    val isError = syncState.status == SyncEngine.SyncStatus.ERROR
    val isSuccess = syncState.status == SyncEngine.SyncStatus.SUCCESS
    val hasPending = syncState.pendingCount > 0

    // لا تعرض شيء إذا كل شيء تمام وما في عمليات معلقة
    if (!isSyncing && !isError && !hasPending) return

    val bgColor by animateColorAsState(
        targetValue = when {
            isSyncing -> Color(0xFFFFF8E1)  // أصفر فاتح — جاري المزامنة
            isError -> Color(0xFFFFEBEE)     // أحمر فاتح — خطأ
            hasPending -> Color(0xFFFFF3E0)  // برتقالي فاتح — عمليات معلقة
            else -> Color.Transparent
        },
        label = "syncBgColor"
    )

    val dotColor = when {
        isSyncing -> Color(0xFFFF9800)  // برتقالي
        isError -> Color(0xFFF44336)     // أحمر
        hasPending -> Color(0xFFFF9800)  // برتقالي
        else -> Color(0xFF4CAF50)        // أخضر
    }

    val text = when {
        isPushing -> "⬆ Uploading changes..."
        isPulling -> "⬇ Downloading updates..."
        isError -> "⚠ Sync failed${syncState.errorMessage?.let { " — $it" } ?: ""}"
        hasPending -> "⏳ ${syncState.pendingCount} pending changes"
        else -> ""
    }

    // نبض للنقطة أثناء المزامنة
    val pulseAlpha = if (isSyncing) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        alpha
    } else {
        1f
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .alpha(pulseAlpha)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = dotColor,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

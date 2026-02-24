package com.pronetwork.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronetwork.app.network.ConnectivityObserver

@Composable
fun ConnectionBadge(
    connectivityStatus: ConnectivityObserver.Status,
    modifier: Modifier = Modifier
) {
    val isOnline = connectivityStatus == ConnectivityObserver.Status.AVAILABLE
    val dotColor = if (isOnline) Color(0xFF4CAF50) else Color(0xFFF44336)
    val bgColor = if (isOnline) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val text = if (isOnline) "Online" else "Offline"
    val textColor = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828)

    Row(
        modifier = modifier
            .padding(vertical = 4.dp),

        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

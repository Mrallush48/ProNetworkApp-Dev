package com.pronetwork.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronetwork.app.R
import com.pronetwork.app.data.DailyBuildingDetailedUi
import com.pronetwork.app.data.DailyClientCollection
import com.pronetwork.app.viewmodel.DailyCollectionUi
import com.pronetwork.app.viewmodel.DailyPerformanceLevel
import com.pronetwork.app.viewmodel.getDailyPerformance
import com.pronetwork.data.DailySummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun DailyCollectionScreen(
    dailyCollection: DailyCollectionUi?,
    dailySummary: DailySummary,
    selectedDateMillis: Long,
    onChangeDate: (Long) -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var expandedBuildingId by remember { mutableStateOf<Int?>(null) }

    // Compute status breakdown from buildings
    val allClients = dailyCollection?.buildings?.flatMap { it.clients } ?: emptyList()
    var showAllClients by remember { mutableStateOf(false) }
    val paidCount = allClients.count { it.paymentStatus == "PAID" }
    val partialCount = allClients.count { it.paymentStatus == "PARTIAL" }
    val settledCount = allClients.count { it.paymentStatus == "SETTLED" }
    val unpaidCount = allClients.count { it.paymentStatus == "UNPAID" }

    // Filter buildings: show only clients with today's transactions by default
    val displayBuildings = if (showAllClients) {
        dailyCollection?.buildings ?: emptyList()
    } else {
        dailyCollection?.buildings?.map { b ->
            b.copy(clients = b.clients.filter { it.todayPaid != 0.0 || it.transactions.isNotEmpty() })
        }?.filter { it.clients.isNotEmpty() } ?: emptyList()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ===== Navigation buttons =====
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedDateMillis
                            add(Calendar.DAY_OF_YEAR, -1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onChangeDate(cal.timeInMillis)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(R.string.daily_collection_prev_day),
                        maxLines = 1
                    )
                }
                Button(
                    onClick = {
                        val todayCal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = selectedDateMillis
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        val newTime = if (cal.timeInMillis > todayCal.timeInMillis) {
                            todayCal.timeInMillis
                        } else {
                            cal.timeInMillis
                        }
                        onChangeDate(newTime)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(R.string.daily_collection_next_day),
                        maxLines = 1
                    )
                }
            }
        }

        // ===== Date =====
        item {
            Text(
                text = stringResource(
                    R.string.daily_collection_date_label,
                    dateFormat.format(Date(selectedDateMillis))
                ),
                style = MaterialTheme.typography.titleMedium
            )
        }

        // ===== Summary Cards =====
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.daily_summary_total),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%.2f", dailySummary.totalAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.daily_summary_clients),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${dailySummary.totalClients}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.daily_summary_transactions),
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${dailySummary.totalTransactions}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // بعد الـ Row الحالية للـ 3 كروت، أضف Row جديدة:
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Settled Amount Card
                val settledAmount = allClients
                    .filter { it.paymentStatus == "SETTLED" }
                    .sumOf { it.totalPaid }
                val refundAmount = allClients
                    .flatMap { it.transactions }
                    .filter { it.type == "Refund" }
                    .sumOf { kotlin.math.abs(it.amount) }

                if (settledAmount > 0.0) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFE3F2FD)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.daily_settled_amount_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF1565C0)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format("%.2f", settledAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                        }
                    }
                }

                if (refundAmount > 0.0) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF3E0)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.daily_refund_amount_label),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format("%.2f", refundAmount),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                    }
                }
            }
        }

        // ===== NEW: Payment Status Breakdown =====
        if (allClients.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(text = stringResource(R.string.daily_payment_status),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StatusBadge(
                                label = stringResource(R.string.status_paid),
                                count = paidCount,
                                color = Color(0xFF2E7D32),
                                bgColor = Color(0xFFE8F5E9),
                                modifier = Modifier.weight(1f)
                            )
                            StatusBadge(
                                label = stringResource(R.string.status_partial),
                                count = partialCount,
                                color = Color(0xFFF57F17),
                                bgColor = Color(0xFFFFF8E1),
                                modifier = Modifier.weight(1f)
                            )
                            StatusBadge(
                                label = stringResource(R.string.status_settled),
                                count = settledCount,
                                color = Color(0xFF1565C0),
                                bgColor = Color(0xFFE3F2FD),
                                modifier = Modifier.weight(1f)
                            )
                            StatusBadge(
                                label = stringResource(R.string.status_unpaid),
                                count = unpaidCount,
                                color = Color(0xFFC62828),
                                bgColor = Color(0xFFFFEBEE),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // ===== Performance + Collection Rate =====
        item {
            val total = dailyCollection?.totalAmount ?: 0.0
            val performance = getDailyPerformance(total)
            val (emoji, color) = when (performance) {
                DailyPerformanceLevel.EXCELLENT -> "\uD83D\uDFE2" to MaterialTheme.colorScheme.primary
                DailyPerformanceLevel.GOOD -> "\uD83D\uDFE1" to Color(0xFFFFC107)
                DailyPerformanceLevel.POOR -> "\uD83D\uDD34" to MaterialTheme.colorScheme.error
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.daily_collection_total_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.daily_collection_total_value, total),
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = emoji, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                text = stringResource(
                                    when (performance) {
                                        DailyPerformanceLevel.EXCELLENT -> R.string.performance_excellent
                                        DailyPerformanceLevel.GOOD -> R.string.performance_good
                                        DailyPerformanceLevel.POOR -> R.string.performance_poor
                                    }
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = color,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    val collectionRate = dailyCollection?.overallCollectionRate ?: 0.0
                    if (collectionRate > 0) {
                        Spacer(Modifier.height(8.dp))
                        val rateColor = when {
                            collectionRate >= 80 -> Color(0xFF2E7D32)
                            collectionRate >= 50 -> Color(0xFFF57F17)
                            else -> Color(0xFFC62828)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(stringResource(R.string.daily_collection_rate_label), style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = String.format("%.1f%%", collectionRate),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = rateColor
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (collectionRate / 100f).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = rateColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                }
            }
        }

        // ===== Building Details Title =====
        // ===== Toggle: Today's Transactions / All Clients =====
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showAllClients = !showAllClients }
                ) {
                    Text(
                        text = if (showAllClients) "\uD83D\uDC65 Show Today's Transactions Only"
                               else "\uD83D\uDCCB Show All Clients",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.daily_collection_by_building_title),
                style = MaterialTheme.typography.titleMedium
            )
        }

        // ===== Building List =====
        val buildings: List<DailyBuildingDetailedUi> = displayBuildings
        if (buildings.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.daily_collection_empty_for_day))
                }
            }
        } else {
            items(buildings, key = { it.buildingId }) { building ->
                BuildingDetailCard(
                    building = building,
                    isExpanded = expandedBuildingId == building.buildingId,
                    onToggle = {
                        expandedBuildingId =
                            if (expandedBuildingId == building.buildingId) null
                            else building.buildingId
                    }
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ================== Status Badge (NEW) ==================
@Composable
private fun StatusBadge(
    label: String,
    count: Int,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// ================== Building Detail Card ==================
@Composable
private fun BuildingDetailCard(
    building: DailyBuildingDetailedUi,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val rateColor = when {
        building.collectionRate >= 80 -> Color(0xFF2E7D32)
        building.collectionRate >= 50 -> Color(0xFFF57F17)
        else -> Color(0xFFC62828)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = building.buildingName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.daily_building_clients_count, building.clientsCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.daily_building_amount_sar, String.format("%.2f", building.totalAmount)),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (building.collectionRate > 0) {
                        Text(
                            text = String.format("%.0f%%", building.collectionRate),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = rateColor
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (building.expectedAmount > 0) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (building.collectionRate / 100f).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = rateColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.daily_collected_label, String.format("%.2f", building.totalAmount)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = stringResource(R.string.daily_expected_label, String.format("%.2f", building.expectedAmount)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            val buildingPaid = building.clients.count { it.paymentStatus == "PAID" }
            val buildingPartial = building.clients.count { it.paymentStatus == "PARTIAL" }
            val buildingSettled = building.clients.count { it.paymentStatus == "SETTLED" }
            val buildingUnpaid = building.clients.count { it.paymentStatus == "UNPAID" }

            if (building.clients.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniStatusChip("✅", buildingPaid, Color(0xFF2E7D32))
                    MiniStatusChip("⚠️", buildingPartial, Color(0xFFF57F17))
                    MiniStatusChip("⚡", buildingSettled, Color(0xFF1565C0))
                    MiniStatusChip("❌", buildingUnpaid, Color(0xFFC62828))
                }
            }
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(stringResource(R.string.daily_table_client), modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.daily_table_room), modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.daily_table_paid), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.daily_table_time), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    building.clients.forEachIndexed { index, client ->
                        ClientRow(client = client, isEven = index % 2 == 0)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(stringResource(R.string.daily_table_total), modifier = Modifier.weight(2f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("${building.clientsCount}", modifier = Modifier.weight(0.7f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(String.format("%.2f", building.totalAmount), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("", modifier = Modifier.weight(1f))
                    }

                }  // ← نهاية Column الخاصة بـ AnimatedVisibility
            }  // ← نهاية AnimatedVisibility

        }
    }
}

// ================== Client Row ==================
@Composable
private fun ClientRow(client: DailyClientCollection, isEven: Boolean) {
    val bgColor = if (isEven) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val paidRatio = if (client.monthlyAmount > 0) client.totalPaid / client.monthlyAmount else 0.0
    val isSettled = client.paymentStatus == "SETTLED"
    val amountColor = when {
        isSettled -> Color(0xFF1565C0)
        paidRatio >= 1.0 -> Color(0xFF2E7D32)
        paidRatio >= 0.5 -> Color(0xFFF57F17)
        else -> Color(0xFFC62828)
    }
    val remaining = (client.monthlyAmount - client.totalPaid).coerceAtLeast(0.0)
    val hasRefundToday = client.transactions.any { it.type == "Refund" }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(2f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = client.clientName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (client.paymentStatus == "SETTLED") {
                        Text(text = "\uD83D\uDD35", fontSize = 10.sp)
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "#${client.subscriptionNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp
                    )
                    if (client.paymentStatus.isNotEmpty()) {
                        val paidLabel = stringResource(R.string.status_paid)
                        val settledLabel = stringResource(R.string.status_settled)
                        val partialLabel = stringResource(R.string.status_partial)
                        val unpaidLabel = stringResource(R.string.status_unpaid)
                        val (statusText, statusColor) = when (client.paymentStatus) {
                            "PAID" -> paidLabel to Color(0xFF2E7D32)
                            "SETTLED" -> settledLabel to Color(0xFF1565C0)
                            "PARTIAL" -> partialLabel to Color(0xFFF57F17)
                            else -> unpaidLabel to Color(0xFFC62828)
                        }
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    }
                }
            }

            Text(
                text = client.roomNumber ?: "-",
                modifier = Modifier.weight(0.7f),
                style = MaterialTheme.typography.bodySmall
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (hasRefundToday) {
                        Text(text = "\u26A0", fontSize = 9.sp)
                    }
                    Text(
                        text = String.format("%.0f", client.todayPaid),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (client.todayPaid < 0) Color(0xFFC62828) else amountColor
                    )
                }
                Text(
                    text = "${String.format("%.0f", client.totalPaid)}/${String.format("%.0f", client.monthlyAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }

            Text(
                text = client.transactionTime,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (hasRefundToday) {
            client.transactions.filter { it.type == "Refund" }.forEach { refund ->
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "\uD83D\uDD34 Refund: ${String.format("%.0f", kotlin.math.abs(refund.amount))} SAR",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp
                    )
                    if (refund.notes.isNotBlank()) {
                        Text(
                            text = "(${refund.notes})",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (client.notes.isNotBlank()) {
            Text(
                text = client.notes,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
@Composable
private fun MiniStatusChip(emoji: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = emoji, fontSize = 10.sp)
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

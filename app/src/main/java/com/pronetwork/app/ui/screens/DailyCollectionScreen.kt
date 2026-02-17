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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@Composable
fun DailyCollectionScreen(
    dailyCollection: DailyCollectionUi?,
    dailySummary: DailySummary,
    selectedDateMillis: Long,
    onChangeDate: (Long) -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var expandedBuildingId by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ===== أزرار التنقل =====
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


        // ===== التاريخ =====
        item {
            Text(
                text = stringResource(
                    R.string.daily_collection_date_label,
                    dateFormat.format(Date(selectedDateMillis))
                ),
                style = MaterialTheme.typography.titleMedium
            )
        }

        // ===== ملخص التحصيل — 3 Cards =====
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

        // ===== تقييم الأداء + نسبة التحصيل =====
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

        // ===== عنوان التفاصيل حسب المبنى =====
        item {
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.daily_collection_by_building_title),
                style = MaterialTheme.typography.titleMedium
            )
        }

        // ===== قائمة المباني =====
        val buildings: List<DailyBuildingDetailedUi> =
            dailyCollection?.buildings ?: emptyList()

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
                            if (expandedBuildingId == building.buildingId) null else building.buildingId
                    }
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ================== كارد مبنى تفصيلي (قابل للتوسيع) ==================
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
            // === رأس الكارد (قابل للضغط) ===
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

                // نسبة التحصيل + سهم التوسيع
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
                        imageVector = if (isExpanded)
                            Icons.Filled.KeyboardArrowUp
                        else
                            Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // === شريط التحصيل ===
            if (building.expectedAmount > 0) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = {
                        (building.collectionRate / 100f).toFloat().coerceIn(0f, 1f)
                    },
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

            // === تفاصيل العملاء (تظهر عند التوسيع) ===
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

                    // عنوان الجدول
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(stringResource(R.string.daily_table_client),
                            modifier = Modifier.weight(2f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(stringResource(R.string.daily_table_room),
                            modifier = Modifier.weight(0.7f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(stringResource(R.string.daily_table_paid),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(stringResource(R.string.daily_table_time),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // صفوف العملاء
                    building.clients.forEachIndexed { index, client ->
                        ClientRow(client = client, isEven = index % 2 == 0)
                    }

                    // صف الإجمالي
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(stringResource(R.string.daily_table_total),
                            modifier = Modifier.weight(2f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${building.clientsCount}",
                            modifier = Modifier.weight(0.7f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            String.format("%.2f", building.totalAmount),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ================== صف عميل واحد ==================
@Composable
private fun ClientRow(client: DailyClientCollection, isEven: Boolean) {
    val bgColor = if (isEven)
        Color.Transparent
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    val paidRatio = if (client.monthlyAmount > 0)
        client.paidAmount / client.monthlyAmount
    else 0.0

    val amountColor = when {
        paidRatio >= 1.0 -> Color(0xFF2E7D32)
        paidRatio >= 0.5 -> Color(0xFFF57F17)
        else -> Color(0xFFC62828)
    }

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
            // اسم العميل + رقم الاشتراك
            Column(modifier = Modifier.weight(2f)) {
                Text(
                    text = client.clientName,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "#${client.subscriptionNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }

            // الغرفة
            Text(
                text = client.roomNumber ?: "-",
                modifier = Modifier.weight(0.7f),
                style = MaterialTheme.typography.bodySmall
            )

            // المبلغ المدفوع / الشهري
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = String.format("%.0f", client.paidAmount),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Text(
                    text = "/${String.format("%.0f", client.monthlyAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
            }

            // وقت الدفع
            Text(
                text = client.transactionTime,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ملاحظات (إن وجدت)
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
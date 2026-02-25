package com.pronetwork.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronetwork.app.R
import com.pronetwork.app.viewmodel.PaymentViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.material.icons.filled.Logout
import com.pronetwork.app.network.SyncEngine
import com.pronetwork.app.ui.components.ConnectionBadge
import com.pronetwork.app.ui.components.SyncStatusBar



// =============== Data Classes ===============
data class RecentTransaction(
    val clientName: String,
    val amount: Double,
    val type: String, // "Payment" or "Refund"
    val time: String,
    val buildingName: String
)

data class UnpaidClientInfo(
    val clientId: Int,
    val clientName: String,
    val buildingName: String,
    val monthlyAmount: Double,
    val totalPaid: Double,
    val remaining: Double
)

// =============== Dashboard Screen ===============
@Composable
fun DashboardScreen(
    userName: String = "",
    userRole: String = "",
    onLogout: () -> Unit = {},
    currentMonthStats: PaymentViewModel.MonthStats?,
    previousMonthStats: PaymentViewModel.MonthStats?,
    totalClients: Int,
    totalBuildings: Int,
    recentTransactions: List<RecentTransaction>,
    topUnpaidClients: List<UnpaidClientInfo>,
    onNavigateToDaily: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToStats: () -> Unit,
    onClientClick: (Int) -> Unit,
    connectivityStatus: com.pronetwork.app.network.ConnectivityObserver.Status =
        com.pronetwork.app.network.ConnectivityObserver.Status.UNAVAILABLE,
    syncState: SyncEngine.SyncState = SyncEngine.SyncState()
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "SA")).apply {
        maximumFractionDigits = 0
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // ===== Welcome Card with Date =====
        item {
            WelcomeCard(
                userName = userName,
                onLogout = onLogout,
                connectivityStatus = connectivityStatus
            )
        }

        // ===== Sync Status =====
        item {
            SyncStatusBar(syncState = syncState)
        }

        // ===== KPI Cards Row =====
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Total Collection This Month
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.dashboard_total_collection),
                    value = currencyFormat.format(currentMonthStats?.totalPaidAmount ?: 0.0),
                    icon = Icons.Filled.AttachMoney,
                    color = Color(0xFF4CAF50)
                )
                // Active Clients
                KpiCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.dashboard_active_clients),
                    value = totalClients.toString(),
                    icon = Icons.Filled.People,
                    color = Color(0xFF2196F3)
                )
            }
        }

        // ===== Collection Rate Card =====
        item {
            CollectionRateCard(
                currentMonthStats = currentMonthStats,
                totalClients = totalClients
            )
        }

        // ===== Month Comparison Card =====
        item {
            MonthComparisonCard(
                currentMonthStats = currentMonthStats,
                previousMonthStats = previousMonthStats,
                currencyFormat = currencyFormat
            )
        }

        // ===== Quick Actions =====
        item {
            QuickActionsCard(
                onNavigateToDaily = onNavigateToDaily,
                onNavigateToClients = onNavigateToClients
            )
        }

        // ===== Needs Attention (Top Unpaid Clients) =====
        if (topUnpaidClients.isNotEmpty()) {
            item {
                NeedsAttentionCard(
                    topUnpaidClients = topUnpaidClients,
                    currencyFormat = currencyFormat,
                    onClientClick = onClientClick,
                    onViewAll = onNavigateToStats
                )
            }
        }

        // ===== Recent Transactions =====
        if (recentTransactions.isNotEmpty()) {
            item {
                RecentTransactionsCard(
                    recentTransactions = recentTransactions,
                    currencyFormat = currencyFormat
                )
            }
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// =============== Welcome Card ===============
@Composable
private fun WelcomeCard(
    userName: String = "",
    onLogout: () -> Unit = {},
    connectivityStatus: com.pronetwork.app.network.ConnectivityObserver.Status =
        com.pronetwork.app.network.ConnectivityObserver.Status.UNAVAILABLE
) {
    val today = Date()
    val gregorianFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
    val gregorianDate = gregorianFormat.format(today)

    // Hijri date calculation
    val islamicCalendar = android.icu.util.IslamicCalendar()
    islamicCalendar.time = today
    val hijriDay = islamicCalendar.get(android.icu.util.Calendar.DAY_OF_MONTH)
    val hijriMonth = islamicCalendar.get(android.icu.util.Calendar.MONTH)
    val hijriYear = islamicCalendar.get(android.icu.util.Calendar.YEAR)

    val hijriMonthNames = arrayOf(
        "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani",
        "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha\'ban",
        "Ramadan", "Shawwal", "Dhul-Qi\'dah", "Dhul-Hijjah"
    )
    val hijriDate = "$hijriDay ${hijriMonthNames[hijriMonth]} $hijriYear H"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.dashboard_welcome),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (userName.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.logged_in_as, userName),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    // Connection Status Badge
                    ConnectionBadge(connectivityStatus = connectivityStatus)
                }
                IconButton(onClick = onLogout) {
                    Icon(
                        Icons.Filled.Logout,
                        contentDescription = stringResource(R.string.logout),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = gregorianDate,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = hijriDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// =============== KPI Card ===============
@Composable
private fun KpiCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// =============== Collection Rate Card ===============
@Composable
private fun CollectionRateCard(
    currentMonthStats: PaymentViewModel.MonthStats?,
    totalClients: Int
) {
    val stats = currentMonthStats
    val paidCount = stats?.paidCount ?: 0
    val partialCount = stats?.partiallyPaidCount ?: 0
    val settledCount = stats?.settledCount ?: 0
    val unpaidCount = stats?.unpaidCount ?: 0
    val total = paidCount + partialCount + settledCount + unpaidCount
    val collectionRate = if (total > 0) ((paidCount + settledCount).toFloat() / total * 100) else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.dashboard_collection_rate),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Circular Progress
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { collectionRate / 100f },
                    modifier = Modifier.size(100.dp),
                    strokeWidth = 10.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = when {
                        collectionRate >= 80 -> Color(0xFF4CAF50)
                        collectionRate >= 50 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
                Text(
                    text = "${collectionRate.toInt()}%",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatusChip(
                    label = stringResource(R.string.status_paid),
                    count = paidCount,
                    color = Color(0xFF4CAF50)
                )
                StatusChip(
                    label = stringResource(R.string.status_partial),
                    count = partialCount,
                    color = Color(0xFFFF9800)
                )
                StatusChip(
                    label = stringResource(R.string.status_settled),
                    count = settledCount,
                    color = Color(0xFF2196F3)
                )
                StatusChip(
                    label = stringResource(R.string.status_unpaid),
                    count = unpaidCount,
                    color = Color(0xFFF44336)
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    count: Int,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =============== Month Comparison Card ===============
@Composable
private fun MonthComparisonCard(
    currentMonthStats: PaymentViewModel.MonthStats?,
    previousMonthStats: PaymentViewModel.MonthStats?,
    currencyFormat: NumberFormat
) {
    val currentAmount = currentMonthStats?.totalPaidAmount ?: 0.0
    val previousAmount = previousMonthStats?.totalPaidAmount ?: 0.0
    val difference = currentAmount - previousAmount
    val percentChange = if (previousAmount > 0) ((difference / previousAmount) * 100) else 0.0
    val isPositive = difference >= 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.dashboard_month_comparison),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.dashboard_this_month),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = currencyFormat.format(currentAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.dashboard_last_month),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = currencyFormat.format(previousAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isPositive) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                    contentDescription = null,
                    tint = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = "${if (isPositive) "+" else ""}${String.format(Locale.ENGLISH, "%.1f", percentChange)}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Text(
                        text = "(${if (isPositive) "+" else ""}${currencyFormat.format(difference)})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// =============== Quick Actions Card ===============
@Composable
private fun QuickActionsCard(
    onNavigateToDaily: () -> Unit,
    onNavigateToClients: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.dashboard_quick_actions),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onNavigateToDaily,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.dashboard_start_daily),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onNavigateToClients,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.dashboard_add_client),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// =============== Needs Attention Card ===============
@Composable
private fun NeedsAttentionCard(
    topUnpaidClients: List<UnpaidClientInfo>,
    currencyFormat: NumberFormat,
    onClientClick: (Int) -> Unit,
    onViewAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.dashboard_needs_attention),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE65100)
                    )
                }
                TextButton(onClick = onViewAll) {
                    Text(
                        text = stringResource(R.string.dashboard_view_all),
                        color = Color(0xFFE65100)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            topUnpaidClients.take(5).forEach { client ->
                UnpaidClientRow(
                    client = client,
                    currencyFormat = currencyFormat,
                    onClick = { onClientClick(client.clientId) }
                )
                if (client != topUnpaidClients.last()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color(0xFFFFCC80)
                    )
                }
            }
        }
    }
}

@Composable
private fun UnpaidClientRow(
    client: UnpaidClientInfo,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = client.clientName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = client.buildingName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Text(
                    text = currencyFormat.format(client.remaining),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF44336)
                )
            }
            Text(
                text = stringResource(R.string.dashboard_remaining),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// =============== Recent Transactions Card ===============
@Composable
private fun RecentTransactionsCard(
    recentTransactions: List<RecentTransaction>,
    currencyFormat: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.dashboard_recent_transactions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Filled.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            recentTransactions.take(5).forEach { transaction ->
                TransactionRow(
                    transaction = transaction,
                    currencyFormat = currencyFormat
                )
                if (transaction != recentTransactions.last()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: RecentTransaction,
    currencyFormat: NumberFormat
) {
    val isRefund = transaction.type == "Refund"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRefund) Color(0xFFFFF3E0)
                        else Color(0xFFE8F5E9)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isRefund) Icons.Filled.Undo else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (isRefund) Color(0xFFFF9800) else Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = transaction.clientName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${transaction.buildingName} - ${transaction.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Text(
                text = "${if (isRefund) "-" else "+"}${currencyFormat.format(kotlin.math.abs(transaction.amount))}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isRefund) Color(0xFFFF9800) else Color(0xFF4CAF50)
            )
        }
    }
}
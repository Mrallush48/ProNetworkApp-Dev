package com.pronetwork.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pronetwork.app.R
import com.pronetwork.app.data.Client
import com.pronetwork.app.viewmodel.PaymentViewModel
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.graphics.Color
import com.pronetwork.app.viewmodel.MonthStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    clientsCount: Int,
    buildingsCount: Int,
    monthStats: MonthStats?,
    monthOptions: List<String>,
    selectedMonth: String,
    onMonthChange: (String) -> Unit,
    allClients: List<Client> = emptyList()
) {
    var monthDropdownExpanded by remember { mutableStateOf(false) }

    val safeSelectedMonth = remember(selectedMonth, monthOptions) {
        if (monthOptions.contains(selectedMonth)) selectedMonth
        else monthOptions.firstOrNull().orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // اختيار الشهر
        ExposedDropdownMenuBox(
            expanded = monthDropdownExpanded,
            onExpandedChange = { monthDropdownExpanded = !monthDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = safeSelectedMonth,
                onValueChange = { },
                readOnly = true,
                label = { Text(stringResource(R.string.stats_month_label)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthDropdownExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = monthDropdownExpanded,
                onDismissRequest = { monthDropdownExpanded = false }
            ) {
                monthOptions.forEach { month ->
                    DropdownMenuItem(
                        text = { Text(month) },
                        onClick = {
                            monthDropdownExpanded = false
                            onMonthChange(month)
                        }
                    )
                }
            }
        }

        // Monthly Collection Ratio Card
        if (monthStats != null) {
            val totalClients = monthStats.paidCount + monthStats.partiallyPaidCount + monthStats.settledCount + monthStats.unpaidCount
            val collectedClients = monthStats.paidCount + monthStats.settledCount
            val collectionRate = if (totalClients > 0) {
                (collectedClients.toFloat() / totalClients.toFloat()) * 100f
            } else {
                0f
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.monthly_collection_ratio),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("${stringResource(R.string.expected_clients)}: $totalClients")
                            Text("${stringResource(R.string.paid_clients)}: ${monthStats.paidCount}")
                        }
                        if (monthStats.settledCount > 0) {
                            Text("${stringResource(R.string.stats_settled_clients)}: ${monthStats.settledCount}")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { collectionRate / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "%.1f%%".format(collectionRate),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // كروت الاحصائيات العامة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.stats_total_clients),
                    value = clientsCount.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.stats_total_buildings),
                    value = buildingsCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.stats_paid_clients),
                    value = monthStats.paidCount.toString(),
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.stats_partial_clients),
                    value = monthStats.partiallyPaidCount.toString(),
                    color = Color(0xFFF57F17),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.stats_settled_clients),
                    value = monthStats.settledCount.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.stats_unpaid_clients),
                    value = monthStats.unpaidCount.toString(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.stats_settled_amount),
                    value = formatCurrencyLocalized(monthStats.settledAmount),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = stringResource(R.string.stats_total_paid_amount),
                    value = formatCurrencyLocalized(monthStats.totalPaidAmount),
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = stringResource(R.string.stats_total_unpaid_amount),
                    value = formatCurrencyLocalized(monthStats.totalUnpaidAmount),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// تنسيق مبالغ حسب Locale الحالي + عملة من strings
@Composable
private fun formatCurrencyLocalized(amount: Double): String {
    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale.getDefault()).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    }
    val formatted = numberFormat.format(amount)
    val suffix = stringResource(R.string.currency_suffix_sar)
    return stringResource(R.string.stats_amount_with_suffix, formatted, suffix)
}

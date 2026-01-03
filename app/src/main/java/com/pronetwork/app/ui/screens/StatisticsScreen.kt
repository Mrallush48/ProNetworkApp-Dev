package com.pronetwork.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pronetwork.app.data.Client
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    clientsCount: Int,
    buildingsCount: Int,
    paidClientsCount: Int,
    unpaidClientsCount: Int,
    allClients: List<Client>,
    monthOptions: List<String>,
    onMarkClientLate: (Client, String) -> Unit
) {
    var selectedMonth by remember { mutableStateOf(monthOptions.first()) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }
    var showLateClientsDialog by remember { mutableStateOf(false) }

    // Calculate late customers - clients who haven't paid for previous months
    val currentMonthIndex = monthOptions.indexOf(selectedMonth)
    val lateClients = if (currentMonthIndex > 0) {
        val previousMonths = monthOptions.subList(currentMonthIndex, monthOptions.size)
        allClients.filter { client ->
            // Check if client should be active in previous months and was not paid
            previousMonths.any { month ->
                try {
                    val clientDate = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(client.startMonth)
                    val monthDate = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(month)

                    if (clientDate != null && monthDate != null && monthDate.time >= clientDate.time) {
                        // Client should be active in this month, check if paid
                        if (client.endMonth != null) {
                            val endDate = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(client.endMonth)
                            endDate != null && monthDate.time < endDate.time && !client.isPaid
                        } else {
                            !client.isPaid
                        }
                    } else false
                } catch (e: Exception) {
                    false
                }
            }
        }
    } else {
        emptyList()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("إحصائيات التطبيق", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)

        // Main statistics cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "إجمالي العملاء",
                value = clientsCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "المباني",
                value = buildingsCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "العملاء المدفوع لهم",
                value = paidClientsCount.toString(),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "العملاء غير المدفوع لهم",
                value = unpaidClientsCount.toString(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
        }

        // Late customers section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "العملاء المتأخرين",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Month selection for late customers (محدث)
                ExposedDropdownMenuBox(
                    expanded = monthDropdownExpanded,
                    onExpandedChange = { monthDropdownExpanded = !monthDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedMonth,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("الشهر المرجعي") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthDropdownExpanded) },
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = monthDropdownExpanded,
                        onDismissRequest = { monthDropdownExpanded = false }
                    ) {
                        Column {
                            monthOptions.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(month) },
                                    onClick = {
                                        selectedMonth = month
                                        monthDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "عدد العملاء المتأخرين: ${lateClients.size}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (lateClients.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                if (lateClients.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showLateClientsDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("عرض التفاصيل", color = MaterialTheme.colorScheme.onError)
                    }
                }
            }
        }
    }

    // Late clients dialog
    if (showLateClientsDialog) {
        AlertDialog(
            onDismissRequest = { showLateClientsDialog = false },
            title = { Text("العملاء المتأخرين - $selectedMonth") },
            text = {
                Column {
                    lateClients.forEach { client ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    client.name,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    "رقم الاشتراك: ${client.subscriptionNumber}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "بداية الاشتراك: ${client.startMonth}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showLateClientsDialog = false }) {
                    Text("إغلاق")
                }
            }
        )
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
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = color
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
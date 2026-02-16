@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.pronetwork.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.stringResource
import com.pronetwork.app.R
import androidx.compose.material3.Icon



enum class PaymentReportType {
    SUMMARY,
    DETAILED
}

enum class PaymentReportPeriod {
    MONTHLY,
    QUARTERLY,
    YEARLY,
    CUSTOM
}

enum class PaymentReportFilter {
    ALL,
    PAID_ONLY,
    UNPAID_ONLY,
    PARTIAL_ONLY
}

@Composable
fun PaymentExportDialog(
    monthOptions: List<String>,
    selectedMonth: String,
    buildings: List<Pair<Int, String>>,
    packages: List<String>,
    onDismiss: () -> Unit,
    onExport: (
        reportType: PaymentReportType,
        period: PaymentReportPeriod,
        month: String,
        endMonth: String?,
        format: ExportFormat,
        buildingFilter: Int?,
        packageFilter: String?,
        statusFilter: PaymentReportFilter
    ) -> Unit
) {
    var reportType by remember { mutableStateOf(PaymentReportType.SUMMARY) }
    var period by remember { mutableStateOf(PaymentReportPeriod.MONTHLY) }

    var chosenMonth by remember { mutableStateOf(selectedMonth) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }

    var endMonth by remember { mutableStateOf(selectedMonth) }
    var endMonthDropdownExpanded by remember { mutableStateOf(false) }

    var buildingFilter by remember { mutableStateOf<Int?>(null) }
    var buildingDropdownExpanded by remember { mutableStateOf(false) }

    var packageFilter by remember { mutableStateOf<String?>(null) }
    var packageDropdownExpanded by remember { mutableStateOf(false) }

    var statusFilter by remember { mutableStateOf(PaymentReportFilter.ALL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    "Payment Report",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Configure your professional report",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ══════ REPORT TYPE ══════
                Text(
                    "Report Type",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = reportType == PaymentReportType.SUMMARY,
                        onClick = { reportType = PaymentReportType.SUMMARY },
                        label = { Text("Summary") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = reportType == PaymentReportType.DETAILED,
                        onClick = { reportType = PaymentReportType.DETAILED },
                        label = { Text("Detailed") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    if (reportType == PaymentReportType.SUMMARY)
                        "KPIs, collection rates, building analysis"
                    else
                        "Every client, every transaction, full audit trail",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                // ══════ PERIOD ══════
                Text(
                    "Period",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = period == PaymentReportPeriod.MONTHLY,
                        onClick = { period = PaymentReportPeriod.MONTHLY },
                        label = { Text("1M", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = period == PaymentReportPeriod.QUARTERLY,
                        onClick = { period = PaymentReportPeriod.QUARTERLY },
                        label = { Text("3M", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = period == PaymentReportPeriod.YEARLY,
                        onClick = { period = PaymentReportPeriod.YEARLY },
                        label = { Text("12M", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = period == PaymentReportPeriod.CUSTOM,
                        onClick = { period = PaymentReportPeriod.CUSTOM },
                        label = { Text("Custom", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    when (period) {
                        PaymentReportPeriod.MONTHLY -> "Single month report"
                        PaymentReportPeriod.QUARTERLY -> "3-month comparison with trends"
                        PaymentReportPeriod.YEARLY -> "12-month annual report with analysis"
                        PaymentReportPeriod.CUSTOM -> "Custom date range with comparison"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ══════ MONTH SELECTION ══════
                if (period == PaymentReportPeriod.MONTHLY || period == PaymentReportPeriod.QUARTERLY || period == PaymentReportPeriod.YEARLY) {
                    Text(
                        if (period == PaymentReportPeriod.MONTHLY) "Month"
                        else if (period == PaymentReportPeriod.QUARTERLY) "Starting Quarter Month"
                        else "Year (starting from)",
                        style = MaterialTheme.typography.labelMedium
                    )
                    ExposedDropdownMenuBox(
                        expanded = monthDropdownExpanded,
                        onExpandedChange = { monthDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = chosenMonth,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        ExposedDropdownMenu(
                            expanded = monthDropdownExpanded,
                            onDismissRequest = { monthDropdownExpanded = false }
                        ) {
                            monthOptions.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(month) },
                                    onClick = {
                                        chosenMonth = month
                                        monthDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (period == PaymentReportPeriod.CUSTOM) {
                    Text("From", style = MaterialTheme.typography.labelMedium)
                    ExposedDropdownMenuBox(
                        expanded = monthDropdownExpanded,
                        onExpandedChange = { monthDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = chosenMonth,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        ExposedDropdownMenu(
                            expanded = monthDropdownExpanded,
                            onDismissRequest = { monthDropdownExpanded = false }
                        ) {
                            monthOptions.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(month) },
                                    onClick = {
                                        chosenMonth = month
                                        monthDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text("To", style = MaterialTheme.typography.labelMedium)
                    ExposedDropdownMenuBox(
                        expanded = endMonthDropdownExpanded,
                        onExpandedChange = { endMonthDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = endMonth,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = endMonthDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        ExposedDropdownMenu(
                            expanded = endMonthDropdownExpanded,
                            onDismissRequest = { endMonthDropdownExpanded = false }
                        ) {
                            monthOptions.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(month) },
                                    onClick = {
                                        endMonth = month
                                        endMonthDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                // ══════ STATUS FILTER ══════
                Text(
                    "Payment Status",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = statusFilter == PaymentReportFilter.ALL,
                        onClick = { statusFilter = PaymentReportFilter.ALL },
                        label = { Text("All", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = statusFilter == PaymentReportFilter.PAID_ONLY,
                        onClick = { statusFilter = PaymentReportFilter.PAID_ONLY },
                        label = { Text("Paid", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = statusFilter == PaymentReportFilter.PARTIAL_ONLY,
                        onClick = { statusFilter = PaymentReportFilter.PARTIAL_ONLY },
                        label = { Text("Half", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = statusFilter == PaymentReportFilter.UNPAID_ONLY,
                        onClick = { statusFilter = PaymentReportFilter.UNPAID_ONLY },
                        label = { Text("None", style = MaterialTheme.typography.bodySmall) },
                        modifier = Modifier.weight(1f)
                    )
                }


                HorizontalDivider()

                // ══════ BUILDING FILTER ══════
                Text(
                    "Building",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                ExposedDropdownMenuBox(
                    expanded = buildingDropdownExpanded,
                    onExpandedChange = { buildingDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = buildingFilter?.let { bid ->
                            buildings.firstOrNull { it.first == bid }?.second
                        } ?: "All Buildings",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = buildingDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    ExposedDropdownMenu(
                        expanded = buildingDropdownExpanded,
                        onDismissRequest = { buildingDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Buildings") },
                            onClick = {
                                buildingFilter = null
                                buildingDropdownExpanded = false
                            }
                        )
                        buildings.forEach { (id, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    buildingFilter = id
                                    buildingDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // ══════ PACKAGE FILTER ══════
                Text(
                    "Package",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                ExposedDropdownMenuBox(
                    expanded = packageDropdownExpanded,
                    onExpandedChange = { packageDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = packageFilter ?: "All Packages",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = packageDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    ExposedDropdownMenu(
                        expanded = packageDropdownExpanded,
                        onDismissRequest = { packageDropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Packages") },
                            onClick = {
                                packageFilter = null
                                packageDropdownExpanded = false
                            }
                        )
                        packages.forEach { pkg ->
                            DropdownMenuItem(
                                text = { Text(pkg) },
                                onClick = {
                                    packageFilter = pkg
                                    packageDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // ══════ Save to Downloads ══════
                Text(
                    stringResource(R.string.export_save_to_downloads),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            onExport(
                                reportType, period, chosenMonth,
                                if (period == PaymentReportPeriod.CUSTOM) endMonth else null,
                                ExportFormat.PDF,
                                buildingFilter, packageFilter, statusFilter
                            )
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.export_pdf), style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            onExport(
                                reportType, period, chosenMonth,
                                if (period == PaymentReportPeriod.CUSTOM) endMonth else null,
                                ExportFormat.EXCEL,
                                buildingFilter, packageFilter, statusFilter
                            )
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.TableChart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.export_excel), style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ══════ Share ══════
                Text(
                    stringResource(R.string.export_share_via),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onExport(
                                reportType, period, chosenMonth,
                                if (period == PaymentReportPeriod.CUSTOM) endMonth else null,
                                ExportFormat.SHARE_PDF,
                                buildingFilter, packageFilter, statusFilter
                            )
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.export_share_pdf), style = MaterialTheme.typography.bodySmall)
                    }

                    OutlinedButton(
                        onClick = {
                            onExport(
                                reportType, period, chosenMonth,
                                if (period == PaymentReportPeriod.CUSTOM) endMonth else null,
                                ExportFormat.SHARE,
                                buildingFilter, packageFilter, statusFilter
                            )
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            Icons.Default.TableChart,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.export_share_excel), style = MaterialTheme.typography.bodySmall)
                    }
                }

                // Cancel
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.export_cancel))
                }
            }
        },

        dismissButton = null
    )
}

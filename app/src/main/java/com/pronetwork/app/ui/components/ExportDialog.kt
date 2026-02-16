package com.pronetwork.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pronetwork.app.R

enum class ExportType { SUMMARY, DETAILED }
enum class ExportPeriod { MONTHLY, QUARTERLY, YEARLY, CUSTOM }
enum class ExportFormat { SHARE, PDF, EXCEL, SHARE_PDF }


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onExport: (
        type: ExportType,
        period: ExportPeriod,
        format: ExportFormat,
        buildingFilter: Int?,
        packageFilter: String?
    ) -> Unit,
    showBuildingFilter: Boolean = true,
    showPackageFilter: Boolean = true,
    buildings: List<Pair<Int, String>> = emptyList(),
    packages: List<String> = emptyList()
) {
    var selectedType by remember { mutableStateOf(ExportType.SUMMARY) }
    var selectedPeriod by remember { mutableStateOf(ExportPeriod.MONTHLY) }
    var selectedBuilding by remember { mutableStateOf<Int?>(null) }
    var selectedPackage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.export_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        confirmButton = {},
        dismissButton = {},
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // نوع التقرير
                Text(
                    text = stringResource(R.string.export_report_type),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedType == ExportType.SUMMARY,
                        onClick = { selectedType = ExportType.SUMMARY },
                        label = { Text(stringResource(R.string.export_type_summary)) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedType == ExportType.DETAILED,
                        onClick = { selectedType = ExportType.DETAILED },
                        label = { Text(stringResource(R.string.export_type_detailed)) },
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider()

                // الفترة الزمنية
                Text(
                    text = stringResource(R.string.export_time_period),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedPeriod == ExportPeriod.MONTHLY,
                            onClick = { selectedPeriod = ExportPeriod.MONTHLY },
                            label = { Text(stringResource(R.string.export_period_monthly)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedPeriod == ExportPeriod.QUARTERLY,
                            onClick = { selectedPeriod = ExportPeriod.QUARTERLY },
                            label = { Text(stringResource(R.string.export_period_quarterly)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = selectedPeriod == ExportPeriod.YEARLY,
                            onClick = { selectedPeriod = ExportPeriod.YEARLY },
                            label = { Text(stringResource(R.string.export_period_yearly)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedPeriod == ExportPeriod.CUSTOM,
                            onClick = { selectedPeriod = ExportPeriod.CUSTOM },
                            label = { Text(stringResource(R.string.export_period_custom)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider()

                // الفلاتر
                if (showBuildingFilter && buildings.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.export_filter_building),
                        style = MaterialTheme.typography.labelMedium
                    )
                    var buildingExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = buildingExpanded,
                        onExpandedChange = { buildingExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedBuilding?.let { id ->
                                buildings.find { it.first == id }?.second
                            } ?: stringResource(R.string.export_all_buildings),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = buildingExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = buildingExpanded,
                            onDismissRequest = { buildingExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_all_buildings)) },
                                onClick = {
                                    selectedBuilding = null
                                    buildingExpanded = false
                                }
                            )
                            buildings.forEach { (id, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        selectedBuilding = id
                                        buildingExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (showPackageFilter && packages.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.export_filter_package),
                        style = MaterialTheme.typography.labelMedium
                    )
                    var packageExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = packageExpanded,
                        onExpandedChange = { packageExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedPackage
                                ?: stringResource(R.string.export_all_packages),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = packageExpanded)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = packageExpanded,
                            onDismissRequest = { packageExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.export_all_packages)) },
                                onClick = {
                                    selectedPackage = null
                                    packageExpanded = false
                                }
                            )
                            packages.forEach { pkg ->
                                DropdownMenuItem(
                                    text = { Text(pkg) },
                                    onClick = {
                                        selectedPackage = pkg
                                        packageExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

// ══════ حفظ في Downloads ══════
                Text(
                    text = stringResource(R.string.export_save_to_downloads),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            onExport(
                                selectedType,
                                selectedPeriod,
                                ExportFormat.PDF,
                                selectedBuilding,
                                selectedPackage
                            )
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                stringResource(R.string.export_pdf),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            onExport(
                                selectedType,
                                selectedPeriod,
                                ExportFormat.EXCEL,
                                selectedBuilding,
                                selectedPackage
                            )
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Icon(
                                Icons.Default.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                stringResource(R.string.export_excel),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

// ══════ مشاركة ══════
                Text(
                    text = stringResource(R.string.export_share_via),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onExport(
                                selectedType,
                                selectedPeriod,
                                ExportFormat.SHARE_PDF,
                                selectedBuilding,
                                selectedPackage
                            )
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                stringResource(R.string.export_share_pdf),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            onExport(
                                selectedType,
                                selectedPeriod,
                                ExportFormat.SHARE,
                                selectedBuilding,
                                selectedPackage
                            )
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Icon(
                                Icons.Default.TableChart,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                stringResource(R.string.export_share_excel),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

// زر Cancel
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(stringResource(R.string.export_cancel))
                }
            }
        }
    )
}

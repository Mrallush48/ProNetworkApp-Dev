package com.pronetwork.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import com.pronetwork.app.R

enum class ExportOption { CSV, PDF, ALL_PAYMENTS, SELECTED_MONTHS, IMPORT_CSV }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTopBar(
    title: String,
    showOptions: Boolean = true,
    options: List<ExportOption> = emptyList(),
    onOptionClick: (ExportOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = { Text(text = title) },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        actions = {
            if (showOptions && options.isNotEmpty()) {
                IconButton(onClick = { expanded = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (option) {
                                        ExportOption.CSV -> "Export to CSV"
                                        ExportOption.PDF -> "Export to PDF"
                                        ExportOption.ALL_PAYMENTS -> "Export All Payments"
                                        ExportOption.SELECTED_MONTHS -> "Export Selected Months"
                                        ExportOption.IMPORT_CSV -> "Import from CSV"
                                    }
                                )
                            },
                            onClick = {
                                expanded = false
                                onOptionClick(option)
                            }
                        )
                    }
                }
            }
        }
    )
}

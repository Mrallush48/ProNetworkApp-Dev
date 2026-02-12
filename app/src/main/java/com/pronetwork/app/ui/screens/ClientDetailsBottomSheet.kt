package com.pronetwork.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pronetwork.app.R
import com.pronetwork.app.data.Client
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailsBottomSheet(
    client: Client,
    buildingName: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePaid: () -> Unit,   // للتوافق مع الاستدعاءات فقط
    onUndoPaid: () -> Unit,     // للتوافق مع الاستدعاءات فقط
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // العنوان + أزرار تعديل/حذف
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.client_details_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onEdit,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.client_details_edit_button),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    IconButton(
                        onClick = { showDeleteDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.client_details_delete_button),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // معلومات العميل
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    DetailRow(
                        label = stringResource(R.string.client_details_name_label),
                        value = client.name
                    )
                    Spacer(Modifier.height(12.dp))
                    DetailRow(
                        label = stringResource(R.string.client_details_subscription_label),
                        value = client.subscriptionNumber
                    )

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    DetailRow(
                        label = stringResource(R.string.client_details_building_label),
                        value = buildingName
                    )
                    Spacer(Modifier.height(12.dp))
                    DetailRow(
                        label = stringResource(R.string.client_details_package_label),
                        value = client.packageType
                    )

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.client_details_monthly_price_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    R.string.client_details_monthly_price_value,
                                    client.price
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.client_details_start_month_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = client.startMonth,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    if (client.phone.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(16.dp))
                        DetailRow(
                            label = stringResource(R.string.client_details_phone_label),
                            value = client.phone
                        )
                    }

                    if (client.address.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        DetailRow(
                            label = stringResource(R.string.client_details_address_label),
                            value = client.address
                        )
                    }

                    if (client.roomNumber?.isNotEmpty() == true) {
                        Spacer(Modifier.height(12.dp))
                        DetailRow(
                            label = stringResource(R.string.client_details_room_label),
                            value = client.roomNumber!!
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    if (client.notes.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.client_details_notes_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(
                                    text = client.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.padding(16.dp)
        )

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Text(
                        text = stringResource(R.string.client_details_delete_client_title)
                    )
                },
                text = {
                    Text(
                        text = stringResource(
                            R.string.client_details_delete_client_text,
                            client.name
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDelete()
                            showDeleteDialog = false
                            scope.launch {
                                sheetState.hide()
                            }.invokeOnCompletion {
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.client_details_delete_client_yes)
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteDialog = false }) {
                        Text(text = stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

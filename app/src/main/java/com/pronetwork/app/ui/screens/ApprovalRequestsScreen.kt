package com.pronetwork.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pronetwork.app.R
import com.pronetwork.app.network.ApprovalRequestResponse
import com.pronetwork.app.viewmodel.ApprovalRequestsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApprovalRequestsScreen(
    uiState: ApprovalRequestsUiState,
    onRefresh: () -> Unit,
    onApprove: (Int) -> Unit,
    onRejectClick: (Int) -> Unit,
    onDismissReject: () -> Unit,
    onConfirmReject: (Int) -> Unit,
    onFilterChange: (String) -> Unit,
    onClearMessages: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val approvedMsg = stringResource(R.string.approval_success_approved)
    val rejectedMsg = stringResource(R.string.approval_success_rejected)

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            val text = when (msg) {
                "REQUEST_APPROVED" -> approvedMsg
                "REQUEST_REJECTED" -> rejectedMsg
                else -> msg
            }
            snackbarHostState.showSnackbar(text)
            onClearMessages()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            if (!uiState.showRejectDialog) {
                snackbarHostState.showSnackbar(msg)
                onClearMessages()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.screen_approval_requests))
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.approval_refresh))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp)
        ) {
            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("ALL", "PENDING", "APPROVED", "REJECTED")
                filters.forEach { filter ->
                    FilterChip(
                        selected = uiState.selectedFilter == filter,
                        onClick = { onFilterChange(filter) },
                        label = {
                            Text(
                                when (filter) {
                                    "ALL" -> stringResource(R.string.approval_filter_all)
                                    "PENDING" -> stringResource(R.string.approval_filter_pending)
                                    "APPROVED" -> stringResource(R.string.approval_filter_approved)
                                    "REJECTED" -> stringResource(R.string.approval_filter_rejected)
                                    else -> filter
                                }
                            )
                        }
                    )
                }
            }

            if (uiState.isLoading && uiState.requests.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.requests.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.approval_no_requests),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.requests, key = { it.id }) { request ->
                        RequestCard(
                            request = request,
                            isAdmin = uiState.isAdmin,
                            onApprove = { onApprove(request.id) },
                            onReject = { onRejectClick(request.id) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showRejectDialog && uiState.selectedRequestId != null) {
        AlertDialog(
            onDismissRequest = onDismissReject,
            title = { Text(stringResource(R.string.approval_confirm_reject_title)) },
            text = { Text(stringResource(R.string.approval_confirm_reject_msg)) },
            confirmButton = {
                Button(
                    onClick = { onConfirmReject(uiState.selectedRequestId) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.approval_reject_request))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissReject) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
@Composable
fun RequestCard(
    request: ApprovalRequestResponse,
    isAdmin: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (request.request_type) {
                            "DELETE_CLIENT" -> stringResource(R.string.approval_type_delete_client)
                            "DELETE_BUILDING" -> stringResource(R.string.approval_type_delete_building)
                            "EXPORT_REPORT" -> stringResource(R.string.approval_type_export_report)
                            "DISCONNECT_CLIENT" -> stringResource(R.string.approval_type_disconnect)
                            else -> request.request_type
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.approval_target_label) + " " + (request.target_name ?: stringResource(R.string.approval_target_unknown)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                StatusBadge(status = request.status)
            }

            if (!request.reason.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.approval_reason_label) + " " + request.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = request.created_at.take(16).replace("T", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (isAdmin && request.status == "PENDING") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onReject,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.approval_btn_reject),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }

                        IconButton(
                            onClick = onApprove,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = stringResource(R.string.approval_btn_approve),
                                tint = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val (color, text) = when (status) {
        "PENDING" -> MaterialTheme.colorScheme.primary to stringResource(R.string.approval_status_pending)
        "APPROVED" -> Color(0xFF4CAF50) to stringResource(R.string.approval_status_approved)
        "REJECTED" -> MaterialTheme.colorScheme.error to stringResource(R.string.approval_status_rejected)
        else -> MaterialTheme.colorScheme.onSurface to status
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

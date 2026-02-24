package com.pronetwork.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pronetwork.app.R
import com.pronetwork.app.network.ApprovalRequestResponse
import com.pronetwork.app.viewmodel.ApprovalRequestsUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRequestsScreen(
    uiState: ApprovalRequestsUiState,
    onRefresh: () -> Unit,
    onFilterChange: (String) -> Unit,
    onClearMessages: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            onClearMessages()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.my_requests_title)) },
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
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            stringResource(R.string.my_requests_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.requests, key = { it.id }) { request ->
                        MyRequestCard(request = request)
                    }
                }
            }
        }
    }
}

@Composable
fun MyRequestCard(request: ApprovalRequestResponse) {
    val statusColor = when (request.status) {
        "PENDING" -> MaterialTheme.colorScheme.primary
        "APPROVED" -> Color(0xFF4CAF50)
        "REJECTED" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    val statusIcon = when (request.status) {
        "PENDING" -> Icons.Default.HourglassTop
        "APPROVED" -> Icons.Default.CheckCircle
        "REJECTED" -> Icons.Default.Cancel
        else -> Icons.Default.Info
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(Modifier.padding(16.dp)) {
            // Status icon on the left
            Icon(
                statusIcon,
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 12.dp)
                    .align(Alignment.CenterVertically)
            )

            Column(modifier = Modifier.weight(1f)) {
                // Request type
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

                // Target name
                Text(
                    text = request.target_name ?: stringResource(R.string.approval_target_unknown),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Reason if exists
                if (!request.reason.isNullOrBlank()) {
                    Text(
                        text = stringResource(R.string.approval_reason_label) + " " + request.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(8.dp))

                // Date + Status badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = request.created_at.take(16).replace("T", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    StatusBadge(status = request.status)
                }

                // Reviewed date if available
                if (request.reviewed_at != null) {
                    Text(
                        text = stringResource(R.string.my_requests_reviewed_at) + " " +
                                request.reviewed_at.take(16).replace("T", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }
        }
    }
}

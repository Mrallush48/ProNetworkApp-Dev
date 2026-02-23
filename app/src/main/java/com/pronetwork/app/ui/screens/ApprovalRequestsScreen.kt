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

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { msg ->
            val text = when (msg) {
                "REQUEST_APPROVED" -> "تمت الموافقة على الطلب بنجاح"
                "REQUEST_REJECTED" -> "تم رفض الطلب"
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
                title = { Text(stringResource(R.string.screen_approval_requests)) },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "تحديث")
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
                                    "ALL" -> "الكل"
                                    "PENDING" -> "قيد الانتظار"
                                    "APPROVED" -> "مقبول"
                                    "REJECTED" -> "مرفوض"
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
                    Text("لا توجد طلبات حالياً", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            title = { Text("تأكيد الرفض") },
            text = { Text("هل أنت متأكد من رفض هذا الطلب؟") },
            confirmButton = {
                Button(
                    onClick = { onConfirmReject(uiState.selectedRequestId) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("رفض الطلب")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissReject) {
                    Text("إلغاء")
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
                            "DELETE_CLIENT" -> "حذف مشترك"
                            "DELETE_BUILDING" -> "حذف مبنى"
                            "EXPORT_REPORT" -> "تصدير تقرير"
                            "DISCONNECT_CLIENT" -> "فصل خدمة"
                            else -> request.request_type
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "الهدف: ${request.target_name ?: "غير محدد"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                StatusBadge(status = request.status)
            }

            if (!request.reason.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "السبب: ${request.reason}",
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
                            Icon(Icons.Default.Close, contentDescription = "رفض", tint = MaterialTheme.colorScheme.error)
                    Phase 5.4: Add ApprovalRequestsScreen.kt    }
                        IconButton(
                            onClick = onApprove,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "موافقة", tint = Color(0xFF4CAF50))
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
        "PENDING" -> MaterialTheme.colorScheme.primary to "قيد الانتظار"
        "APPROVED" -> Color(0xFF4CAF50) to "تمت الموافقة"
        "REJECTED" -> MaterialTheme.colorScheme.error to "مرفوض"
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

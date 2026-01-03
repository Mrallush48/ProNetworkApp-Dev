package com.pronetwork.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pronetwork.app.data.Client
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailsScreen(
    client: Client,
    buildingName: String,
    onEdit: (Client) -> Unit,
    onDelete: (Client) -> Unit,
    onTogglePaid: (Boolean) -> Unit,
    onUndoPaid: () -> Unit,
    onBack: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل العميل") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("معلومات العميل", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("الاسم:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(client.name, style = MaterialTheme.typography.bodyLarge)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("رقم الاشتراك:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(client.subscriptionNumber, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("المبنى:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(buildingName, style = MaterialTheme.typography.bodyLarge)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("الباقة:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(client.packageType, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("السعر:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${client.price} ريال", style = MaterialTheme.typography.bodyLarge)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("شهر البداية:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(client.startMonth, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    if (client.phone.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("رقم الجوال:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(client.phone, style = MaterialTheme.typography.bodyLarge)
                    }

                    if (client.address.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("العنوان:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(client.address, style = MaterialTheme.typography.bodyLarge)
                    }

                    if (client.roomNumber?.isNotEmpty() == true) {
                        Spacer(Modifier.height(12.dp))
                        Text("رقم الغرفة:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(client.roomNumber!!, style = MaterialTheme.typography.bodyLarge)
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("حالة الدفع:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            val statusColor = if (client.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            Text(
                                if (client.isPaid) "مدفوع" else "غير مدفوع",
                                style = MaterialTheme.typography.bodyLarge,
                                color = statusColor
                            )
                        }
                        if (client.isPaid && client.paymentDate != null) {
                            Column(Modifier.weight(1f)) {
                                Text("تاريخ الدفع:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val paymentDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                    .format(java.util.Date(client.paymentDate))
                                Text(paymentDate, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }

                    if (client.notes.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("ملاحظات:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(client.notes, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "مزامنة عبر جميع الشهور",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onEdit(client) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("تعديل")
                }

                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }

                if (!client.isPaid) {
                    var paymentClickCount by remember { mutableIntStateOf(0) }
                    LaunchedEffect(paymentClickCount) {
                        if (paymentClickCount > 0) {
                            kotlinx.coroutines.delay(3000) // Reset after 3 seconds
                            paymentClickCount = 0
                        }
                    }

                    Button(
                        onClick = {
                            paymentClickCount++
                            if (paymentClickCount >= 2) {
                                onTogglePaid(true)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "تم تأكيد الدفع",
                                        actionLabel = "تراجع"
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        onUndoPaid()
                                    }
                                }
                                paymentClickCount = 0
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("اضغط مرة أخرى للتأكيد")
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            if (paymentClickCount == 1) "اضغط للتأكيد" else "تأكيد الدفع",
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                } else {
                    var undoClickCount by remember { mutableIntStateOf(0) }
                    LaunchedEffect(undoClickCount) {
                        if (undoClickCount > 0) {
                            kotlinx.coroutines.delay(3000) // Reset after 3 seconds
                            undoClickCount = 0
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            undoClickCount++
                            if (undoClickCount >= 2) {
                                onUndoPaid()
                                scope.launch {
                                    snackbarHostState.showSnackbar("تم التراجع عن الدفع")
                                }
                                undoClickCount = 0
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar("اضغط مرة أخرى للتأكيد")
                                }
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (undoClickCount == 1) "اضغط للتأكيد" else "تراجع")
                    }
                }
            }
        }
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("تأكيد الحذف") },
                text = { Text("هل أنت متأكد من حذف العميل؟") },
                confirmButton = {
                    Button(onClick = {
                        onDelete(client)
                        showDeleteDialog = false
                        onBack()
                    }) { Text("نعم") }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteDialog = false }) { Text("إلغاء") }
                }
            )
        }
    }
}
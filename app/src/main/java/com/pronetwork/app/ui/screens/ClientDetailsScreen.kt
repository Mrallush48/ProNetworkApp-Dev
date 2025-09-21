package com.pronetwork.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.runtime.*
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
        Column(Modifier.padding(padding).padding(24.dp)) {
            Text("الاسم: ${client.name}", style = MaterialTheme.typography.titleLarge)
            Text("المبنى: $buildingName", style = MaterialTheme.typography.bodyMedium)
            Text("رقم الاشتراك: ${client.subscriptionNumber}", style = MaterialTheme.typography.bodyMedium)
            Text("الباقة: ${client.packageType}", style = MaterialTheme.typography.bodyMedium)
            Text("الحالة: ${if (client.isPaid) "مدفوع" else "غير مدفوع"}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            Row {
                Button(onClick = { onEdit(client) }) { Text("تعديل") }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = { showDeleteDialog = true }) { Text("حذف") }
                Spacer(Modifier.width(8.dp))
                if (!client.isPaid) {
                    Button(
                        onClick = {
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
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("تأكيد الدفع", color = MaterialTheme.colorScheme.onTertiary)
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            onUndoPaid()
                            scope.launch { snackbarHostState.showSnackbar("تم التراجع عن الدفع") }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("تراجع")
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
package com.pronetwork.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pronetwork.app.data.Client

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailsScreen(
    client: Client,
    onBack: () -> Unit,
    onEdit: (Client) -> Unit,
    onDelete: (Client) -> Unit
) {
    // ✅ حالة لإظهار نافذة التأكيد
    var showDeleteDialog by remember { mutableStateOf(false) }

    // ✅ نافذة تأكيد الحذف
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل أنت متأكد أنك تريد حذف هذا العميل؟ لا يمكن التراجع بعد الحذف.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(client) // الحذف يتم بعد التأكيد فقط
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل العميل") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(text = "الاسم: ${client.name}", style = MaterialTheme.typography.titleMedium)
            Text(text = "رقم الاشتراك: ${client.subscriptionNumber}")
            Text(text = "الغرفة: ${client.roomNumber ?: "-"}")
            Text(text = "الجوال: ${client.mobile ?: "-"}")
            Text(text = "السعر: ${client.price} ر.س")
            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { onEdit(client) }) {
                    Text("تعديل")
                }
                Button(
                    onClick = { showDeleteDialog = true }, // ✅ نفتح نافذة التأكيد بدل الحذف مباشرة
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            }
        }
    }
}

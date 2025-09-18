package com.pronetwork.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pronetwork.app.data.Building

@Composable
fun BuildingEditDialog(
    building: Building?,
    onDismiss: () -> Unit,
    onSave: (Building) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(building?.name ?: "") }
    var description by remember { mutableStateOf(building?.description ?: "") }

    // ✅ حالة لإظهار نافذة تأكيد الحذف
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog && building != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل أنت متأكد أنك تريد حذف هذا المبنى؟ سيتم حذف جميع العملاء المرتبطين به ولا يمكن التراجع.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("تأكيد الحذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (building == null) "إضافة مبنى" else "تعديل مبنى") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("اسم المبنى") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("الوصف (اختياري)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            building?.copy(name = name, description = description)
                                ?: Building(name = name, description = description)
                        )
                    }
                }
            ) { Text("حفظ") }
        },
        dismissButton = {
            Row {
                if (building != null && onDelete != null) {
                    TextButton(onClick = { showDeleteDialog = true }) {
                        Text("حذف", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) { Text("إلغاء") }
            }
        }
    )
}

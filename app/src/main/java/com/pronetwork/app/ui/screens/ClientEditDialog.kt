package com.pronetwork.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pronetwork.app.data.Client

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientEditDialog(
    client: Client? = null,
    selectedMonthIso: String,
    buildingId: Int,
    onDismiss: () -> Unit,
    onSave: (Client) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by rememberSaveable { mutableStateOf(client?.name ?: "") }
    var subscriptionNumber by rememberSaveable { mutableStateOf(client?.subscriptionNumber ?: "") }
    var roomNumber by rememberSaveable { mutableStateOf(client?.roomNumber ?: "") }
    var mobile by rememberSaveable { mutableStateOf(client?.mobile ?: "") }
    var priceText by rememberSaveable { mutableStateOf(client?.price?.toString() ?: "") }
    var isPaid by rememberSaveable { mutableStateOf(client?.isPaid ?: false) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm && client != null && onDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل أنت متأكد أنك تريد حذف هذا العميل؟ لا يمكن التراجع بعد الحذف.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onDelete()
                }) {
                    Text("تأكيد الحذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (client == null) "إضافة عميل" else "تعديل عميل") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("الاسم") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subscriptionNumber, onValueChange = { subscriptionNumber = it }, label = { Text("رقم الاشتراك") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                OutlinedTextField(value = roomNumber, onValueChange = { roomNumber = it }, label = { Text("رقم الغرفة (اختياري)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("رقم الجوال (اختياري)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("السعر") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPaid, onCheckedChange = { isPaid = it })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تم الدفع")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val priceDouble = priceText.toDoubleOrNull() ?: 0.0
                val result = client?.copy(
                    name = name,
                    subscriptionNumber = subscriptionNumber,
                    roomNumber = roomNumber.ifBlank { null },
                    mobile = mobile.ifBlank { null },
                    price = priceDouble,
                    isPaid = isPaid,
                    startMonth = selectedMonthIso,
                    buildingId = buildingId
                ) ?: Client(
                    id = 0,
                    name = name,
                    subscriptionNumber = subscriptionNumber,
                    roomNumber = roomNumber.ifBlank { null },
                    mobile = mobile.ifBlank { null },
                    price = priceDouble,
                    buildingId = buildingId,
                    startMonth = selectedMonthIso,
                    endMonth = null,
                    isPaid = isPaid,
                    paymentDate = null
                )
                onSave(result)
            }) {
                Text("حفظ")
            }
        },
        dismissButton = {
            Row {
                if (client != null && onDelete != null) {
                    TextButton(onClick = { showDeleteConfirm = true }) {
                        Text("حذف", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                TextButton(onClick = onDismiss) {
                    Text("إلغاء")
                }
            }
        }
    )
}

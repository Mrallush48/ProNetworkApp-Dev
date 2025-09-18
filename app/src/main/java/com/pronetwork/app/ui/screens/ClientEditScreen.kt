package com.pronetwork.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pronetwork.app.data.Client

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientEditScreen(
    client: Client,
    onBack: () -> Unit,
    onSave: (Client) -> Unit
) {
    // ✅ نستخدم نسخة قابلة للتعديل من القيم
    var name by remember { mutableStateOf(client.name) }
    var subscriptionNumber by remember { mutableStateOf(client.subscriptionNumber) }
    var roomNumber by remember { mutableStateOf(client.roomNumber ?: "") }
    var mobile by remember { mutableStateOf(client.mobile ?: "") }
    var price by remember { mutableStateOf(client.price.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (client.id == 0) "إضافة عميل جديد" else "تعديل العميل") },
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
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = subscriptionNumber,
                onValueChange = { subscriptionNumber = it },
                label = { Text("رقم الاشتراك") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = roomNumber,
                onValueChange = { roomNumber = it },
                label = { Text("رقم الغرفة") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mobile,
                onValueChange = { mobile = it },
                label = { Text("رقم الجوال") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("السعر") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val priceDouble = price.toDoubleOrNull()
                    if (priceDouble != null && name.isNotBlank() && subscriptionNumber.isNotBlank()) {
                        // ✅ نرجع نسخة جديدة من العميل مع القيم الجديدة
                        onSave(
                            client.copy(
                                name = name,
                                subscriptionNumber = subscriptionNumber,
                                roomNumber = roomNumber.ifBlank { null },
                                mobile = mobile.ifBlank { null },
                                price = priceDouble
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("حفظ")
            }
        }
    }
}

package com.pronetwork.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pronetwork.app.data.Client
import com.pronetwork.app.data.Building

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientEditScreen(
    client: Client?,
    buildingList: List<Building>,
    onSave: (Client) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(client?.name ?: "") }
    var subscriptionNumber by remember { mutableStateOf(client?.subscriptionNumber ?: "") }
    var price by remember { mutableStateOf(client?.price?.toString() ?: "") }
    var buildingId by remember { mutableIntStateOf(client?.buildingId ?: (buildingList.firstOrNull()?.id ?: 0)) }
    var phone by remember { mutableStateOf(client?.phone ?: "") }
    var address by remember { mutableStateOf(client?.address ?: "") }
    var packageType by remember { mutableStateOf(client?.packageType ?: "5Mbps") }
    var notes by remember { mutableStateOf(client?.notes ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (client == null) "إضافة عميل" else "تعديل عميل") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("اسم العميل") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = subscriptionNumber,
                onValueChange = { subscriptionNumber = it },
                label = { Text("رقم الاشتراك") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("السعر") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            // ... باقي الحقول حسب حاجتك ...
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val parsedPrice = price.toDoubleOrNull() ?: 0.0
                    onSave(
                        Client(
                            name = name,
                            subscriptionNumber = subscriptionNumber,
                            price = parsedPrice,
                            buildingId = buildingId,
                            startMonth = client?.startMonth ?: "",
                            phone = phone,
                            address = address,
                            packageType = packageType,
                            notes = notes
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("حفظ")
            }
        }
    }
}
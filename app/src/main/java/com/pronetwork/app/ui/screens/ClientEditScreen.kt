package com.pronetwork.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    // --- الحالات الأساسية ---
    var name by remember { mutableStateOf(client?.name ?: "") }
    var subscriptionNumber by remember { mutableStateOf(client?.subscriptionNumber ?: "") }
    var price by remember { mutableStateOf(client?.price?.toString() ?: "") }
    var phone by remember { mutableStateOf(client?.phone ?: "") }
    var address by remember { mutableStateOf(client?.address ?: "") }
    var notes by remember { mutableStateOf(client?.notes ?: "") }

    // --- حالات القوائم المنسدلة ---

    // حالة المبنى
    val initialBuilding = buildingList.find { it.id == (client?.buildingId ?: 0) }
    var selectedBuilding by remember { mutableStateOf(initialBuilding) }
    var buildingExpanded by remember { mutableStateOf(false) }

    // حالة الباقة
    val packageOptions = listOf("5Mbps", "10Mbps", "20Mbps", "50Mbps", "100Mbps")
    var selectedPackage by remember { mutableStateOf(client?.packageType ?: packageOptions.first()) }
    var packageExpanded by remember { mutableStateOf(false) }

    // حالة تاريخ البداية (كمثال لقائمة بسيطة)
    val dateOptions = listOf("1 يناير", "1 فبراير", "1 مارس", "1 أبريل", "1 مايو", "1 يونيو", "1 يوليو", "1 أغسطس", "1 سبتمبر", "1 أكتوبر", "1 نوفمبر", "1 ديسمبر")
    var selectedDate by remember { mutableStateOf(client?.startMonth ?: dateOptions.first()) }
    var dateExpanded by remember { mutableStateOf(false) }


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
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = subscriptionNumber,
                onValueChange = { subscriptionNumber = it },
                label = { Text("رقم الاشتراك") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("السعر") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // === حقل المبنى (القائمة المنسدلة) ===
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = buildingExpanded,
                onExpandedChange = { buildingExpanded = !buildingExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedBuilding?.name ?: "اختر مبنى",
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = buildingExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = buildingExpanded,
                    onDismissRequest = { buildingExpanded = false }
                ) {
                    LazyColumn {
                        items(buildingList) { building ->
                            DropdownMenuItem(
                                text = { Text(building.name) },
                                onClick = {
                                    selectedBuilding = building
                                    buildingExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // === حقل الباقة (القائمة المنسدلة) ===
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = packageExpanded,
                onExpandedChange = { packageExpanded = !packageExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedPackage,
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = packageExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = packageExpanded,
                    onDismissRequest = { packageExpanded = false }
                ) {
                    packageOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedPackage = option
                                packageExpanded = false
                            }
                        )
                    }
                }
            }

            // === حقل تاريخ البداية (القائمة المنسدلة) ===
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = dateExpanded,
                onExpandedChange = { dateExpanded = !dateExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedDate,
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = dateExpanded,
                    onDismissRequest = { dateExpanded = false }
                ) {
                    dateOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedDate = option
                                dateExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("رقم الهاتف") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("العنوان") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("ملاحظات") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val parsedPrice = price.toDoubleOrNull() ?: 0.0
                    onSave(
                        Client(
                            name = name,
                            subscriptionNumber = subscriptionNumber,
                            price = parsedPrice,
                            buildingId = selectedBuilding?.id ?: 0, // نستخدم الـ ID من المبنى المختار
                            startMonth = selectedDate, // نستخدم التاريخ المختار
                            phone = phone,
                            address = address,
                            packageType = selectedPackage, // نستخدم الباقة المختارة
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
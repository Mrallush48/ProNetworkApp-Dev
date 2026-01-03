package com.pronetwork.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pronetwork.app.data.Building
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState()

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onDateSelected(datePickerState.selectedDateMillis) }) {
                Text("تأكيد")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientEditDialog(
    buildingList: List<Building>,
    initialName: String = "",
    initialSubscriptionNumber: String = "",
    initialPrice: String = "",
    initialBuildingId: Int? = null,
    initialStartMonth: String = "",
    initialPhone: String = "",
    initialAddress: String = "",
    initialPackageType: String = "5Mbps",
    initialNotes: String = "",
    buildingSelectionEnabled: Boolean = true,
    onSave: (String, String, Double, Int, String, String, String, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    // --- الحالات الأساسية ---
    var name by remember { mutableStateOf(initialName) }
    var subscriptionNumber by remember { mutableStateOf(initialSubscriptionNumber) }
    var price by remember { mutableStateOf(initialPrice) }
    var phone by remember { mutableStateOf(initialPhone) }
    var address by remember { mutableStateOf(initialAddress) }
    var notes by remember { mutableStateOf(initialNotes) }

    // --- حالات القوائم المنسدلة ---

    // حالة المبنى (محدثة)
    val initialBuilding = buildingList.find { it.id == (initialBuildingId ?: 0) }
    var selectedBuilding by remember { mutableStateOf(initialBuilding) }
    var buildingExpanded by remember { mutableStateOf(false) }

    // حالة الباقة (محدثة)
    val packageOptions = listOf("5Mbps", "7Mbps", "10Mbps", "15Mbps", "20Mbps", "25Mbps", "30Mbps")
    var selectedPackage by remember { mutableStateOf(initialPackageType.ifEmpty { packageOptions.first() }) }
    var packageExpanded by remember { mutableStateOf(false) }

    // حالة تاريخ البداية
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var startMonth by remember {
        mutableStateOf(
            initialStartMonth.ifEmpty {
                dateFormatter.format(Date())
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل/إضافة عميل", style = MaterialTheme.typography.titleLarge) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
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
                    Spacer(Modifier.height(8.dp))
                }

                if (buildingSelectionEnabled) {
                    item {
                        // === التعديل الرئيسي هنا ===
                        // التحقق أولاً مما إذا كانت قائمة المباني تحتوي على عناصر
                        if (buildingList.isNotEmpty()) {
                            ExposedDropdownMenuBox(
                                expanded = buildingExpanded,
                                onExpandedChange = { buildingExpanded = !buildingExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedBuilding?.name ?: "اختر مبنى",
                                    onValueChange = { },
                                    readOnly = true,
                                    label = { Text("المبنى") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = buildingExpanded) },
                                    modifier = Modifier.menuAnchor()
                                )
                                ExposedDropdownMenu(
                                    expanded = buildingExpanded,
                                    onDismissRequest = { buildingExpanded = false }
                                ) {
                                    // === الإصلاح النهائي: استبدال LazyColumn بـ Column ===
                                    Column {
                                        buildingList.forEach { building ->
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
                        } else {
                            OutlinedTextField(
                                value = "لا توجد مباني متاحة. الرجاء إضافة مبنى أولاً.",
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("المبنى") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                item {
                    // === حقل تاريخ البداية ===
                    ExposedDropdownMenuBox(
                        expanded = false, // لا نستخدم expanded state هنا لأننا نفتح dialog
                        onExpandedChange = { showDatePicker = true }, // عند الضغط نفتح الـ dialog
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = startMonth,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("تاريخ البداية") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) },
                            modifier = Modifier.menuAnchor()
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("رقم الجوال") },
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
                }

                item {
                    // === حقل الباقة (القائمة المنسدلة) ===
                    ExposedDropdownMenuBox(
                        expanded = packageExpanded,
                        onExpandedChange = { packageExpanded = !packageExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedPackage,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("الباقة") },
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
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("ملاحظات") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedPrice = price.toDoubleOrNull() ?: 0.0
                    onSave(
                        name,
                        subscriptionNumber,
                        parsedPrice,
                        selectedBuilding?.id ?: 0,
                        startMonth,
                        phone,
                        address,
                        selectedPackage,
                        notes
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("حفظ", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )

    // Date Picker Dialog
    if (showDatePicker) {
        CustomDatePickerDialog(
            onDateSelected = { dateMillis ->
                if (dateMillis != null) {
                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = dateMillis
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH) + 1
                    val day = calendar.get(Calendar.DAY_OF_MONTH)
                    startMonth = String.format("%04d-%02d-%02d", year, month, day)
                }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}
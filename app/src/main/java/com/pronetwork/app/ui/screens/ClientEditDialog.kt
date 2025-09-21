package com.pronetwork.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
    // Generate month options for compatibility with minSdk 24
    val months = remember {
        val calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        List(25) {
            val monthString = formatter.format(calendar.time)
            calendar.add(Calendar.MONTH, -1)
            monthString
        }
    }
    val monthOptions = months
    
    // Date picker state
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { 
        mutableStateOf(
            if (initialStartMonth.isNotEmpty()) {
                try {
                    val parts = initialStartMonth.split("-")
                    val year = parts[0].toInt()
                    val month = parts[1].toInt() - 1 // Calendar month is 0-based
                    val calendar = Calendar.getInstance()
                    calendar.set(year, month, 1)
                    calendar.timeInMillis
                } catch (e: Exception) {
                    System.currentTimeMillis()
                }
            } else {
                System.currentTimeMillis()
            }
        )
    }
    
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val monthFormatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    var startMonth by remember { 
        mutableStateOf(
            initialStartMonth.ifEmpty { 
                monthFormatter.format(Date(selectedDate))
            }
        )
    }

    var name by remember { mutableStateOf(initialName) }
    var subscriptionNumber by remember { mutableStateOf(initialSubscriptionNumber) }
    var price by remember { mutableStateOf(initialPrice) }
    var buildingId by remember { mutableIntStateOf(initialBuildingId ?: (buildingList.firstOrNull()?.id ?: 0)) }
    var phone by remember { mutableStateOf(initialPhone) }
    var address by remember { mutableStateOf(initialAddress) }
    var notes by remember { mutableStateOf(initialNotes) }
    var packageType by remember { mutableStateOf(initialPackageType) }
    var packageExpanded by remember { mutableStateOf(false) }
    val packageOptions = listOf("5Mbps", "7Mbps", "10Mbps", "15Mbps", "20Mbps", "25Mbps", "30Mbps")
    var buildingDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل/إضافة عميل", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
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
                if (buildingSelectionEnabled) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        OutlinedTextField(
                            value = buildingList.firstOrNull { it.id == buildingId }?.name ?: "",
                            onValueChange = {},
                            label = { Text("المبنى") },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { buildingDropdownExpanded = true },
                            trailingIcon = {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                        )
                        DropdownMenu(
                            expanded = buildingDropdownExpanded,
                            onDismissRequest = { buildingDropdownExpanded = false }
                        ) {
                            buildingList.forEach { building ->
                                DropdownMenuItem(
                                    text = { Text(building.name) },
                                    onClick = {
                                        buildingId = building.id
                                        buildingDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                // تاريخ الإضافة (منتقي التاريخ)
                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    OutlinedTextField(
                        value = "${dateFormatter.format(Date(selectedDate))} (${startMonth})",
                        onValueChange = {},
                        label = { Text("تاريخ الإضافة") },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                        trailingIcon = {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                    )
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الجوال") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    OutlinedTextField(
                        value = packageType,
                        onValueChange = {},
                        label = { Text("الباقة") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { packageExpanded = true },
                        trailingIcon = {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                    )
                    DropdownMenu(
                        expanded = packageExpanded,
                        onDismissRequest = { packageExpanded = false }
                    ) {
                        packageOptions.forEach {
                            DropdownMenuItem(
                                text = { Text(it) },
                                onClick = {
                                    packageType = it
                                    packageExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 2
                )
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
                        buildingId,
                        startMonth,
                        phone,
                        address,
                        packageType,
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
                    selectedDate = dateMillis
                    startMonth = monthFormatter.format(Date(dateMillis))
                }
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}
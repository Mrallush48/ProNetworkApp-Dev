package com.pronetwork.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pronetwork.app.data.Building
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientEditDialog(
    buildingList: List<Building>,
    initialName: String = "",
    initialSubscriptionNumber: String = "",
    initialPrice: String = "",
    initialBuildingId: Int = 0,
    initialStartMonth: String = "",
    initialStartDay: Int = 1,
    initialFirstMonthAmount: String = "",
    initialPhone: String = "",
    initialAddress: String = "",
    initialPackageType: String = "5Mbps",
    initialNotes: String = "",
    buildingSelectionEnabled: Boolean = true,
    onSave: (
        name: String,
        subscriptionNumber: String,
        price: Double,
        buildingId: Int,
        startMonth: String,
        startDay: Int,
        firstMonthAmount: Double,
        phone: String,
        address: String,
        packageType: String,
        notes: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var subscriptionNumber by remember { mutableStateOf(initialSubscriptionNumber) }
    var priceText by remember { mutableStateOf(initialPrice) }
    var selectedBuildingId by remember {
        mutableStateOf(
            initialBuildingId.takeIf { it > 0 } ?: buildingList.firstOrNull()?.id ?: 0
        )
    }
    var startMonth by remember { mutableStateOf(initialStartMonth) }
    var startDay by remember { mutableStateOf(initialStartDay.toString()) }
    var firstMonthAmountText by remember { mutableStateOf(initialFirstMonthAmount) }
    var phone by remember { mutableStateOf(initialPhone) }
    var address by remember { mutableStateOf(initialAddress) }
    var packageType by remember { mutableStateOf(initialPackageType) }
    var notes by remember { mutableStateOf(initialNotes) }

    var buildingDropdownExpanded by remember { mutableStateOf(false) }
    var packageDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // حسبة المبلغ الجزئي للشهر الأول على أساس 30 يوم دائماً
    LaunchedEffect(priceText, startMonth, startDay) {
        if (priceText.isNotEmpty() && startMonth.isNotEmpty() && startDay.isNotEmpty()) {
            val priceValue = priceText.toDoubleOrNull()
            val dayValue = startDay.toIntOrNull()

            // اليوم من 1 إلى 30 فقط لأننا نعتبر الشهر 30 يوماً دائماً
            if (priceValue != null && dayValue != null && dayValue in 1..30) {
                val daysInMonth = 30

                val remainingDays = daysInMonth - dayValue + 1
                val partialAmount = (priceValue / daysInMonth) * remainingDays

                firstMonthAmountText = String.format("%.2f", partialAmount)
            }
        }
    }

    val packageOptions = listOf("5Mbps", "10Mbps", "20Mbps", "50Mbps", "100Mbps", "أخرى")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialName.isEmpty()) "إضافة عميل جديد" else "تعديل العميل",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("الاسم *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = subscriptionNumber,
                    onValueChange = { subscriptionNumber = it },
                    label = { Text("رقم الاشتراك *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("السعر الشهري الكامل (ريال) *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = packageType,
                        onValueChange = {},
                        label = { Text("الباقة") },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { packageDropdownExpanded = true },
                        trailingIcon = {
                            IconButton(onClick = { packageDropdownExpanded = !packageDropdownExpanded }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenu(
                        expanded = packageDropdownExpanded,
                        onDismissRequest = { packageDropdownExpanded = false }
                    ) {
                        packageOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    packageType = option
                                    packageDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "📅 تاريخ بداية الاشتراك",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startMonth,
                        onValueChange = { startMonth = it },
                        label = { Text("الشهر (yyyy-MM) *") },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("2026-01") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = "اختر التاريخ")
                            }
                        }
                    )

                    OutlinedTextField(
                        value = startDay,
                        onValueChange = {
                            if (it.isEmpty() || (it.toIntOrNull() ?: 0) in 1..30) {
                                startDay = it
                            }
                        },
                        label = { Text("اليوم *") },
                        modifier = Modifier.weight(0.5f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("1-30") },
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = firstMonthAmountText,
                    onValueChange = { firstMonthAmountText = it },
                    label = { Text("المبلغ الفعلي للشهر الأول (ريال) *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    supportingText = {
                        val dayValue = startDay.toIntOrNull()
                        if (dayValue != null && dayValue in 1..30) {
                            val daysInMonth = 30
                            val remainingDays = daysInMonth - dayValue + 1
                            Text(
                                "💡 الأيام المتبقية: $remainingDays من $daysInMonth يوم",
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (buildingSelectionEnabled && buildingList.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = buildingList.firstOrNull { it.id == selectedBuildingId }?.name ?: "",
                            onValueChange = {},
                            label = { Text("المبنى *") },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { buildingDropdownExpanded = true },
                            trailingIcon = {
                                IconButton(onClick = { buildingDropdownExpanded = !buildingDropdownExpanded }) {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                }
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
                                        selectedBuildingId = building.id
                                        buildingDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("رقم الجوال") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("العنوان") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank() &&
                                subscriptionNumber.isNotBlank() &&
                                priceText.isNotBlank() &&
                                startMonth.isNotBlank() &&
                                startDay.isNotBlank() &&
                                firstMonthAmountText.isNotBlank()
                            ) {

                                val priceValue = priceText.toDoubleOrNull()
                                val dayValue = startDay.toIntOrNull()
                                val firstMonthAmountValue = firstMonthAmountText.toDoubleOrNull()

                                if (priceValue != null &&
                                    dayValue != null &&
                                    firstMonthAmountValue != null &&
                                    dayValue in 1..30 &&
                                    priceValue > 0 &&
                                    firstMonthAmountValue > 0
                                ) {
                                    onSave(
                                        name.trim(),
                                        subscriptionNumber.trim(),
                                        priceValue,
                                        selectedBuildingId,
                                        startMonth,
                                        dayValue,
                                        firstMonthAmountValue,
                                        phone.trim(),
                                        address.trim(),
                                        packageType,
                                        notes.trim()
                                    )
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = name.isNotBlank() &&
                                subscriptionNumber.isNotBlank() &&
                                priceText.isNotBlank() &&
                                startMonth.isNotBlank() &&
                                startDay.isNotBlank() &&
                                firstMonthAmountText.isNotBlank()
                    ) {
                        Text("حفظ")
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            currentMonth = startMonth,
            onDateSelected = { selectedMonth ->
                startMonth = selectedMonth
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

@Composable
fun DatePickerDialog(
    currentMonth: String,
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = Calendar.getInstance()

    val monthParts = currentMonth.split("-")
    if (monthParts.size == 2) {
        val year = monthParts[0].toIntOrNull()
        val month = monthParts[1].toIntOrNull()
        if (year != null && month != null) {
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month - 1)
        }
    }

    var selectedYear by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableStateOf(calendar.get(Calendar.MONTH) + 1) }

    var showYearPicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    "اختر الشهر والسنة",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showYearPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedYear.toString())
                        }
                        DropdownMenu(
                            expanded = showYearPicker,
                            onDismissRequest = { showYearPicker = false }
                        ) {
                            (2020..2030).forEach { year ->
                                DropdownMenuItem(
                                    text = { Text(year.toString()) },
                                    onClick = {
                                        selectedYear = year
                                        showYearPicker = false
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { showMonthPicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(String.format("%02d", selectedMonth))
                        }
                        DropdownMenu(
                            expanded = showMonthPicker,
                            onDismissRequest = { showMonthPicker = false }
                        ) {
                            (1..12).forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(String.format("%02d", month)) },
                                    onClick = {
                                        selectedMonth = month
                                        showMonthPicker = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }

                    Button(
                        onClick = {
                            val formattedMonth = String.format("%04d-%02d", selectedYear, selectedMonth)
                            onDateSelected(formattedMonth)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("تأكيد")
                    }
                }
            }
        }
    }
}
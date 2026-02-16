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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pronetwork.app.R
import com.pronetwork.app.data.Building
import java.util.*

// دالة مساعدة لتثبيت lineHeight في الـ label
@Composable
private fun FixedLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(
            lineHeight = 16.sp  // تثبيت الارتفاع الرأسي لليبل
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientEditDialog(
    buildingList: List<Building>,
    initialName: String = "",
    initialSubscriptionNumber: String = "",
    initialPrice: String = "",
    initialBuildingId: Int = 0,
    initialRoomNumber: String = "",
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
        roomNumber: String,
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

    // المبنى nullable عشان نميّز بين "لم يُختر بعد" وقيمة حقيقية
    var selectedBuildingId by remember {
        mutableStateOf<Int?>(
            if (!buildingSelectionEnabled && initialBuildingId > 0) {
                initialBuildingId
            } else {
                initialBuildingId.takeIf { it > 0 }
            }
        )
    }
    var roomNumber by remember { mutableStateOf(initialRoomNumber) }
    var startMonth by remember { mutableStateOf(initialStartMonth) }
    var startDay by remember { mutableStateOf(initialStartDay.toString()) }
    var firstMonthAmountText by remember { mutableStateOf(initialFirstMonthAmount) }
    var phone by remember { mutableStateOf(initialPhone) }
    var address by remember { mutableStateOf(initialAddress) }

    // الباقة nullable في وضع الإضافة
    var packageType by remember {
        mutableStateOf<String?>(
            if (initialName.isNotEmpty()) initialPackageType else null
        )
    }

    var notes by remember { mutableStateOf(initialNotes) }

    // نصوص الأخطاء كمتحولات عادية (تُقرأ مرة واحدة في السياق المركّب)
    val nameErrorText = stringResource(R.string.client_edit_name_error)
    val subscriptionErrorText = stringResource(R.string.client_edit_subscription_error)
    val priceErrorText = stringResource(R.string.client_edit_price_error)
    val startMonthErrorText = stringResource(R.string.client_edit_start_month_error)
    val startDayErrorText = stringResource(R.string.client_edit_start_day_error)
    val firstMonthAmountErrorText =
        stringResource(R.string.client_edit_first_month_amount_error)
    val buildingErrorText = stringResource(R.string.client_edit_building_error)
    val packageErrorText = stringResource(R.string.client_edit_package_error)

    // متغيرات الأخطاء لكل الحقول الإلزامية
    var nameError by remember { mutableStateOf<String?>(null) }
    var subscriptionError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }
    var startMonthError by remember { mutableStateOf<String?>(null) }
    var startDayError by remember { mutableStateOf<String?>(null) }
    var firstMonthAmountError by remember { mutableStateOf<String?>(null) }
    var buildingError by remember { mutableStateOf<String?>(null) }
    var packageError by remember { mutableStateOf<String?>(null) }
    val roomErrorText = stringResource(R.string.client_edit_room_error)
    var roomError by remember { mutableStateOf<String?>(null) }

    var buildingDropdownExpanded by remember { mutableStateOf(false) }
    var packageDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // حسبة المبلغ الجزئي للشهر الأول على أساس 30 يوم دائماً
    LaunchedEffect(priceText, startMonth, startDay) {
        if (priceText.isNotEmpty() && startMonth.isNotEmpty() && startDay.isNotEmpty()) {
            val priceValue = priceText.toDoubleOrNull()
            val dayValue = startDay.toIntOrNull()

            if (priceValue != null && dayValue != null && dayValue in 1..30) {
                val daysInMonth = 30
                val remainingDays = daysInMonth - dayValue + 1
                val partialAmount = (priceValue / daysInMonth) * remainingDays
                firstMonthAmountText =
                    String.format(Locale.getDefault(), "%.2f", partialAmount)
            }
        }
    }

    val packageOptions = listOf("5Mbps", "7Mbps", "10Mbps", "15Mbps", "25Mbps", "30Mbps", "Other")

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
                    text = if (initialName.isEmpty())
                        stringResource(R.string.client_edit_title_add)
                    else
                        stringResource(R.string.client_edit_title_edit),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(20.dp))

                ClientInfoSection(
                    name = name,
                    onNameChange = {
                        name = it
                        if (nameError != null && it.isNotBlank()) nameError = null
                    },
                    nameError = nameError,
                    subscriptionNumber = subscriptionNumber,
                    onSubscriptionChange = {
                        subscriptionNumber = it
                        if (subscriptionError != null && it.isNotBlank()) subscriptionError = null
                    },
                    subscriptionError = subscriptionError,
                    priceText = priceText,
                    onPriceChange = {
                        priceText = it
                        if (priceError != null && it.isNotBlank()) priceError = null
                    },
                    priceError = priceError,
                    packageType = packageType,
                    packageError = packageError,
                    packageOptions = packageOptions,
                    packageDropdownExpanded = packageDropdownExpanded,
                    onPackageDropdownExpandedChange = { packageDropdownExpanded = it },
                    onPackageSelected = { option ->
                        packageType = option
                        packageError = null
                        packageDropdownExpanded = false
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                SubscriptionStartSection(
                    startMonth = startMonth,
                    onStartMonthChange = {
                        startMonth = it
                        if (startMonthError != null && it.isNotBlank()) startMonthError = null
                    },
                    startMonthError = startMonthError,
                    startDay = startDay,
                    onStartDayChange = {
                        if (it.isEmpty() || (it.toIntOrNull() ?: 0) in 1..30) {
                            startDay = it
                            if (startDayError != null && it.isNotBlank()) startDayError = null
                        }
                    },
                    startDayError = startDayError,
                    firstMonthAmountText = firstMonthAmountText,
                    onFirstMonthAmountChange = {
                        firstMonthAmountText = it
                        if (firstMonthAmountError != null && it.isNotBlank()) {
                            firstMonthAmountError = null
                        }
                    },
                    firstMonthAmountError = firstMonthAmountError,
                    onShowDatePicker = { showDatePicker = true }
                )

                Spacer(modifier = Modifier.height(12.dp))

                BuildingAndContactSection(
                    buildingSelectionEnabled = buildingSelectionEnabled,
                    buildingList = buildingList,
                    selectedBuildingId = selectedBuildingId,
                    buildingError = buildingError,
                    buildingDropdownExpanded = buildingDropdownExpanded,
                    onBuildingDropdownExpandedChange = { buildingDropdownExpanded = it },
                    onBuildingSelected = { id ->
                        selectedBuildingId = id
                        buildingError = null
                        buildingDropdownExpanded = false
                    },
                    roomNumber = roomNumber,
                    onRoomNumberChange = {
                        roomNumber = it; if (roomError != null && it.isNotBlank()) roomError = null
                    },
                    roomError = roomError,
                    phone = phone,
                    onPhoneChange = { phone = it },
                    address = address,
                    onAddressChange = { address = it },
                    notes = notes,
                    onNotesChange = { notes = it }
                )


                Spacer(modifier = Modifier.height(20.dp))

                // حساب صلاحية الحقول
                val basicFieldsValid =
                    name.isNotBlank() &&
                            subscriptionNumber.isNotBlank() &&
                            priceText.isNotBlank() &&
                            startMonth.isNotBlank() &&
                            startDay.isNotBlank() &&
                            firstMonthAmountText.isNotBlank()

                val buildingValid = selectedBuildingId != null
                val packageValid = !packageType.isNullOrBlank()

                val roomValid = roomNumber.isNotBlank()
                val canSave = basicFieldsValid && buildingValid && packageValid && roomValid

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.client_edit_cancel_button))
                    }

                    Button(
                        onClick = {
                            // 1) تصفير الأخطاء
                            nameError = null
                            subscriptionError = null
                            priceError = null
                            startMonthError = null
                            startDayError = null
                            firstMonthAmountError = null
                            buildingError = null
                            packageError = null

                            // 2) تعبئة الأخطاء حسب الحقول
                            if (name.isBlank()) {
                                nameError = nameErrorText
                            }
                            if (subscriptionNumber.isBlank()) {
                                subscriptionError = subscriptionErrorText
                            }
                            if (priceText.isBlank()) {
                                priceError = priceErrorText
                            }
                            if (startMonth.isBlank()) {
                                startMonthError = startMonthErrorText
                            }
                            if (startDay.isBlank()) {
                                startDayError = startDayErrorText
                            }
                            if (firstMonthAmountText.isBlank()) {
                                firstMonthAmountError = firstMonthAmountErrorText
                            }
                            if (selectedBuildingId == null) {
                                buildingError = buildingErrorText
                            }
                            if (packageType.isNullOrBlank()) {
                                packageError = packageErrorText
                            }

                            if (roomNumber.isBlank()) {
                                roomError = roomErrorText
                            }

                            // 3) لو في حقل ناقص، نوقف هنا (مع عرض الرسائل فقط)
                            if (!canSave) return@Button

                            // 4) التحقق العددي ثم onSave
                            val priceValue = priceText.toDoubleOrNull()
                            val dayValue = startDay.toIntOrNull()
                            val firstMonthAmountValue = firstMonthAmountText.toDoubleOrNull()

                            if (priceValue != null &&
                                dayValue != null &&
                                firstMonthAmountValue != null &&
                                dayValue in 1..30 &&
                                priceValue > 0 &&
                                firstMonthAmountValue > 0 &&
                                selectedBuildingId != null &&
                                !packageType.isNullOrBlank()
                            ) {
                                onSave(
                                    name.trim(),
                                    subscriptionNumber.trim(),
                                    priceValue,
                                    selectedBuildingId!!,
                                    roomNumber.trim(),
                                    startMonth,
                                    dayValue,
                                    firstMonthAmountValue,
                                    phone.trim(),
                                    address.trim(),
                                    packageType!!.trim(),
                                    notes.trim()
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        // يبقى الزر دائماً مفعّل ليشغّل منطق الأخطاء
                        enabled = true
                    ) {
                        Text(stringResource(R.string.client_edit_save_button))
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
private fun ClientInfoSection(
    name: String,
    onNameChange: (String) -> Unit,
    nameError: String?,
    subscriptionNumber: String,
    onSubscriptionChange: (String) -> Unit,
    subscriptionError: String?,
    priceText: String,
    onPriceChange: (String) -> Unit,
    priceError: String?,
    packageType: String?,
    packageError: String?,
    packageOptions: List<String>,
    packageDropdownExpanded: Boolean,
    onPackageDropdownExpandedChange: (Boolean) -> Unit,
    onPackageSelected: (String) -> Unit
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { FixedLabel(stringResource(R.string.client_edit_name_label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = nameError != null,
        supportingText = {
            nameError?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = subscriptionNumber,
        onValueChange = onSubscriptionChange,
        label = {
            FixedLabel(
                stringResource(R.string.client_edit_subscription_label)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = subscriptionError != null,
        supportingText = {
            subscriptionError?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = priceText,
        onValueChange = onPriceChange,
        label = {
            FixedLabel(
                stringResource(R.string.client_edit_price_label)
            )
        },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        isError = priceError != null,
        supportingText = {
            priceError?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    )

    Spacer(modifier = Modifier.height(12.dp))

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = packageType ?: "",
            onValueChange = {},
            label = {
                FixedLabel(
                    stringResource(R.string.client_edit_package_label)
                )
            },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPackageDropdownExpandedChange(true) },
            trailingIcon = {
                IconButton(
                    onClick = {
                        onPackageDropdownExpandedChange(!packageDropdownExpanded)
                    }
                ) {
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = null
                    )
                }
            },
            isError = packageError != null,
            supportingText = {
                packageError?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )
        DropdownMenu(
            expanded = packageDropdownExpanded,
            onDismissRequest = { onPackageDropdownExpandedChange(false) }
        ) {
            packageOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onPackageSelected(option) }
                )
            }
        }
    }
}

@Composable
private fun SubscriptionStartSection(
    startMonth: String,
    onStartMonthChange: (String) -> Unit,
    startMonthError: String?,
    startDay: String,
    onStartDayChange: (String) -> Unit,
    startDayError: String?,
    firstMonthAmountText: String,
    onFirstMonthAmountChange: (String) -> Unit,
    firstMonthAmountError: String?,
    onShowDatePicker: () -> Unit
) {
    Text(
        text = stringResource(R.string.client_edit_start_section_title),
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
            onValueChange = onStartMonthChange,
            label = {
                FixedLabel(
                    stringResource(
                        R.string.client_edit_start_month_label
                    )
                )
            },
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    stringResource(
                        R.string.client_edit_start_month_placeholder
                    )
                )
            },
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = onShowDatePicker) {
                    Icon(
                        Icons.Filled.CalendarToday,
                        contentDescription = stringResource(
                            R.string.client_edit_start_month_icon_cd
                        )
                    )
                }
            },
            isError = startMonthError != null,
            supportingText = {
                startMonthError?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )

        OutlinedTextField(
            value = startDay,
            onValueChange = onStartDayChange,
            label = {
                FixedLabel(
                    stringResource(
                        R.string.client_edit_start_day_label
                    )
                )
            },
            modifier = Modifier.weight(0.5f),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            placeholder = {
                Text(
                    stringResource(
                        R.string.client_edit_start_day_placeholder
                    )
                )
            },
            singleLine = true,
            isError = startDayError != null,
            supportingText = {
                startDayError?.let { msg ->
                    Text(
                        text = msg,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = firstMonthAmountText,
        onValueChange = onFirstMonthAmountChange,
        label = {
            FixedLabel(
                stringResource(
                    R.string.client_edit_first_month_amount_label
                )
            )
        },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        isError = firstMonthAmountError != null,
        supportingText = {
            if (firstMonthAmountError != null) {
                Text(
                    text = firstMonthAmountError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                val dayValue = startDay.toIntOrNull()
                if (dayValue != null && dayValue in 1..30) {
                    val daysInMonth = 30
                    val remainingDays = daysInMonth - dayValue + 1
                    Text(
                        text = stringResource(
                            R.string.client_edit_first_month_hint,
                            remainingDays,
                            daysInMonth
                        ),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    )
}

@Composable
private fun BuildingAndContactSection(
    buildingSelectionEnabled: Boolean,
    buildingList: List<Building>,
    selectedBuildingId: Int?,
    buildingError: String?,
    buildingDropdownExpanded: Boolean,
    onBuildingDropdownExpandedChange: (Boolean) -> Unit,
    onBuildingSelected: (Int) -> Unit,
    roomNumber: String,
    onRoomNumberChange: (String) -> Unit,
    roomError: String? = null,
    phone: String,
    onPhoneChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit
) {
    if (buildingSelectionEnabled && buildingList.isNotEmpty()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = buildingList.firstOrNull { it.id == selectedBuildingId }?.name
                    ?: "",
                onValueChange = {},
                label = {
                    FixedLabel(
                        stringResource(
                            R.string.client_edit_building_label
                        )
                    )
                },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBuildingDropdownExpandedChange(true) },
                trailingIcon = {
                    IconButton(onClick = {
                        onBuildingDropdownExpandedChange(!buildingDropdownExpanded)
                    }) {
                        Icon(
                            Icons.Filled.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                },
                isError = buildingError != null,
                supportingText = {
                    buildingError?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            )
            DropdownMenu(
                expanded = buildingDropdownExpanded,
                onDismissRequest = { onBuildingDropdownExpandedChange(false) }
            ) {
                buildingList.forEach { building ->
                    DropdownMenuItem(
                        text = { Text(building.name) },
                        onClick = { onBuildingSelected(building.id) }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    OutlinedTextField(
        value = roomNumber,
        onValueChange = onRoomNumberChange,
        label = { FixedLabel(stringResource(R.string.client_edit_room_number_label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = roomError != null,
        supportingText = {
            roomError?.let { msg ->
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = phone,
        onValueChange = onPhoneChange,
        label = { FixedLabel(stringResource(R.string.client_edit_phone_label)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = address,
        onValueChange = onAddressChange,
        label = { FixedLabel(stringResource(R.string.client_edit_address_label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = false,
        maxLines = 2
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = { FixedLabel(stringResource(R.string.client_edit_notes_label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = false,
        maxLines = 3
    )
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
                    text = stringResource(R.string.month_picker_title),
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
                        Text(stringResource(R.string.month_picker_cancel))
                    }

                    Button(
                        onClick = {
                            val formattedMonth =
                                String.format("%04d-%02d", selectedYear, selectedMonth)
                            onDateSelected(formattedMonth)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.month_picker_confirm))
                    }
                }
            }
        }
    }
}

package com.pronetwork.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pronetwork.app.R
import com.pronetwork.app.data.Building
import com.pronetwork.app.data.Client
import androidx.compose.foundation.text.KeyboardOptions


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
    var priceText by remember { mutableStateOf(client?.price?.toString() ?: "") }
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
    var selectedPackage by remember {
        mutableStateOf(client?.packageType ?: packageOptions.first())
    }
    var packageExpanded by remember { mutableStateOf(false) }

    // حالة تاريخ البداية (قائمة بسيطة – لاحقًا ممكن تستبدل بـ DatePicker حقيقي)
    val dateOptions = listOf(
        stringResource(R.string.client_edit_date_jan_1),
        stringResource(R.string.client_edit_date_feb_1),
        stringResource(R.string.client_edit_date_mar_1),
        stringResource(R.string.client_edit_date_apr_1),
        stringResource(R.string.client_edit_date_may_1),
        stringResource(R.string.client_edit_date_jun_1),
        stringResource(R.string.client_edit_date_jul_1),
        stringResource(R.string.client_edit_date_aug_1),
        stringResource(R.string.client_edit_date_sep_1),
        stringResource(R.string.client_edit_date_oct_1),
        stringResource(R.string.client_edit_date_nov_1),
        stringResource(R.string.client_edit_date_dec_1),
    )
    var selectedDate by remember { mutableStateOf(client?.startMonth ?: dateOptions.first()) }
    var dateExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (client == null)
                            stringResource(R.string.client_edit_screen_title_add)
                        else
                            stringResource(R.string.client_edit_screen_title_edit)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
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
            ClientEditBasicFieldsSection(
                name = name,
                onNameChange = { name = it },
                subscriptionNumber = subscriptionNumber,
                onSubscriptionChange = { subscriptionNumber = it },
                priceText = priceText,
                onPriceChange = { priceText = it },
            )

            Spacer(Modifier.height(8.dp))

            ClientEditDropdownsSection(
                buildingList = buildingList,
                selectedBuilding = selectedBuilding,
                onBuildingSelected = { selectedBuilding = it },
                buildingExpanded = buildingExpanded,
                onBuildingExpandedChange = { buildingExpanded = it },
                packageOptions = packageOptions,
                selectedPackage = selectedPackage,
                onPackageSelected = { selectedPackage = it },
                packageExpanded = packageExpanded,
                onPackageExpandedChange = { packageExpanded = it },
                dateOptions = dateOptions,
                selectedDate = selectedDate,
                onDateSelected = { selectedDate = it },
                dateExpanded = dateExpanded,
                onDateExpandedChange = { dateExpanded = it }
            )

            Spacer(Modifier.height(8.dp))

            ClientEditContactSection(
                phone = phone,
                onPhoneChange = { phone = it },
                address = address,
                onAddressChange = { address = it },
                notes = notes,
                onNotesChange = { notes = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val parsedPrice = priceText.toDoubleOrNull() ?: 0.0

                    val updatedClient = if (client == null) {
                        Client(
                            name = name,
                            subscriptionNumber = subscriptionNumber,
                            price = parsedPrice,
                            buildingId = selectedBuilding?.id ?: 0,
                            startMonth = selectedDate,
                            phone = phone,
                            address = address,
                            packageType = selectedPackage,
                            notes = notes
                        )
                    } else {
                        client.copy(
                            name = name,
                            subscriptionNumber = subscriptionNumber,
                            price = parsedPrice,
                            buildingId = selectedBuilding?.id ?: client.buildingId,
                            startMonth = selectedDate,
                            phone = phone,
                            address = address,
                            packageType = selectedPackage,
                            notes = notes
                        )
                    }

                    onSave(updatedClient)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = stringResource(R.string.client_edit_screen_save_button))
            }
        }
    }
}

@Composable
private fun ClientEditBasicFieldsSection(
    name: String,
    onNameChange: (String) -> Unit,
    subscriptionNumber: String,
    onSubscriptionChange: (String) -> Unit,
    priceText: String,
    onPriceChange: (String) -> Unit
) {
    OutlinedTextField(
        value = name,
        onValueChange = onNameChange,
        label = { Text(stringResource(R.string.client_edit_name_label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = subscriptionNumber,
        onValueChange = onSubscriptionChange,
        label = { Text(stringResource(R.string.client_edit_subscription_label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = priceText,
        onValueChange = onPriceChange,
        label = { Text(stringResource(R.string.client_edit_price_label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientEditDropdownsSection(
    buildingList: List<Building>,
    selectedBuilding: Building?,
    onBuildingSelected: (Building) -> Unit,
    buildingExpanded: Boolean,
    onBuildingExpandedChange: (Boolean) -> Unit,
    packageOptions: List<String>,
    selectedPackage: String,
    onPackageSelected: (String) -> Unit,
    packageExpanded: Boolean,
    onPackageExpandedChange: (Boolean) -> Unit,
    dateOptions: List<String>,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    dateExpanded: Boolean,
    onDateExpandedChange: (Boolean) -> Unit
) {
    // المبنى
    ExposedDropdownMenuBox(
        expanded = buildingExpanded,
        onExpandedChange = { onBuildingExpandedChange(!buildingExpanded) },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedBuilding?.name ?: stringResource(R.string.client_edit_building_placeholder),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.client_edit_building_label)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = buildingExpanded)
            },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = buildingExpanded,
            onDismissRequest = { onBuildingExpandedChange(false) }
        ) {
            LazyColumn {
                items(buildingList) { building ->
                    DropdownMenuItem(
                        text = { Text(building.name) },
                        onClick = {
                            onBuildingSelected(building)
                            onBuildingExpandedChange(false)
                        }
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    // الباقة
    ExposedDropdownMenuBox(
        expanded = packageExpanded,
        onExpandedChange = { onPackageExpandedChange(!packageExpanded) },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedPackage,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.client_edit_package_label)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = packageExpanded)
            },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = packageExpanded,
            onDismissRequest = { onPackageExpandedChange(false) }
        ) {
            packageOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onPackageSelected(option)
                        onPackageExpandedChange(false)
                    }
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    // تاريخ البداية
    ExposedDropdownMenuBox(
        expanded = dateExpanded,
        onExpandedChange = { onDateExpandedChange(!dateExpanded) },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedDate,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.client_edit_start_month_label)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateExpanded)
            },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = dateExpanded,
            onDismissRequest = { onDateExpandedChange(false) }
        ) {
            dateOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onDateSelected(option)
                        onDateExpandedChange(false)
                    }
                )
            }
        }
    }
}

@Composable
private fun ClientEditContactSection(
    phone: String,
    onPhoneChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit
) {
    OutlinedTextField(
        value = phone,
        onValueChange = onPhoneChange,
        label = { Text(stringResource(R.string.client_edit_phone_label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
        )
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = address,
        onValueChange = onAddressChange,
        label = { Text(stringResource(R.string.client_edit_address_label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = notes,
        onValueChange = onNotesChange,
        label = { Text(stringResource(R.string.client_edit_notes_label)) },
        modifier = Modifier.fillMaxWidth()
    )
}

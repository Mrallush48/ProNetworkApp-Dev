package com.pronetwork.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pronetwork.app.data.Building
import com.pronetwork.app.data.Client

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingDetailsScreen(
    building: Building,
    allClients: List<Client>,
    monthOptions: List<String>,
    onAddClient: (Client) -> Unit,
    onEditClient: (Client) -> Unit,
    onDeleteClient: (Client) -> Unit,
    onTogglePaid: (Client, Boolean) -> Unit,
    onUndoPaid: (Client) -> Unit,
    onBack: () -> Unit
) {
    var showAddClientDialog by remember { mutableStateOf(false) }
    var selectedClient by remember { mutableStateOf<Client?>(null) }
    var showEditClientDialog by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf(monthOptions.first()) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }

    val buildingClients = allClients.filter { it.buildingId == building.id && it.startMonth == selectedMonth }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(building.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddClientDialog = true }) {
                Icon(Icons.Filled.Person, contentDescription = "إضافة عميل")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("الموقع: ${building.location}", style = MaterialTheme.typography.titleMedium)
            Text("ملاحظات: ${building.notes}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))
            // قائمة منسدلة للشهر
            Box(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                OutlinedTextField(
                    value = selectedMonth,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("تصفح العملاء حسب الشهر") },
                    trailingIcon = {
                        IconButton(onClick = { monthDropdownExpanded = !monthDropdownExpanded }) {
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { monthDropdownExpanded = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                DropdownMenu(
                    expanded = monthDropdownExpanded,
                    onDismissRequest = { monthDropdownExpanded = false }
                ) {
                    monthOptions.forEach { month ->
                        DropdownMenuItem(
                            text = { Text(month) },
                            onClick = {
                                selectedMonth = month
                                monthDropdownExpanded = false
                            }
                        )
                    }
                }
            }
            Text("عملاء المبنى في $selectedMonth:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (buildingClients.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("لا يوجد عملاء في هذا الشهر.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                buildingClients.forEach { client ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { selectedClient = client },
                        elevation = CardDefaults.cardElevation(5.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (client.isPaid) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("اسم: ${client.name}", style = MaterialTheme.typography.titleMedium)
                                Text("رقم الاشتراك: ${client.subscriptionNumber}", style = MaterialTheme.typography.bodySmall)
                                Text("رقم الجوال: ${client.phone}", style = MaterialTheme.typography.bodySmall)
                                Text("الحالة: ${if (client.isPaid) "مدفوع" else "غير مدفوع"}", style = MaterialTheme.typography.bodySmall)
                            }
                            if (!client.isPaid) {
                                Button(
                                    onClick = { onTogglePaid(client, true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("تأكيد الدفع", color = MaterialTheme.colorScheme.onTertiary)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { onUndoPaid(client) }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("تراجع", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
            if (showAddClientDialog) {
                ClientEditDialog(
                    buildingList = listOf(building),
                    initialBuildingId = building.id,
                    initialStartMonth = selectedMonth,
                    buildingSelectionEnabled = false,
                    onSave = { name, subscriptionNumber, price, buildingId, startMonth, phone, address, packageType, notes ->
                        onAddClient(
                            Client(
                                name = name,
                                subscriptionNumber = subscriptionNumber,
                                price = price,
                                buildingId = buildingId,
                                startMonth = startMonth,
                                phone = phone,
                                address = address,
                                packageType = packageType,
                                notes = notes
                            )
                        )
                        showAddClientDialog = false
                    },
                    onDismiss = { showAddClientDialog = false }
                )
            }
            selectedClient?.let { client ->
                ClientDetailsScreen(
                    client = client,
                    buildingName = building.name,
                    onEdit = {
                        showEditClientDialog = true
                    },
                    onDelete = {
                        onDeleteClient(client)
                        selectedClient = null
                    },
                    onTogglePaid = { paid ->
                        onTogglePaid(client, paid)
                    },
                    onUndoPaid = {
                        onUndoPaid(client)
                    },
                    onBack = { selectedClient = null }
                )
                if (showEditClientDialog) {
                    ClientEditDialog(
                        buildingList = listOf(building),
                        initialName = client.name,
                        initialSubscriptionNumber = client.subscriptionNumber,
                        initialPrice = client.price.toString(),
                        initialBuildingId = building.id,
                        initialStartMonth = client.startMonth,
                        initialPhone = client.phone,
                        initialAddress = client.address,
                        initialPackageType = client.packageType,
                        initialNotes = client.notes,
                        buildingSelectionEnabled = false,
                        onSave = { name, subscriptionNumber, price, buildingId, startMonth, phone, address, packageType, notes ->
                            onEditClient(
                                client.copy(
                                    name = name,
                                    subscriptionNumber = subscriptionNumber,
                                    price = price,
                                    buildingId = buildingId,
                                    startMonth = startMonth,
                                    phone = phone,
                                    address = address,
                                    packageType = packageType,
                                    notes = notes
                                )
                            )
                            showEditClientDialog = false
                            selectedClient = null
                        },
                        onDismiss = { showEditClientDialog = false }
                    )
                }
            }
        }
    }
}
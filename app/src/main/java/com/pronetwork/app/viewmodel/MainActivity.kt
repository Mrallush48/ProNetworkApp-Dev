package com.pronetwork.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.pronetwork.app.data.Client
import com.pronetwork.app.data.Building
import com.pronetwork.app.ui.screens.*
import com.pronetwork.app.viewmodel.ClientViewModel
import com.pronetwork.app.viewmodel.BuildingViewModel

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val clientViewModel: ClientViewModel by viewModels()
    private val buildingViewModel: BuildingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Light color scheme with clear text and backgrounds
        val myLightColors = lightColorScheme(
            primary = Color(0xFF673AB7),
            onPrimary = Color.White,
            background = Color.White,
            onBackground = Color.Black,
            surface = Color.White,
            onSurface = Color.Black,
            secondary = Color(0xFF9575CD),
            onSecondary = Color.Black,
            primaryContainer = Color(0xFFEDE7F6),
            onPrimaryContainer = Color(0xFF311B92),
            secondaryContainer = Color(0xFFD1C4E9),
            onSecondaryContainer = Color(0xFF512DA8)
        )

        setContent {
            MaterialTheme(
                colorScheme = myLightColors,
                typography = MaterialTheme.typography
            ) {
                var currentScreen by remember { mutableStateOf("clients") }
                var showClientDialog by remember { mutableStateOf(false) }
                var showBuildingDialog by remember { mutableStateOf(false) }
                var showEditBuildingDialog by remember { mutableStateOf(false) }
                var selectedBuilding by remember { mutableStateOf<Building?>(null) }
                var selectedClient by remember { mutableStateOf<Client?>(null) }
                var showEditClientDialog by remember { mutableStateOf(false) }

                val clients by clientViewModel.clients.observeAsState(emptyList())
                val buildings by buildingViewModel.buildings.observeAsState(emptyList())

                // Generate months list for dropdown
                val monthsList = remember {
                    val calendar = Calendar.getInstance()
                    val formatter = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                    List(25) {
                        val monthString = formatter.format(calendar.time)
                        calendar.add(Calendar.MONTH, -1)
                        monthString
                    }
                }
                val monthOptions = monthsList
                var selectedMonth by remember { mutableStateOf(monthOptions.first()) }
                var monthDropdownExpanded by remember { mutableStateOf(false) }

                val filteredClients = clients.filter { client ->
                    // Parse months to compare (format: yyyy-MM)
                    try {
                        val clientStartMonth = client.startMonth
                        val currentViewMonth = selectedMonth

                        // Parse both months for comparison
                        val clientDate = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(clientStartMonth)
                        val viewDate = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(currentViewMonth)

                        // Check if client should be visible in this month
                        if (viewDate != null && clientDate != null && viewDate.time >= clientDate.time) {
                            // If client has an end month, check if view month is before end month
                            if (client.endMonth != null) {
                                val endDate = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(client.endMonth)
                                endDate != null && viewDate.time < endDate.time
                            } else {
                                true // No end month, show indefinitely
                            }
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        // Fallback to exact match if parsing fails
                        client.startMonth == selectedMonth
                    }
                }

                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("ProNetwork App", style = MaterialTheme.typography.titleLarge) },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == "clients",
                                onClick = {
                                    currentScreen = "clients"
                                    selectedBuilding = null
                                    selectedClient = null
                                },
                                icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                label = { Text("العملاء") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == "buildings",
                                onClick = {
                                    currentScreen = "buildings"
                                    selectedBuilding = null
                                    selectedClient = null
                                },
                                icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                                label = { Text("المباني") }
                            )
                            NavigationBarItem(
                                selected = currentScreen == "stats",
                                onClick = {
                                    currentScreen = "stats"
                                    selectedBuilding = null
                                    selectedClient = null
                                },
                                icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                                label = { Text("إحصائيات") }
                            )
                        }
                    },
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    }
                ) { padding ->
                    Surface(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when {
                            currentScreen == "clients" && selectedBuilding == null -> {
                                Column(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    // Dropdown للشهور
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
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { monthDropdownExpanded = true },
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

                                    ClientListScreen(
                                        clients = filteredClients,
                                        buildings = buildings,
                                        onAddClient = {
                                            if (buildings.isNotEmpty()) {
                                                showClientDialog = true
                                            } else {
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("يجب إضافة مبنى أولاً قبل إضافة عميل.")
                                                }
                                            }
                                        },
                                        onClientClick = { selectedClient = it },
                                        onUndoPaid = { client ->
                                            clientViewModel.update(client.copy(isPaid = false, paymentDate = null))
                                            scope.launch {
                                                snackbarHostState.showSnackbar("تم التراجع عن تأكيد الدفع")
                                            }
                                        },
                                        onPaid = { client ->
                                            clientViewModel.update(client.copy(isPaid = true, paymentDate = System.currentTimeMillis()))
                                            scope.launch {
                                                val result = snackbarHostState.showSnackbar(
                                                    message = "تم تأكيد الدفع",
                                                    actionLabel = "تراجع"
                                                )
                                                if (result == SnackbarResult.ActionPerformed) {
                                                    clientViewModel.update(client.copy(isPaid = false, paymentDate = null))
                                                }
                                            }
                                        }
                                    )

                                    if (showClientDialog) {
                                        ClientEditDialog(
                                            buildingList = buildings,
                                            initialStartMonth = selectedMonth,
                                            buildingSelectionEnabled = true,
                                            onSave = { name, subscriptionNumber, price, buildingId, startMonth, phone, address, packageType, notes ->
                                                clientViewModel.insert(
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
                                                showClientDialog = false
                                            },
                                            onDismiss = { showClientDialog = false }
                                        )
                                    }
                                    selectedClient?.let { client ->
                                        ClientDetailsScreen(
                                            client = client,
                                            buildingName = buildings.firstOrNull { it.id == client.buildingId }?.name ?: "",
                                            onEdit = { clientToEdit ->
                                                selectedClient = clientToEdit
                                                showEditClientDialog = true
                                            },
                                            onDelete = { clientToDelete ->
                                                // Check if we're deleting from start month or future month
                                                if (selectedMonth == clientToDelete.startMonth) {
                                                    // Deleting from start month - delete completely
                                                    clientViewModel.delete(clientToDelete)
                                                } else {
                                                    // Deleting from future month - set end month to current selected month
                                                    clientViewModel.update(clientToDelete.copy(endMonth = selectedMonth))
                                                }
                                                selectedClient = null
                                            },
                                            onTogglePaid = { paid ->
                                                val updatedClient = if (paid) {
                                                    client.copy(isPaid = paid, paymentDate = System.currentTimeMillis())
                                                } else {
                                                    client.copy(isPaid = paid, paymentDate = null)
                                                }
                                                clientViewModel.update(updatedClient)
                                            },
                                            onUndoPaid = {
                                                clientViewModel.update(client.copy(isPaid = false, paymentDate = null))
                                            },
                                            onBack = { selectedClient = null }
                                        )
                                        if (showEditClientDialog) {
                                            ClientEditDialog(
                                                buildingList = buildings,
                                                initialName = selectedClient!!.name,
                                                initialSubscriptionNumber = selectedClient!!.subscriptionNumber,
                                                initialPrice = selectedClient!!.price.toString(),
                                                initialBuildingId = selectedClient!!.buildingId,
                                                initialStartMonth = selectedClient!!.startMonth,
                                                initialPhone = selectedClient!!.phone,
                                                initialAddress = selectedClient!!.address,
                                                initialPackageType = selectedClient!!.packageType,
                                                initialNotes = selectedClient!!.notes,
                                                buildingSelectionEnabled = true,
                                                onSave = { name, subscriptionNumber, price, buildingId, startMonth, phone, address, packageType, notes ->
                                                    clientViewModel.update(
                                                        selectedClient!!.copy(
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
                            currentScreen == "buildings" && selectedBuilding == null -> {
                                BuildingListScreen(
                                    buildings = buildings,
                                    onAddBuilding = { showBuildingDialog = true },
                                    onBuildingClick = { selectedBuilding = it },
                                    onSearch = { buildingViewModel.setSearchQuery(it) }
                                )
                                if (showBuildingDialog) {
                                    BuildingEditDialog(
                                        onSave = { name, location, notes ->
                                            buildingViewModel.insert(
                                                Building(
                                                    name = name,
                                                    location = location,
                                                    notes = notes
                                                )
                                            )
                                            showBuildingDialog = false
                                        },
                                        onDismiss = { showBuildingDialog = false }
                                    )
                                }
                            }
                            selectedBuilding != null -> {
                                BuildingDetailsScreen(
                                    building = selectedBuilding!!,
                                    allClients = clients,
                                    monthOptions = monthOptions,
                                    onAddClient = { client -> clientViewModel.insert(client) },
                                    onEditClient = { clientToEdit ->
                                        selectedClient = clientToEdit
                                        showEditClientDialog = true
                                    },
                                    onDeleteClient = { clientToDelete ->
                                        clientViewModel.delete(clientToDelete)
                                    },
                                    onTogglePaid = { client, paid ->
                                        clientViewModel.update(client.copy(isPaid = paid))
                                    },
                                    onUndoPaid = { client ->
                                        clientViewModel.update(client.copy(isPaid = false))
                                    },
                                    onEditBuilding = { buildingToEdit ->
                                        selectedBuilding = buildingToEdit
                                        showEditBuildingDialog = true
                                    },
                                    onDeleteBuilding = { buildingToDelete ->
                                        // Delete all clients associated with this building first
                                        clients.filter { it.buildingId == buildingToDelete.id }.forEach { client ->
                                            clientViewModel.delete(client)
                                        }
                                        // Then delete the building
                                        buildingViewModel.delete(buildingToDelete)
                                        selectedBuilding = null
                                    },
                                    onBack = { selectedBuilding = null }
                                )
                                if (showEditClientDialog && selectedClient != null) {
                                    ClientEditDialog(
                                        buildingList = listOf(selectedBuilding!!),
                                        initialName = selectedClient!!.name,
                                        initialSubscriptionNumber = selectedClient!!.subscriptionNumber,
                                        initialPrice = selectedClient!!.price.toString(),
                                        initialBuildingId = selectedBuilding!!.id,
                                        initialStartMonth = selectedClient!!.startMonth,
                                        initialPhone = selectedClient!!.phone,
                                        initialAddress = selectedClient!!.address,
                                        initialPackageType = selectedClient!!.packageType,
                                        initialNotes = selectedClient!!.notes,
                                        buildingSelectionEnabled = false,
                                        onSave = { name, subscriptionNumber, price, buildingId, startMonth, phone, address, packageType, notes ->
                                            clientViewModel.update(
                                                selectedClient!!.copy(
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
                                if (showEditBuildingDialog && selectedBuilding != null) {
                                    BuildingEditDialog(
                                        initialName = selectedBuilding!!.name,
                                        initialLocation = selectedBuilding!!.location,
                                        initialNotes = selectedBuilding!!.notes,
                                        onSave = { name, location, notes ->
                                            buildingViewModel.update(
                                                selectedBuilding!!.copy(
                                                    name = name,
                                                    location = location,
                                                    notes = notes
                                                )
                                            )
                                            showEditBuildingDialog = false
                                        },
                                        onDismiss = { showEditBuildingDialog = false }
                                    )
                                }
                            }
                            currentScreen == "stats" -> {
                                val clientsCount by clientViewModel.clientsCount.observeAsState(0)
                                val paidClientsCount by clientViewModel.paidClientsCount.observeAsState(0)
                                val unpaidClientsCount by clientViewModel.unpaidClientsCount.observeAsState(0)
                                StatisticsScreen(
                                    clientsCount = clientsCount,
                                    buildingsCount = buildings.size,
                                    paidClientsCount = paidClientsCount,
                                    unpaidClientsCount = unpaidClientsCount,
                                    allClients = clients,
                                    monthOptions = monthOptions,
                                    onMarkClientLate = { client, month ->
                                        // Implementation for marking client as late
                                        // For now, this could update the client with a late flag
                                        // or create a separate late client record
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
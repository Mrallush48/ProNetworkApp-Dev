package com.pronetwork.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pronetwork.app.data.Building
import com.pronetwork.app.data.Client
import com.pronetwork.app.ui.screens.*
import com.pronetwork.app.viewmodel.BuildingViewModel
import com.pronetwork.app.viewmodel.ClientViewModel
import com.pronetwork.app.viewmodel.PaymentViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    private val clientViewModel: ClientViewModel by viewModels()
    private val buildingViewModel: BuildingViewModel by viewModels()
    private val paymentViewModel: PaymentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    try {
                        val clientStartMonth = client.startMonth
                        val currentViewMonth = selectedMonth

                        val clientDate =
                            SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(clientStartMonth)
                        val viewDate =
                            SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(currentViewMonth)

                        if (viewDate != null && clientDate != null && viewDate.time >= clientDate.time) {
                            if (client.endMonth != null) {
                                val endDate =
                                    SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(client.endMonth)
                                endDate != null && viewDate.time < endDate.time
                            } else {
                                true
                            }
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        client.startMonth == selectedMonth
                    }
                }

                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                Scaffold(
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    "ProNetwork App",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
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
                                // إذا لا يوجد عميل محدد: نعرض قائمة العملاء
                                if (selectedClient == null) {
                                    Column(
                                        Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = selectedMonth,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("تصفح العملاء حسب الشهر") },
                                                trailingIcon = {
                                                    IconButton(onClick = { monthDropdownExpanded = !monthDropdownExpanded }) {
                                                        Icon(
                                                            Icons.Filled.ArrowDropDown,
                                                            contentDescription = null
                                                        )
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
                                            selectedMonth = selectedMonth,
                                            paymentViewModel = paymentViewModel,
                                            onAddClient = {
                                                if (buildings.isNotEmpty()) {
                                                    showClientDialog = true
                                                } else {
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar("يجب إضافة مبنى أولاً قبل إضافة عميل.")
                                                    }
                                                }
                                            },
                                            onClientClick = { selectedClient = it }
                                        )

                                        if (showClientDialog) {
                                            ClientEditDialog(
                                                buildingList = buildings,
                                                initialStartMonth = selectedMonth,
                                                buildingSelectionEnabled = true,
                                                onSave = { name,
                                                           subscriptionNumber,
                                                           price,
                                                           buildingId,
                                                           startMonth,
                                                           startDay,
                                                           firstMonthAmount,
                                                           phone,
                                                           address,
                                                           packageType,
                                                           notes ->
                                                    scope.launch {
                                                        val newClient = Client(
                                                            name = name,
                                                            subscriptionNumber = subscriptionNumber,
                                                            price = price,
                                                            firstMonthAmount = firstMonthAmount,
                                                            buildingId = buildingId,
                                                            startMonth = startMonth,
                                                            startDay = startDay,
                                                            phone = phone,
                                                            address = address,
                                                            packageType = packageType,
                                                            notes = notes
                                                        )
                                                        clientViewModel.insert(newClient)

                                                        kotlinx.coroutines.delay(300)

                                                        val allClients = clients
                                                        val insertedClient =
                                                            allClients.lastOrNull { it.subscriptionNumber == subscriptionNumber }

                                                        insertedClient?.let { client ->
                                                            paymentViewModel.createPaymentsForClient(
                                                                clientId = client.id,
                                                                startMonth = startMonth,
                                                                endMonth = null,
                                                                amount = price,
                                                                monthOptions = monthOptions
                                                            )
                                                        }

                                                        showClientDialog = false
                                                    }
                                                },
                                                onDismiss = { showClientDialog = false }
                                            )
                                        }
                                    }
                                } else {
                                    // إذا تم اختيار عميل: نعرض شاشة تفاصيل العميل فقط
                                    val client = selectedClient!!
                                    val clientPayments by paymentViewModel
                                        .getClientPayments(client.id)
                                        .observeAsState(emptyList())

                                    val clientMonthUi by paymentViewModel
                                        .getClientMonthPaymentsUi(client.id)
                                        .observeAsState(emptyList())

                                    ClientDetailsScreen(
                                        client = client,
                                        buildingName = buildings.firstOrNull { it.id == client.buildingId }?.name ?: "",
                                        payments = clientPayments,
                                        monthUiList = clientMonthUi,
                                        onEdit = { clientToEdit ->
                                            selectedClient = clientToEdit
                                            showEditClientDialog = true
                                        },
                                        onDelete = { clientToDelete ->
                                            if (selectedMonth == clientToDelete.startMonth) {
                                                clientViewModel.delete(clientToDelete)
                                            } else {
                                                clientViewModel.update(
                                                    clientToDelete.copy(endMonth = selectedMonth)
                                                )
                                            }
                                            selectedClient = null
                                        },
                                        onTogglePayment = { month, shouldPay ->
                                            scope.launch {
                                                if (shouldPay) {
                                                    paymentViewModel.markFullPayment(
                                                        clientId = client.id,
                                                        month = month,
                                                        amount = client.price
                                                    )
                                                } else {
                                                    paymentViewModel.markAsUnpaid(
                                                        clientId = client.id,
                                                        month = month
                                                    )
                                                }
                                            }
                                        },
                                        onPartialPaymentRequest = { month, partialAmount ->
                                            scope.launch {
                                                paymentViewModel.addPartialPayment(
                                                    clientId = client.id,
                                                    month = month,
                                                    monthAmount = client.price,
                                                    partialAmount = partialAmount
                                                )
                                            }
                                        },
                                        getMonthTransactions = { month ->
                                            paymentViewModel.getTransactionsForClientMonth(
                                                clientId = client.id,
                                                month = month
                                            )
                                        },
                                        onBack = { selectedClient = null }
                                    )

                                    if (showEditClientDialog) {
                                        ClientEditDialog(
                                            buildingList = buildings,
                                            initialName = client.name,
                                            initialSubscriptionNumber = client.subscriptionNumber,
                                            initialPrice = client.price.toString(),
                                            initialBuildingId = client.buildingId,
                                            initialStartMonth = client.startMonth,
                                            initialStartDay = client.startDay,
                                            initialFirstMonthAmount = client.firstMonthAmount?.toString() ?: "",
                                            initialPhone = client.phone,
                                            initialAddress = client.address,
                                            initialPackageType = client.packageType,
                                            initialNotes = client.notes,
                                            buildingSelectionEnabled = true,
                                            onSave = { name,
                                                       subscriptionNumber,
                                                       price,
                                                       buildingId,
                                                       startMonth,
                                                       startDay,
                                                       firstMonthAmount,
                                                       phone,
                                                       address,
                                                       packageType,
                                                       notes ->
                                                clientViewModel.update(
                                                    client.copy(
                                                        name = name,
                                                        subscriptionNumber = subscriptionNumber,
                                                        price = price,
                                                        firstMonthAmount = firstMonthAmount,
                                                        buildingId = buildingId,
                                                        startMonth = startMonth,
                                                        startDay = startDay,
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
                                    paymentViewModel = paymentViewModel,
                                    onAddClient = { client ->
                                        scope.launch {
                                            clientViewModel.insert(client)

                                            kotlinx.coroutines.delay(300)

                                            val allClients = clients
                                            val insertedClient =
                                                allClients.lastOrNull { it.subscriptionNumber == client.subscriptionNumber }

                                            insertedClient?.let { newClient ->
                                                paymentViewModel.createPaymentsForClient(
                                                    clientId = newClient.id,
                                                    startMonth = client.startMonth,
                                                    endMonth = null,
                                                    amount = client.price,
                                                    monthOptions = monthOptions
                                                )
                                            }
                                        }
                                    },
                                    onEditClient = { clientToEdit ->
                                        selectedClient = clientToEdit
                                        showEditClientDialog = true
                                    },
                                    onUpdateClient = { clientToUpdate ->
                                        clientViewModel.update(clientToUpdate)
                                    },
                                    onDeleteClient = { clientToDelete ->
                                        clientViewModel.delete(clientToDelete)
                                    },
                                    onEditBuilding = { buildingToEdit ->
                                        selectedBuilding = buildingToEdit
                                        showEditBuildingDialog = true
                                    },
                                    onDeleteBuilding = { buildingToDelete ->
                                        clients.filter { it.buildingId == buildingToDelete.id }
                                            .forEach { client ->
                                                clientViewModel.delete(client)
                                            }
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
                                        initialStartDay = selectedClient!!.startDay,
                                        initialFirstMonthAmount = selectedClient!!.firstMonthAmount?.toString()
                                            ?: "",
                                        initialPhone = selectedClient!!.phone,
                                        initialAddress = selectedClient!!.address,
                                        initialPackageType = selectedClient!!.packageType,
                                        initialNotes = selectedClient!!.notes,
                                        buildingSelectionEnabled = false,
                                        onSave = { name,
                                                   subscriptionNumber,
                                                   price,
                                                   buildingId,
                                                   startMonth,
                                                   startDay,
                                                   firstMonthAmount,
                                                   phone,
                                                   address,
                                                   packageType,
                                                   notes ->
                                            clientViewModel.update(
                                                selectedClient!!.copy(
                                                    name = name,
                                                    subscriptionNumber = subscriptionNumber,
                                                    price = price,
                                                    firstMonthAmount = firstMonthAmount,
                                                    buildingId = buildingId,
                                                    startMonth = startMonth,
                                                    startDay = startDay,
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
                                LaunchedEffect(selectedMonth) {
                                    paymentViewModel.setStatsMonth(selectedMonth)
                                }

                                val clientsCount by clientViewModel.clientsCount.observeAsState(0)
                                val monthStats by paymentViewModel.monthStats.observeAsState(null)

                                StatisticsScreen(
                                    clientsCount = clientsCount,
                                    buildingsCount = buildings.size,
                                    monthStats = monthStats,
                                    monthOptions = monthOptions,
                                    selectedMonth = selectedMonth,
                                    onMonthChange = { newMonth ->
                                        selectedMonth = newMonth
                                        paymentViewModel.setStatsMonth(newMonth)
                                    },
                                    allClients = clients
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
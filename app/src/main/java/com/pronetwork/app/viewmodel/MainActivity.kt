package com.pronetwork.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.pronetwork.app.data.Building
import com.pronetwork.app.data.Client
import com.pronetwork.app.data.ClientDatabase
import com.pronetwork.app.data.DailyBuildingCollection
import com.pronetwork.app.export.ClientsExportManager
import com.pronetwork.app.repository.PaymentTransactionRepository
import com.pronetwork.app.ui.components.ExportDialog
import com.pronetwork.app.ui.components.ExportFormat
import com.pronetwork.app.ui.components.ExportOption
import com.pronetwork.app.ui.components.ScreenTopBar
import com.pronetwork.app.ui.screens.BuildingDetailsScreen
import com.pronetwork.app.ui.screens.BuildingEditDialog
import com.pronetwork.app.ui.screens.BuildingListScreen
import com.pronetwork.app.ui.screens.ClientDetailsScreen
import com.pronetwork.app.ui.screens.ClientEditDialog
import com.pronetwork.app.ui.screens.ClientListScreen
import com.pronetwork.app.ui.screens.DailyCollectionScreen
import com.pronetwork.app.ui.screens.StatisticsScreen
import com.pronetwork.app.ui.theme.ProNetworkSpotTheme
import com.pronetwork.app.viewmodel.BuildingViewModel
import com.pronetwork.app.viewmodel.ClientViewModel
import com.pronetwork.app.viewmodel.DailyCollectionUi
import com.pronetwork.app.viewmodel.PaymentViewModel
import com.pronetwork.data.DailySummary
import com.pronetwork.data.MonthlyCollectionRatio
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {

    private val clientViewModel: ClientViewModel by viewModels()
    private val buildingViewModel: BuildingViewModel by viewModels()
    private val paymentViewModel: PaymentViewModel by viewModels()

    private lateinit var exportManager: ClientsExportManager

    private val transactionRepository by lazy {
        val db = ClientDatabase.getDatabase(application)
        PaymentTransactionRepository(db.paymentTransactionDao(), db.clientDao())
    }

    private fun filterClients(
        clients: List<Client>,
        selectedMonth: String,
        searchQuery: String,
        selectedFilterBuildingId: Int?,
        selectedFilterPackage: String?,
        sortByStartMonth: Boolean
    ): List<Client> {
        val monthFilteredClients = clients.filter { client ->
            try {
                val clientStartMonth = client.startMonth
                val currentViewMonth = selectedMonth
                val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val clientDate = dateFormat.parse(clientStartMonth)
                val viewDate = dateFormat.parse(currentViewMonth)
                if (viewDate != null && clientDate != null && viewDate.time >= clientDate.time) {
                    if (client.endMonth != null) {
                        val endDate = dateFormat.parse(client.endMonth)
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
        val searchedClients = monthFilteredClients.filter { client ->
            if (searchQuery.isBlank()) return@filter true
            val q = searchQuery.trim()
            client.name.contains(q, ignoreCase = true) ||
                    client.subscriptionNumber.contains(q, ignoreCase = true) ||
                    client.phone.contains(q, ignoreCase = true)
        }
        val buildingFilteredClients = searchedClients.filter { client ->
            selectedFilterBuildingId?.let { bid ->
                client.buildingId == bid
            } ?: true
        }
        val packageFilteredClients = buildingFilteredClients.filter { client ->
            selectedFilterPackage?.let { pkg ->
                client.packageType == pkg
            } ?: true
        }
        return if (sortByStartMonth) {
            packageFilteredClients.sortedBy { it.startMonth }
        } else {
            packageFilteredClients
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exportManager = ClientsExportManager(this)

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
            ProNetworkSpotTheme(darkTheme = false) {
                var refreshTrigger by remember { mutableStateOf(0) }
                var showExportDialogClients by remember { mutableStateOf(false) }
                var showExportDialogBuildings by remember { mutableStateOf(false) }
                var showExportDialogStats by remember { mutableStateOf(false) }
                var showExportDialogDaily by remember { mutableStateOf(false) }
                var currentScreen by remember { mutableStateOf("clients") }
                var showClientDialog by remember { mutableStateOf(false) }
                var showBuildingDialog by remember { mutableStateOf(false) }
                var showEditBuildingDialog by remember { mutableStateOf(false) }
                var selectedBuilding by remember { mutableStateOf<Building?>(null) }
                var selectedClient by remember { mutableStateOf<Client?>(null) }
                var showEditClientDialog by remember { mutableStateOf(false) }
                var searchQuery by remember { mutableStateOf("") }
                var selectedFilterBuildingId by remember { mutableStateOf<Int?>(null) }
                var selectedFilterPackage by remember { mutableStateOf<String?>(null) }
                var sortByStartMonth by remember { mutableStateOf(false) }
                var showFilterDialog by remember { mutableStateOf(false) }
                var showFilters by remember { mutableStateOf(false) }
                var showDailyCollection by remember { mutableStateOf(false) }
                var selectedDailyDateMillis by remember {
                    mutableStateOf(
                        Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.timeInMillis
                    )
                }

                val clients by clientViewModel.clients.observeAsState(emptyList())
                val buildings by buildingViewModel.buildings.observeAsState(emptyList())
                val buildingSearchQuery by buildingViewModel.searchQuery.observeAsState("")
                var dailyUi by remember { mutableStateOf<DailyCollectionUi?>(null) }
                var dailySummary by remember { mutableStateOf(DailySummary()) }
                var monthlyRatio by remember {
                    mutableStateOf(
                        MonthlyCollectionRatio(
                            expectedClients = 0,
                            paidClients = 0,
                            collectionRatio = 0f
                        )
                    )
                }

                fun loadDailyCollectionFor(dateMillis: Long) {
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = dateMillis
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val dayStartMillis = calendar.timeInMillis
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                    val dayEndMillis = calendar.timeInMillis
                    val liveData = paymentViewModel.getDailyBuildingCollectionsForDay(
                        dayStartMillis = dayStartMillis,
                        dayEndMillis = dayEndMillis
                    )
                    liveData.observe(this@MainActivity) { buildingCollections: List<DailyBuildingCollection> ->
                        val total = buildingCollections.sumOf { it.totalAmount }
                        dailyUi = DailyCollectionUi(
                            dateMillis = dateMillis,
                            totalAmount = total,
                            buildings = buildingCollections
                        )
                    }

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dateString = dateFormat.format(Date(dateMillis))

                    lifecycleScope.launch {
                        transactionRepository.getDailySummary(dateString)
                            .collect { summary ->
                                dailySummary = summary
                            }
                    }
                }

                LaunchedEffect(refreshTrigger) {
                    transactionRepository.getMonthlyCollectionRatio()
                        .collect { ratio ->
                            monthlyRatio = ratio
                        }
                }

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

                val filteredClients = filterClients(
                    clients = clients,
                    selectedMonth = selectedMonth,
                    searchQuery = searchQuery,
                    selectedFilterBuildingId = selectedFilterBuildingId,
                    selectedFilterPackage = selectedFilterPackage,
                    sortByStartMonth = sortByStartMonth
                )

                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val addRequiresBuildingMessage =
                    stringResource(R.string.clients_add_requires_building)
                val addActionLabel = stringResource(R.string.action_add)

                Scaffold(
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
                                    showDailyCollection = false
                                },
                                icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                label = { Text(stringResource(R.string.screen_clients)) }
                            )
                            NavigationBarItem(
                                selected = currentScreen == "buildings",
                                onClick = {
                                    currentScreen = "buildings"
                                    selectedBuilding = null
                                    selectedClient = null
                                    showDailyCollection = false
                                },
                                icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                                label = { Text(stringResource(R.string.screen_buildings)) }
                            )
                            NavigationBarItem(
                                selected = currentScreen == "stats",
                                onClick = {
                                    currentScreen = "stats"
                                    selectedBuilding = null
                                    selectedClient = null
                                    showDailyCollection = false
                                },
                                icon = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.List,
                                        contentDescription = null
                                    )
                                },
                                label = { Text(stringResource(R.string.screen_stats)) }
                            )
                        }
                    },
                    floatingActionButton = {
                        if (currentScreen == "clients" && selectedBuilding == null && selectedClient == null) {
                            FloatingActionButton(
                                onClick = {
                                    if (buildings.isNotEmpty()) {
                                        showClientDialog = true
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = addRequiresBuildingMessage
                                            )
                                        }
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = addActionLabel
                                )
                            }
                        }
                    },
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { padding ->
                    Surface(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        when {
                            // ===== clients =====
                            currentScreen == "clients" && selectedBuilding == null -> {
                                if (selectedClient == null) {
                                    Scaffold(
                                        topBar = {
                                            ScreenTopBar(
                                                title = stringResource(R.string.screen_clients),
                                                showOptions = true,
                                                options = listOf(
                                                    ExportOption.EXPORT,
                                                    ExportOption.IMPORT_CSV
                                                ),
                                                onOptionClick = { option ->
                                                    when (option) {
                                                        ExportOption.EXPORT -> showExportDialogClients =
                                                            true

                                                        ExportOption.IMPORT_CSV -> { /* Import */
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    ) { paddingValues ->
                                        Column(
                                            Modifier
                                                .fillMaxSize()
                                                .padding(paddingValues)
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = stringResource(
                                                        R.string.clients_title_with_month,
                                                        selectedMonth
                                                    ),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                TextButton(onClick = {
                                                    showFilters = !showFilters
                                                }) {
                                                    Text(
                                                        if (showFilters)
                                                            stringResource(R.string.clients_filters_hide)
                                                        else
                                                            stringResource(R.string.clients_filters_show)
                                                    )
                                                }
                                            }

                                            if (showFilters) {
                                                Box(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .padding(bottom = 12.dp)
                                                ) {
                                                    OutlinedTextField(
                                                        value = selectedMonth,
                                                        onValueChange = {},
                                                        readOnly = true,
                                                        label = {
                                                            Text(
                                                                stringResource(
                                                                    R.string.clients_browse_by_month
                                                                )
                                                            )
                                                        },
                                                        trailingIcon = {
                                                            IconButton(onClick = {
                                                                monthDropdownExpanded =
                                                                    !monthDropdownExpanded
                                                            }) {
                                                                Icon(
                                                                    Icons.Filled.ArrowDropDown,
                                                                    contentDescription = null
                                                                )
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                monthDropdownExpanded = true
                                                            },
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                            unfocusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                                                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                                                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    )
                                                    DropdownMenu(
                                                        expanded = monthDropdownExpanded,
                                                        onDismissRequest = {
                                                            monthDropdownExpanded = false
                                                        }
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

                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(bottom = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedTextField(
                                                        value = searchQuery,
                                                        onValueChange = { searchQuery = it },
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .heightIn(min = 36.dp),
                                                        placeholder = {
                                                            Text(
                                                                stringResource(R.string.clients_search_placeholder),
                                                                style = MaterialTheme.typography.bodySmall
                                                            )
                                                        },
                                                        singleLine = true,
                                                        textStyle = MaterialTheme.typography.bodySmall,
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                            unfocusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                                                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                                                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    )

                                                    Spacer(Modifier.width(8.dp))

                                                    IconButton(
                                                        onClick = { showFilterDialog = true }
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.FilterList,
                                                            contentDescription = stringResource(
                                                                R.string.clients_filter_and_sort
                                                            )
                                                        )
                                                    }
                                                }
                                            }

                                            val addClientRequiresBuildingMessage2 =
                                                stringResource(R.string.clients_add_requires_building)
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
                                                            snackbarHostState.showSnackbar(
                                                                message = addClientRequiresBuildingMessage2
                                                            )
                                                        }
                                                    }
                                                },
                                                onClientClick = { selectedClient = it }
                                            )

                                            if (showFilterDialog) {
                                                AlertDialog(
                                                    onDismissRequest = { showFilterDialog = false },
                                                    title = {
                                                        Text(
                                                            stringResource(
                                                                R.string.clients_filter_and_sort
                                                            )
                                                        )
                                                    },
                                                    text = {
                                                        Column(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            verticalArrangement = Arrangement.spacedBy(
                                                                8.dp
                                                            )
                                                        ) {
                                                            Text(
                                                                stringResource(R.string.clients_filter_building_label),
                                                                style = MaterialTheme.typography.labelMedium
                                                            )
                                                            var buildingFilterExpanded by remember {
                                                                mutableStateOf(
                                                                    false
                                                                )
                                                            }
                                                            ExposedDropdownMenuBox(
                                                                expanded = buildingFilterExpanded,
                                                                onExpandedChange = {
                                                                    buildingFilterExpanded =
                                                                        !buildingFilterExpanded
                                                                }
                                                            ) {
                                                                OutlinedTextField(
                                                                    readOnly = true,
                                                                    value = selectedFilterBuildingId?.let { bid ->
                                                                        buildings.firstOrNull { it.id == bid }?.name
                                                                            ?: stringResource(R.string.clients_filter_all_buildings)
                                                                    }
                                                                        ?: stringResource(R.string.clients_filter_all_buildings),
                                                                    onValueChange = {},
                                                                    trailingIcon = {
                                                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                                                            expanded = buildingFilterExpanded
                                                                        )
                                                                    },
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .menuAnchor()
                                                                )
                                                                ExposedDropdownMenu(
                                                                    expanded = buildingFilterExpanded,
                                                                    onDismissRequest = {
                                                                        buildingFilterExpanded =
                                                                            false
                                                                    }
                                                                ) {
                                                                    DropdownMenuItem(
                                                                        text = {
                                                                            Text(
                                                                                stringResource(R.string.clients_filter_all_buildings)
                                                                            )
                                                                        },
                                                                        onClick = {
                                                                            selectedFilterBuildingId =
                                                                                null
                                                                            buildingFilterExpanded =
                                                                                false
                                                                        }
                                                                    )
                                                                    buildings.forEach { b ->
                                                                        DropdownMenuItem(
                                                                            text = { Text(b.name) },
                                                                            onClick = {
                                                                                selectedFilterBuildingId =
                                                                                    b.id
                                                                                buildingFilterExpanded =
                                                                                    false
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            Text(
                                                                stringResource(R.string.clients_filter_package_label),
                                                                style = MaterialTheme.typography.labelMedium
                                                            )
                                                            val packageTypes =
                                                                clients.map { it.packageType }
                                                                    .distinct()
                                                                    .sorted()
                                                            var packageFilterExpanded by remember {
                                                                mutableStateOf(
                                                                    false
                                                                )
                                                            }
                                                            ExposedDropdownMenuBox(
                                                                expanded = packageFilterExpanded,
                                                                onExpandedChange = {
                                                                    packageFilterExpanded =
                                                                        !packageFilterExpanded
                                                                }
                                                            ) {
                                                                OutlinedTextField(
                                                                    readOnly = true,
                                                                    value = selectedFilterPackage
                                                                        ?: stringResource(R.string.clients_filter_all_packages),
                                                                    onValueChange = {},
                                                                    trailingIcon = {
                                                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                                                            expanded = packageFilterExpanded
                                                                        )
                                                                    },
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .menuAnchor()
                                                                )
                                                                ExposedDropdownMenu(
                                                                    expanded = packageFilterExpanded,
                                                                    onDismissRequest = {
                                                                        packageFilterExpanded =
                                                                            false
                                                                    }
                                                                ) {
                                                                    DropdownMenuItem(
                                                                        text = {
                                                                            Text(
                                                                                stringResource(R.string.clients_filter_all_packages)
                                                                            )
                                                                        },
                                                                        onClick = {
                                                                            selectedFilterPackage =
                                                                                null
                                                                            packageFilterExpanded =
                                                                                false
                                                                        }
                                                                    )
                                                                    packageTypes.forEach { pkg ->
                                                                        DropdownMenuItem(
                                                                            text = { Text(pkg) },
                                                                            onClick = {
                                                                                selectedFilterPackage =
                                                                                    pkg
                                                                                packageFilterExpanded =
                                                                                    false
                                                                            }
                                                                        )
                                                                    }
                                                                }
                                                            }

                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Checkbox(
                                                                    checked = sortByStartMonth,
                                                                    onCheckedChange = {
                                                                        sortByStartMonth = it
                                                                    }
                                                                )
                                                                Text(stringResource(R.string.clients_filter_sort_by_start))
                                                            }
                                                        }
                                                    },
                                                    confirmButton = {
                                                        Button(onClick = {
                                                            showFilterDialog = false
                                                        }) {
                                                            Text(stringResource(R.string.clients_filter_apply))
                                                        }
                                                    },
                                                    dismissButton = {
                                                        OutlinedButton(onClick = {
                                                            selectedFilterBuildingId = null
                                                            selectedFilterPackage = null
                                                            sortByStartMonth = false
                                                            showFilterDialog = false
                                                        }) {
                                                            Text(stringResource(R.string.clients_filter_clear))
                                                        }
                                                    }
                                                )
                                            }
                                        }

                                        if (showClientDialog) {
                                            ClientEditDialog(
                                                buildingList = buildings,
                                                buildingSelectionEnabled = true,
                                                onSave = { name, subscriptionNumber, price, buildingId, roomNumber, startMonth, startDay, firstMonthAmount, phone, address, packageType, notes ->
                                                    val newClient = Client(
                                                        name = name,
                                                        subscriptionNumber = subscriptionNumber,
                                                        price = price,
                                                        firstMonthAmount = firstMonthAmount,
                                                        buildingId = buildingId,
                                                        roomNumber = roomNumber,
                                                        startMonth = startMonth,
                                                        startDay = startDay,
                                                        phone = phone,
                                                        address = address,
                                                        packageType = packageType,
                                                        notes = notes
                                                    )
                                                    scope.launch {
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
                                                                monthOptions = monthOptions,
                                                                firstMonthAmount = firstMonthAmount
                                                            )
                                                        }
                                                    }
                                                    showClientDialog = false
                                                },
                                                onDismiss = { showClientDialog = false }
                                            )
                                        }

                                        if (showExportDialogClients) {
                                            ExportDialog(
                                                onDismiss = { showExportDialogClients = false },
                                                onExport = { type, period, format, buildingFilter, packageFilter ->
                                                    val filteredClientsForExport =
                                                        clients.filter { client ->
                                                            (buildingFilter == null || client.buildingId == buildingFilter) &&
                                                                    (packageFilter == null || client.packageType == packageFilter)
                                                        }

                                                    when (format) {
                                                        ExportFormat.SHARE -> {
                                                            exportManager.shareExcel(
                                                                clients = filteredClientsForExport,
                                                                buildings = buildings
                                                            )
                                                        }

                                                        ExportFormat.PDF -> {
                                                            exportManager.exportPdfToDownloads(
                                                                clients = filteredClientsForExport,
                                                                buildings = buildings
                                                            )
                                                        }

                                                        ExportFormat.EXCEL -> {
                                                            exportManager.exportExcelToDownloads(
                                                                clients = filteredClientsForExport,
                                                                buildings = buildings
                                                            )
                                                        }
                                                    }
                                                },
                                                buildings = buildings.map { it.id to it.name },
                                                packages = clients.map { it.packageType }.distinct()
                                                    .sorted()
                                            )
                                        }
                                    }
                                } else {
                                    val client = selectedClient!!
                                    val clientPayments by paymentViewModel
                                        .getClientPayments(client.id)
                                        .observeAsState(emptyList())
                                    val clientMonthUi by paymentViewModel
                                        .getClientMonthPaymentsUi(client.id)
                                        .observeAsState(emptyList())

                                    ClientDetailsScreen(
                                        client = client,
                                        buildingName = buildings.firstOrNull { it.id == client.buildingId }?.name
                                            ?: "",
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
                                        onTogglePayment = { month, monthAmount, shouldPay ->
                                            scope.launch {
                                                if (shouldPay) {
                                                    paymentViewModel.markFullPayment(
                                                        clientId = client.id,
                                                        month = month,
                                                        amount = monthAmount
                                                    )
                                                } else {
                                                    paymentViewModel.markAsUnpaid(
                                                        clientId = client.id,
                                                        month = month
                                                    )
                                                }
                                                refreshTrigger++
                                            }
                                        },
                                        onPartialPaymentRequest = { month, monthAmount, partialAmount ->
                                            scope.launch {
                                                paymentViewModel.addPartialPayment(
                                                    clientId = client.id,
                                                    month = month,
                                                    monthAmount = monthAmount,
                                                    partialAmount = partialAmount
                                                )
                                                refreshTrigger++
                                            }
                                        },
                                        getMonthTransactions = { month ->
                                            paymentViewModel.getTransactionsForClientMonth(
                                                clientId = client.id,
                                                month = month
                                            )
                                        },
                                        onDeleteTransaction = { transactionId ->
                                            paymentViewModel.deleteTransaction(transactionId)
                                            refreshTrigger++
                                        },
                                        onAddReverseTransaction = { month, monthAmount, refundAmount, reason ->
                                            paymentViewModel.addReverseTransaction(
                                                clientId = client.id,
                                                month = month,
                                                monthAmount = monthAmount,
                                                refundAmount = refundAmount,
                                                reason = reason
                                            )
                                            refreshTrigger++
                                        },
                                        onBack = { selectedClient = null }
                                    )
                                }
                            }

                            // ===== buildings list =====
                            currentScreen == "buildings" && selectedBuilding == null -> {
                                Scaffold(
                                    topBar = {
                                        ScreenTopBar(
                                            title = stringResource(R.string.screen_buildings),
                                            showOptions = true,
                                            options = listOf(
                                                ExportOption.EXPORT,
                                                ExportOption.IMPORT_CSV
                                            ),
                                            onOptionClick = { option ->
                                                when (option) {
                                                    ExportOption.EXPORT -> showExportDialogBuildings =
                                                        true

                                                    ExportOption.IMPORT_CSV -> { /* Import */
                                                    }
                                                }
                                            }
                                        )
                                    }
                                ) { paddingValues ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(paddingValues)
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        BuildingListScreen(
                                            buildings = buildings,
                                            searchQuery = buildingSearchQuery,
                                            onAddBuilding = { showBuildingDialog = true },
                                            onBuildingClick = { selectedBuilding = it },
                                            onSearch = { buildingViewModel.setSearchQuery(it) }
                                        )
                                    }
                                }

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

                            // ===== building details / client in building =====
                            selectedBuilding != null -> {
                                if (selectedClient == null) {
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
                                                        monthOptions = monthOptions,
                                                        firstMonthAmount = client.firstMonthAmount
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
                                        onClientClick = { client ->
                                            selectedClient = client
                                        },
                                        onBack = { selectedBuilding = null }
                                    )

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

                                    if (showEditClientDialog && selectedClient != null) {
                                        ClientEditDialog(
                                            buildingList = buildings,
                                            initialName = selectedClient!!.name,
                                            initialSubscriptionNumber = selectedClient!!.subscriptionNumber,
                                            initialPrice = selectedClient!!.price.toString(),
                                            initialBuildingId = selectedClient!!.buildingId,
                                            initialRoomNumber = selectedClient!!.roomNumber ?: "",
                                            initialStartMonth = selectedClient!!.startMonth,
                                            initialStartDay = selectedClient!!.startDay,
                                            initialFirstMonthAmount = selectedClient!!.firstMonthAmount?.toString()
                                                ?: "",
                                            initialPhone = selectedClient!!.phone,
                                            initialAddress = selectedClient!!.address,
                                            initialPackageType = selectedClient!!.packageType,
                                            initialNotes = selectedClient!!.notes,
                                            buildingSelectionEnabled = true,
                                            onSave = { name,
                                                       subscriptionNumber,
                                                       price: Double,
                                                       buildingId,
                                                       roomNumber,
                                                       startMonth,
                                                       startDay,
                                                       firstMonthAmount: Double?,
                                                       phone,
                                                       address,
                                                       packageType,
                                                       notes ->
                                                val oldPrice = selectedClient!!.price
                                                val newPrice = price
                                                clientViewModel.update(
                                                    selectedClient!!.copy(
                                                        name = name,
                                                        subscriptionNumber = subscriptionNumber,
                                                        price = newPrice,
                                                        firstMonthAmount = firstMonthAmount,
                                                        buildingId = buildingId,
                                                        roomNumber = roomNumber,
                                                        startMonth = startMonth,
                                                        startDay = startDay,
                                                        phone = phone,
                                                        address = address,
                                                        packageType = packageType,
                                                        notes = notes
                                                    )
                                                )
                                                if (newPrice != oldPrice) {
                                                    paymentViewModel.applyNewMonthlyPriceFromNextUnpaidMonth(
                                                        clientId = selectedClient!!.id,
                                                        newAmount = newPrice
                                                    )
                                                }
                                                showEditClientDialog = false
                                                selectedClient = null
                                            },
                                            onDismiss = { showEditClientDialog = false }
                                        )
                                    }

                                } else {
                                    val client = selectedClient!!
                                    val clientPayments by paymentViewModel
                                        .getClientPayments(client.id)
                                        .observeAsState(emptyList())
                                    val clientMonthUi by paymentViewModel
                                        .getClientMonthPaymentsUi(client.id)
                                        .observeAsState(emptyList())

                                    ClientDetailsScreen(
                                        client = client,
                                        buildingName = buildings.firstOrNull { it.id == client.buildingId }?.name
                                            ?: "",
                                        payments = clientPayments,
                                        monthUiList = clientMonthUi,
                                        onEdit = { clientToEdit ->
                                            selectedClient = clientToEdit
                                            showEditClientDialog = true
                                        },
                                        onDelete = { clientToDelete ->
                                            clientViewModel.delete(clientToDelete)
                                            selectedClient = null
                                        },
                                        onTogglePayment = { month, monthAmount, shouldPay ->
                                            scope.launch {
                                                if (shouldPay) {
                                                    paymentViewModel.markFullPayment(
                                                        clientId = client.id,
                                                        month = month,
                                                        amount = monthAmount
                                                    )
                                                } else {
                                                    paymentViewModel.markAsUnpaid(
                                                        clientId = client.id,
                                                        month = month
                                                    )
                                                }
                                                refreshTrigger++
                                            }
                                        },
                                        onPartialPaymentRequest = { month, monthAmount, partialAmount ->
                                            scope.launch {
                                                paymentViewModel.addPartialPayment(
                                                    clientId = client.id,
                                                    month = month,
                                                    monthAmount = monthAmount,
                                                    partialAmount = partialAmount
                                                )
                                                refreshTrigger++
                                            }
                                        },
                                        getMonthTransactions = { month ->
                                            paymentViewModel.getTransactionsForClientMonth(
                                                clientId = client.id,
                                                month = month
                                            )
                                        },
                                        onDeleteTransaction = { transactionId ->
                                            paymentViewModel.deleteTransaction(transactionId)
                                            refreshTrigger++
                                        },
                                        onAddReverseTransaction = { month, monthAmount, refundAmount, reason ->
                                            paymentViewModel.addReverseTransaction(
                                                clientId = client.id,
                                                month = month,
                                                monthAmount = monthAmount,
                                                refundAmount = refundAmount,
                                                reason = reason
                                            )
                                            refreshTrigger++
                                        },
                                        onBack = {
                                            selectedClient = null
                                        }
                                    )
                                }
                            }

                            // ===== stats =====
                            currentScreen == "stats" -> {
                                LaunchedEffect(selectedMonth, refreshTrigger) {
                                    paymentViewModel.setStatsMonth(selectedMonth)
                                }

                                LaunchedEffect(refreshTrigger) {
                                    transactionRepository.getMonthlyCollectionRatio()
                                        .collect { ratio ->
                                            monthlyRatio = ratio
                                        }
                                }

                                val clientsCount by clientViewModel.clientsCount.observeAsState(0)
                                val monthStats by paymentViewModel.monthStats.observeAsState(null)
                                if (!showDailyCollection) {
                                    Scaffold(
                                        topBar = {
                                            ScreenTopBar(
                                                title = stringResource(R.string.screen_stats),
                                                showOptions = true,
                                                options = listOf(ExportOption.EXPORT),
                                                onOptionClick = { option ->
                                                    when (option) {
                                                        ExportOption.EXPORT -> showExportDialogStats =
                                                            true

                                                        else -> {}
                                                    }
                                                }
                                            )
                                        }
                                    ) { paddingValues ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(paddingValues)
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    showDailyCollection = true
                                                    loadDailyCollectionFor(selectedDailyDateMillis)
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.CenterHorizontally)
                                                    .padding(bottom = 12.dp)
                                            ) {
                                                Text(stringResource(R.string.stats_daily_collection))
                                            }
                                            StatisticsScreen(
                                                clientsCount = clientsCount,
                                                buildingsCount = buildings.size,
                                                monthStats = monthStats,
                                                monthlyRatio = monthlyRatio,
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
                                } else {
                                    Scaffold(
                                        topBar = {
                                            ScreenTopBar(
                                                title = stringResource(R.string.stats_daily_collection),
                                                showOptions = true,
                                                options = listOf(ExportOption.EXPORT),
                                                onOptionClick = { option ->
                                                    when (option) {
                                                        ExportOption.EXPORT -> showExportDialogDaily =
                                                            true

                                                        else -> {}
                                                    }
                                                }
                                            )
                                        }
                                    ) { paddingValues ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(paddingValues)
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            DailyCollectionScreen(
                                                dailyCollection = dailyUi,
                                                dailySummary = dailySummary,
                                                selectedDateMillis = selectedDailyDateMillis,
                                                onChangeDate = { newMillis ->
                                                    selectedDailyDateMillis = newMillis
                                                    loadDailyCollectionFor(newMillis)
                                                }
                                            )

                                            Spacer(Modifier.height(8.dp))
                                            OutlinedButton(
                                                onClick = { showDailyCollection = false },
                                                modifier = Modifier.align(Alignment.CenterHorizontally)
                                            ) {
                                                Text(stringResource(R.string.stats_back_to_statistics))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // حوارات edit العامة
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

                        if (showEditClientDialog && selectedClient != null) {
                            ClientEditDialog(
                                buildingList = buildings,
                                initialName = selectedClient!!.name,
                                initialSubscriptionNumber = selectedClient!!.subscriptionNumber,
                                initialPrice = selectedClient!!.price.toString(),
                                initialBuildingId = selectedClient!!.buildingId,
                                initialRoomNumber = selectedClient!!.roomNumber ?: "",
                                initialStartMonth = selectedClient!!.startMonth,
                                initialStartDay = selectedClient!!.startDay,
                                initialFirstMonthAmount = selectedClient!!.firstMonthAmount?.toString()
                                    ?: "",
                                initialPhone = selectedClient!!.phone,
                                initialAddress = selectedClient!!.address,
                                initialPackageType = selectedClient!!.packageType,
                                initialNotes = selectedClient!!.notes,
                                buildingSelectionEnabled = true,
                                onSave = { name,
                                           subscriptionNumber,
                                           price: Double,
                                           buildingId,
                                           roomNumber,
                                           startMonth,
                                           startDay,
                                           firstMonthAmount: Double?,
                                           phone,
                                           address,
                                           packageType,
                                           notes ->
                                    val oldPrice = selectedClient!!.price
                                    val newPrice = price
                                    clientViewModel.update(
                                        selectedClient!!.copy(
                                            name = name,
                                            subscriptionNumber = subscriptionNumber,
                                            price = newPrice,
                                            firstMonthAmount = firstMonthAmount,
                                            buildingId = buildingId,
                                            roomNumber = roomNumber,
                                            startMonth = startMonth,
                                            startDay = startDay,
                                            phone = phone,
                                            address = address,
                                            packageType = packageType,
                                            notes = notes
                                        )
                                    )
                                    if (newPrice != oldPrice) {
                                        paymentViewModel.applyNewMonthlyPriceFromNextUnpaidMonth(
                                            clientId = selectedClient!!.id,
                                            newAmount = newPrice
                                        )
                                    }
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
    }
}

package com.pronetwork.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import com.pronetwork.app.data.Building
import com.pronetwork.app.data.Client
import com.pronetwork.app.export.ClientsExportManager
import com.pronetwork.app.export.ClientsImportManager
import com.pronetwork.app.export.DailyCollectionExportManager
import com.pronetwork.app.export.PaymentsExportManager
import com.pronetwork.app.network.ApprovalHelper
import com.pronetwork.app.network.ConnectivityObserver
import com.pronetwork.app.network.NotificationHelper
import com.pronetwork.app.network.RequestPollingWorker
import com.pronetwork.app.network.SyncEngine
import com.pronetwork.app.repository.BuildingRepository
import com.pronetwork.app.ui.components.DailyExportDialog
import com.pronetwork.app.ui.components.DailyReportMode
import com.pronetwork.app.ui.components.ExportDialog
import com.pronetwork.app.ui.components.ExportFormat
import com.pronetwork.app.ui.components.ExportOption
import com.pronetwork.app.ui.components.PaymentExportDialog
import com.pronetwork.app.ui.components.ScreenTopBar
import com.pronetwork.app.ui.components.SortOption
import com.pronetwork.app.ui.components.ViewOptionsDialog
import com.pronetwork.app.ui.screens.ApprovalRequestsScreen
import com.pronetwork.app.ui.screens.BuildingDetailsScreen
import com.pronetwork.app.ui.screens.BuildingEditDialog
import com.pronetwork.app.ui.screens.BuildingListScreen
import com.pronetwork.app.ui.screens.ClientDetailsScreen
import com.pronetwork.app.ui.screens.ClientEditDialog
import com.pronetwork.app.ui.screens.ClientListScreen
import com.pronetwork.app.ui.screens.DailyCollectionScreen
import com.pronetwork.app.ui.screens.DashboardScreen
import com.pronetwork.app.ui.screens.LoginScreen
import com.pronetwork.app.ui.screens.MyRequestsScreen
import com.pronetwork.app.ui.screens.RecentTransaction
import com.pronetwork.app.ui.screens.StatisticsScreen
import com.pronetwork.app.ui.screens.UnpaidClientInfo
import com.pronetwork.app.ui.screens.UserManagementScreen
import com.pronetwork.app.ui.theme.ProNetworkSpotTheme
import com.pronetwork.app.viewmodel.ApprovalRequestsViewModel
import com.pronetwork.app.viewmodel.BuildingViewModel
import com.pronetwork.app.viewmodel.ClientViewModel
import com.pronetwork.app.viewmodel.DailyCollectionUi
import com.pronetwork.app.viewmodel.LoginViewModel
import com.pronetwork.app.viewmodel.PaymentViewModel
import com.pronetwork.app.viewmodel.UserManagementViewModel
import com.pronetwork.data.DailySummary
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject


@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // === Hilt-injected dependencies ===
    @Inject lateinit var buildingRepo: BuildingRepository

    @Inject
    lateinit var paymentTransactionRepo: com.pronetwork.app.repository.PaymentTransactionRepository

    @Inject
    lateinit var syncEngine: com.pronetwork.app.network.SyncEngine

    // ViewModel references (set in setContent for importFileLauncher)
    private lateinit var _clientViewModel: ClientViewModel
    private lateinit var _paymentViewModel: PaymentViewModel

    private lateinit var exportManager: ClientsExportManager
    private lateinit var paymentsExportManager: PaymentsExportManager
    private lateinit var dailyExportManager: DailyCollectionExportManager
    private lateinit var importManager: ClientsImportManager
    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            lifecycleScope.launch {
                val result = importManager.importFromFile(
                    uri = selectedUri,
                    onClientsReady = { newClients ->
                        val monthsList =
                            java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                                .let { fmt ->
                                    val cal = java.util.Calendar.getInstance()
                                    List(25) {
                                        val m = fmt.format(cal.time)
                                        cal.add(java.util.Calendar.MONTH, -1)
                                        m
                                    }
                                }
                        // حفظ كل عميل والحصول على ID مباشرة ثم إنشاء سجلات الدفع
                        newClients.forEach { client ->
                            val newId = _clientViewModel.insertAndGetId(client)
                            val savedClientId = newId.toInt()
                            _paymentViewModel.createPaymentsForClient(
                                clientId = savedClientId,
                                startMonth = client.startMonth,
                                endMonth = null,
                                amount = client.price,
                                monthOptions = monthsList,
                                firstMonthAmount = client.firstMonthAmount
                            )
                        }
                    }
                )

                val msg = buildString {
                    if (result.success > 0) append("Imported ${result.success} clients")
                    if (result.newBuildings > 0) append(" (${result.newBuildings} new buildings created)")
                    if (result.skipped > 0) {
                        if (result.success > 0) append(", ")
                        append("Skipped ${result.skipped} duplicates")
                    }
                    if (result.success == 0 && result.skipped == 0) {
                        append("Import failed: ${result.errors.firstOrNull() ?: "Unknown error"}")
                    }
                }
                android.widget.Toast.makeText(
                    this@MainActivity,
                    msg,
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    private fun filterClients(
        clients: List<Client>,
        selectedMonth: String,
        searchQuery: String,
        selectedFilterBuildingId: Int?,
        selectedFilterPackage: String?,
        sortOption: SortOption
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
            selectedFilterBuildingId?.let { bid -> client.buildingId == bid } ?: true
        }

        val packageFilteredClients = buildingFilteredClients.filter { client ->
            selectedFilterPackage?.let { pkg -> client.packageType == pkg } ?: true
        }

        return when (sortOption) {
            SortOption.NAME_ASC -> packageFilteredClients.sortedBy { it.name.lowercase() }
            SortOption.NAME_DESC -> packageFilteredClients.sortedByDescending { it.name.lowercase() }
            SortOption.STATUS_UNPAID_FIRST -> packageFilteredClients
            SortOption.STATUS_PAID_FIRST -> packageFilteredClients
            SortOption.PRICE_HIGH -> packageFilteredClients.sortedByDescending { it.price }
            SortOption.PRICE_LOW -> packageFilteredClients.sortedBy { it.price }
            SortOption.BUILDING -> packageFilteredClients.sortedBy { it.buildingId }
            SortOption.PACKAGE -> packageFilteredClients.sortedBy { it.packageType.lowercase() }
            SortOption.START_MONTH -> packageFilteredClients.sortedBy { it.startMonth }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exportManager = ClientsExportManager(this)

        paymentsExportManager = PaymentsExportManager(
            this,
            paymentTransactionRepo,
            com.pronetwork.app.data.ClientDatabase.getDatabase(application)
        )

        dailyExportManager = DailyCollectionExportManager(this)

        importManager = ClientsImportManager(this, buildingRepo)

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

        // Setup notification channels
        NotificationHelper.createChannels(this)

        // Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
                    .launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Start background polling for approval request updates
        RequestPollingWorker.schedule(this)

        setContent {
            ProNetworkSpotTheme(darkTheme = false) {

                // === Hilt ViewModels ===
                val clientViewModel: ClientViewModel = hiltViewModel()
                val buildingViewModel: BuildingViewModel = hiltViewModel()
                val paymentViewModel: PaymentViewModel = hiltViewModel()
                val loginViewModel: LoginViewModel = hiltViewModel()
                val userManagementViewModel: UserManagementViewModel = hiltViewModel()
                val approvalRequestsViewModel: ApprovalRequestsViewModel = hiltViewModel()

                // Set references for importFileLauncher
                _clientViewModel = clientViewModel
                _paymentViewModel = paymentViewModel

                // AuthManager from Hilt (via LoginViewModel)
                val authManager = loginViewModel.authManager

                // Load requests on app start for badge count
                LaunchedEffect(Unit) {
                    approvalRequestsViewModel.loadRequests()
                }

                val loginState by loginViewModel.uiState.collectAsState()


                if (!loginState.loginSuccess) {
                    // ===== LOGIN SCREEN =====
                    LoginScreen(
                        uiState = loginState,
                        onUsernameChange = { loginViewModel.onUsernameChange(it) },
                        onPasswordChange = { loginViewModel.onPasswordChange(it) },
                        onTogglePassword = { loginViewModel.togglePasswordVisibility() },
                        onLogin = { loginViewModel.login() },
                        onClearError = { loginViewModel.clearError() }
                    )
                    return@ProNetworkSpotTheme
                }

                // ===== MAIN APP (after login) =====
                var refreshTrigger by remember { mutableStateOf(0) }
                var showExitDialog by remember { mutableStateOf(false) }
                var showExportDialogClients by remember { mutableStateOf(false) }
                var showExportDialogBuildings by remember { mutableStateOf(false) }
                var showExportDialogStats by remember { mutableStateOf(false) }
                var showExportDialogDaily by remember { mutableStateOf(false) }
                var currentScreen by remember { mutableStateOf("dashboard") }
                var showClientDialog by remember { mutableStateOf(false) }
                var showBuildingDialog by remember { mutableStateOf(false) }
                var showEditBuildingDialog by remember { mutableStateOf(false) }
                var selectedBuilding by remember { mutableStateOf<Building?>(null) }
                var selectedClient by remember { mutableStateOf<Client?>(null) }
                var showEditClientDialog by remember { mutableStateOf(false) }
                var searchQuery by remember { mutableStateOf("") }
                var selectedFilterBuildingId by remember { mutableStateOf<Int?>(null) }
                var selectedFilterPackage by remember { mutableStateOf<String?>(null) }
                var selectedSortOption by remember { mutableStateOf(SortOption.NAME_ASC) }
                var showViewOptionsDialog by remember { mutableStateOf(false) }
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

                // معالجة زر الرجوع في النظام
                BackHandler(enabled = true) {
                    when {
                        // إذا في شاشة تفاصيل عميل → ارجع لقائمة العملاء/المبنى
                        selectedClient != null -> {
                            selectedClient = null
                            showEditClientDialog = false
                        }
                        // إذا في شاشة تفاصيل مبنى → ارجع لقائمة المباني
                        selectedBuilding != null -> {
                            selectedBuilding = null
                            showEditBuildingDialog = false
                        }
                        // إذا في التحصيل اليومي → ارجع للإحصائيات
                        showDailyCollection -> {
                            showDailyCollection = false
                        }
                        // إذا في أي شاشة غير clients → ارجع لـ clients
                        currentScreen != "clients" -> {
                            currentScreen = "clients"
                        }
                        // إذا في الشاشة الرئيسية → أظهر مربع حوار تأكيد الخروج
                        else -> {
                            showExitDialog = true
                        }
                    }
                }

                val clients by clientViewModel.clients.observeAsState(emptyList())
                val buildings by buildingViewModel.buildings.observeAsState(emptyList())
                val buildingSearchQuery by buildingViewModel.searchQuery.observeAsState("")
                var dailyUi by remember { mutableStateOf<DailyCollectionUi?>(null) }
                var dailySummary by remember { mutableStateOf(DailySummary()) }

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

                    val monthFormat =
                        java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.getDefault())
                    val currentMonth = monthFormat.format(java.util.Date(dateMillis))

                    // جلب التحصيل التفصيلي (عميل بعميل)
                    val detailedLiveData = paymentViewModel.getDetailedDailyCollections(
                        dayStartMillis, dayEndMillis, currentMonth
                    )
                    detailedLiveData.observe(this@MainActivity) { detailedBuildings ->
                        val total = detailedBuildings.sumOf { it.totalAmount }
                        val totalExpected = detailedBuildings.sumOf { it.expectedAmount }
                        val overallRate =
                            if (totalExpected > 0) (total / totalExpected) * 100 else 0.0
                        val totalClients = detailedBuildings.sumOf { it.clientsCount }
                        val topBuilding =
                            detailedBuildings.maxByOrNull { it.collectionRate }?.buildingName
                        val lowBuilding =
                            detailedBuildings.minByOrNull { it.collectionRate }?.buildingName

                        dailyUi = DailyCollectionUi(
                            dateMillis = dateMillis,
                            totalAmount = total,
                            buildings = detailedBuildings,
                            totalExpected = totalExpected,
                            overallCollectionRate = overallRate,
                            totalClientsCount = totalClients,
                            topBuilding = topBuilding,
                            lowBuilding = lowBuilding,
                            paidClientsCount = detailedBuildings.flatMap { it.clients }
                                .count { it.paymentStatus == "PAID" },
                            partialClientsCount = detailedBuildings.flatMap { it.clients }
                                .count { it.paymentStatus == "PARTIAL" },
                            settledClientsCount = detailedBuildings.flatMap { it.clients }
                                .count { it.paymentStatus == "SETTLED" },
                            unpaidClientsCount = detailedBuildings.flatMap { it.clients }
                                .count { it.paymentStatus == "UNPAID" },
                            settledAmount = detailedBuildings.flatMap { it.clients }
                                .filter { it.paymentStatus == "SETTLED" }.sumOf { it.totalPaid },
                            refundAmount = detailedBuildings.flatMap { it.clients }
                                .flatMap { it.transactions }.filter { it.type == "Refund" }
                                .sumOf { kotlin.math.abs(it.amount) }
                        )
                    }

                    // جلب الملخص (totalAmount, totalClients, totalTransactions)
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val dateString = dateFormat.format(Date(dateMillis))
                    lifecycleScope.launch {
                        paymentViewModel.getDailySummary(dateString)
                            .collect { summary ->
                                if (summary != null) {
                                    dailySummary = summary
                                }
                            }
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
                    sortOption = selectedSortOption
                )

                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val addRequiresBuildingMessage =
                    stringResource(R.string.clients_add_requires_building)
                val addActionLabel = stringResource(R.string.action_add)

                // Sync status
                val context = LocalContext.current
                val app = context.applicationContext as com.pronetwork.app.ProNetworkApp
                val connectivityStatus by app.connectivityObserver.observe()
                    .collectAsState(initial = ConnectivityObserver.Status.UNAVAILABLE)

                val syncState by syncEngine.syncState.collectAsState()


                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            // Dashboard Tab
                            NavigationBarItem(
                                selected = currentScreen == "dashboard",
                                onClick = {
                                    currentScreen = "dashboard"
                                    selectedBuilding = null
                                    selectedClient = null
                                    showDailyCollection = false
                                },
                                icon = { Icon(Icons.Filled.Dashboard, contentDescription = null) },
                                label = { Text(stringResource(R.string.screen_dashboard)) }
                            )
                            // Clients Tab
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

                            // Admin Tab (Admin) / My Requests Tab (User)
                            NavigationBarItem(
                                selected = currentScreen == "admin",
                                onClick = {
                                    currentScreen = "admin"
                                    selectedBuilding = null
                                    selectedClient = null
                                    showDailyCollection = false
                                },
                                icon = {
                                    val approvalState by approvalRequestsViewModel.uiState.collectAsState()
                                    val pendingCount =
                                        approvalState.requests.count { it.status == "PENDING" }

                                    if (pendingCount > 0 && currentScreen != "admin") {
                                        androidx.compose.material3.BadgedBox(
                                            badge = {
                                                androidx.compose.material3.Badge {
                                                    Text("$pendingCount")
                                                }
                                            }
                                        ) {
                                            if (loginState.role == "ADMIN") {
                                                Icon(
                                                    Icons.Filled.AdminPanelSettings,
                                                    contentDescription = null
                                                )
                                            } else {
                                                Icon(
                                                    Icons.AutoMirrored.Filled.List,
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                    } else {
                                        if (loginState.role == "ADMIN") {
                                            Icon(
                                                Icons.Filled.AdminPanelSettings,
                                                contentDescription = null
                                            )
                                        } else {
                                            Icon(
                                                Icons.AutoMirrored.Filled.List,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                },
                                label = {
                                    if (loginState.role == "ADMIN") {
                                        Text(stringResource(R.string.screen_admin))
                                    } else {
                                        Text(stringResource(R.string.my_requests_title))
                                    }
                                }
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
                        // Load stats for Dashboard and Stats screens
                        LaunchedEffect(selectedMonth, refreshTrigger) {
                            paymentViewModel.setStatsMonth(selectedMonth)
                        }
                        val clientsCount by clientViewModel.clientsCount.observeAsState(0)
                        val monthStats by paymentViewModel.monthStats.observeAsState(null)

                        when {
                            // ===== Dashboard =====
                            currentScreen == "dashboard" -> {
                                val previousMonthStatsState by paymentViewModel.previousMonthStats.observeAsState(
                                    null
                                )

                                // آخر الحركات
                                val recentTxRaw by paymentViewModel.getRecentTransactions(10)
                                    .observeAsState(emptyList())
                                val timeFormat = remember {
                                    SimpleDateFormat(
                                        "hh:mm a",
                                        Locale.getDefault()
                                    )
                                }
                                val dashboardRecentTransactions = remember(recentTxRaw) {
                                    recentTxRaw.map { tx ->
                                        RecentTransaction(
                                            clientName = tx.clientName,
                                            amount = tx.transactionAmount,
                                            type = if (tx.transactionAmount < 0) "Refund" else "Payment",
                                            time = timeFormat.format(Date(tx.transactionDate)),
                                            buildingName = tx.buildingName
                                        )
                                    }
                                }

                                // العملاء المتأخرين
                                val unpaidRaw by paymentViewModel.getTopUnpaidClients(
                                    selectedMonth,
                                    5
                                ).observeAsState(emptyList())
                                val dashboardUnpaidClients = remember(unpaidRaw) {
                                    unpaidRaw.map { client ->
                                        UnpaidClientInfo(
                                            clientId = client.clientId,
                                            clientName = client.clientName,
                                            buildingName = client.buildingName,
                                            monthlyAmount = client.monthlyAmount,
                                            totalPaid = client.totalPaid,
                                            remaining = client.remaining
                                        )
                                    }
                                }

                                // Logout Confirmation Dialog
                                var showLogoutDialog by remember { mutableStateOf(false) }

                                if (showLogoutDialog) {
                                    AlertDialog(
                                        onDismissRequest = { showLogoutDialog = false },
                                        title = { Text(stringResource(R.string.logout_confirm_title)) },
                                        text = { Text(stringResource(R.string.logout_confirm_msg)) },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    showLogoutDialog = false
                                                    RequestPollingWorker.cancel(this@MainActivity)
                                                    loginViewModel.logout()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.error
                                                )
                                            ) {
                                                Text(stringResource(R.string.logout))
                                            }
                                        },
                                        dismissButton = {
                                            OutlinedButton(onClick = {
                                                showLogoutDialog = false
                                            }) {
                                                Text(stringResource(R.string.action_cancel))
                                            }
                                        }
                                    )
                                }

                                DashboardScreen(
                                    userName = loginState.displayName,
                                    userRole = loginState.role,
                                    onLogout = { showLogoutDialog = true },
                                    connectivityStatus = connectivityStatus,
                                    currentMonthStats = monthStats,
                                    previousMonthStats = previousMonthStatsState,
                                    totalClients = filteredClients.size,
                                    totalBuildings = buildings.size,
                                    recentTransactions = dashboardRecentTransactions,
                                    topUnpaidClients = dashboardUnpaidClients,
                                    syncState = syncState,

                                    onNavigateToDaily = {
                                        currentScreen = "stats"
                                        showDailyCollection = true
                                        loadDailyCollectionFor(selectedDailyDateMillis)
                                    },
                                    onNavigateToClients = {
                                        currentScreen = "clients"
                                    },
                                    onNavigateToStats = {
                                        currentScreen = "stats"
                                    },
                                    onClientClick = { clientId ->
                                        val client = clients.find { it.id == clientId }
                                        if (client != null) {
                                            selectedClient = client
                                            currentScreen = "clients"
                                        }
                                    }
                                )
                            }

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
                                                    ExportOption.IMPORT_EXCEL
                                                ),
                                                onOptionClick = { option ->
                                                    when (option) {
                                                        ExportOption.EXPORT -> {
                                                            if (authManager.isAdmin()) {
                                                                showExportDialogClients = true
                                                            } else {
                                                                ApprovalHelper.executeOrRequest(
                                                                    context = this@MainActivity,
                                                                    authManager = authManager,
                                                                    scope = scope,
                                                                    requestType = "EXPORT_REPORT",
                                                                    targetName = "Clients Export",
                                                                    onAdminDirect = { },
                                                                    onRequestSent = {
                                                                        android.widget.Toast.makeText(
                                                                            this@MainActivity,
                                                                            getString(R.string.approval_export_request_sent),
                                                                            android.widget.Toast.LENGTH_LONG
                                                                        ).show()
                                                                    },
                                                                    onError = { error ->
                                                                        android.widget.Toast.makeText(
                                                                            this@MainActivity,
                                                                            getString(
                                                                                R.string.approval_request_error,
                                                                                error
                                                                            ),
                                                                            android.widget.Toast.LENGTH_LONG
                                                                        ).show()
                                                                    }
                                                                )
                                                            }
                                                        }

                                                        ExportOption.IMPORT_EXCEL -> {
                                                            importFileLauncher.launch(
                                                                arrayOf(
                                                                    "application/vnd.ms-excel",
                                                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                                                )
                                                            )
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
                                                                    monthDropdownExpanded =
                                                                        false
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

                                            // زر خيارات الفرز
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 4.dp),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                TextButton(onClick = {
                                                    showViewOptionsDialog = true
                                                }) {
                                                    Icon(
                                                        imageVector = Icons.Filled.FilterList,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(stringResource(R.string.view_options_button))
                                                }
                                            }

                                            // حوار خيارات الفرز
                                            if (showViewOptionsDialog) {
                                                ViewOptionsDialog(
                                                    currentSort = selectedSortOption,
                                                    onSortSelected = { option ->
                                                        selectedSortOption = option
                                                        showViewOptionsDialog = false
                                                    },
                                                    onDismiss = {
                                                        showViewOptionsDialog = false
                                                    }
                                                )
                                            }

                                            val addClientRequiresBuildingMessage2 =
                                                stringResource(R.string.clients_add_requires_building)
                                            ClientListScreen(
                                                clients = filteredClients,
                                                buildings = buildings,
                                                selectedMonth = selectedMonth,
                                                paymentViewModel = paymentViewModel,
                                                sortOption = selectedSortOption,
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
                                                    onDismissRequest = {
                                                        showFilterDialog = false
                                                    },
                                                    title = { Text(stringResource(R.string.clients_filter_and_sort)) },
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
                                                                                null; buildingFilterExpanded =
                                                                            false
                                                                        }
                                                                    )
                                                                    buildings.forEach { b ->
                                                                        DropdownMenuItem(
                                                                            text = { Text(b.name) },
                                                                            onClick = {
                                                                                selectedFilterBuildingId =
                                                                                    b.id; buildingFilterExpanded =
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
                                                                    .distinct().sorted()
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
                                                                                null; packageFilterExpanded =
                                                                            false
                                                                        }
                                                                    )
                                                                    packageTypes.forEach { pkg ->
                                                                        DropdownMenuItem(
                                                                            text = { Text(pkg) },
                                                                            onClick = {
                                                                                selectedFilterPackage =
                                                                                    pkg; packageFilterExpanded =
                                                                                false
                                                                            }
                                                                        )
                                                                    }
                                                                }
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

                                        // ✅ تعديل: إضافة SHARE_PDF لتصدير العملاء
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

                                                        ExportFormat.SHARE_PDF -> {
                                                            exportManager.sharePdf(
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
                                                packages = clients.map { it.packageType }
                                                    .distinct()
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
                                                ExportOption.IMPORT_EXCEL
                                            ),
                                            onOptionClick = { option ->
                                                when (option) {
                                                    ExportOption.EXPORT -> showExportDialogBuildings =
                                                        true

                                                    ExportOption.IMPORT_EXCEL -> { /* Import */
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
                                            initialRoomNumber = selectedClient!!.roomNumber
                                                ?: "",
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
                                if (!showDailyCollection) {
                                    Scaffold(
                                        topBar = {
                                            ScreenTopBar(
                                                title = stringResource(R.string.screen_stats),
                                                showOptions = true,
                                                options = listOf(ExportOption.EXPORT),
                                                onOptionClick = { option ->
                                                    when (option) {
                                                        ExportOption.EXPORT -> {
                                                            if (authManager.isAdmin()) {
                                                                showExportDialogStats = true
                                                            } else {
                                                                ApprovalHelper.executeOrRequest(
                                                                    context = this@MainActivity,
                                                                    authManager = authManager,
                                                                    scope = scope,
                                                                    requestType = "EXPORT_REPORT",
                                                                    targetName = "Statistics Export",
                                                                    onAdminDirect = { },
                                                                    onRequestSent = {
                                                                        android.widget.Toast.makeText(
                                                                            this@MainActivity,
                                                                            getString(R.string.approval_export_request_sent),
                                                                            android.widget.Toast.LENGTH_LONG
                                                                        ).show()
                                                                    },
                                                                    onError = { error ->
                                                                        android.widget.Toast.makeText(
                                                                            this@MainActivity,
                                                                            getString(
                                                                                R.string.approval_request_error,
                                                                                error
                                                                            ),
                                                                            android.widget.Toast.LENGTH_LONG
                                                                        ).show()
                                                                    }
                                                                )
                                                            }
                                                        }

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
                                                    loadDailyCollectionFor(
                                                        selectedDailyDateMillis
                                                    )
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
                                                        ExportOption.EXPORT -> {
                                                            if (authManager.isAdmin()) {
                                                                showExportDialogDaily = true
                                                            } else {
                                                                ApprovalHelper.executeOrRequest(
                                                                    context = this@MainActivity,
                                                                    authManager = authManager,
                                                                    scope = scope,
                                                                    requestType = "EXPORT_REPORT",
                                                                    targetName = "Daily Collection Export",
                                                                    onAdminDirect = { },
                                                                    onRequestSent = {
                                                                        android.widget.Toast.makeText(
                                                                            this@MainActivity,
                                                                            getString(R.string.approval_export_request_sent),
                                                                            android.widget.Toast.LENGTH_LONG
                                                                        ).show()
                                                                    },
                                                                    onError = { error ->
                                                                        android.widget.Toast.makeText(
                                                                            this@MainActivity,
                                                                            getString(
                                                                                R.string.approval_request_error,
                                                                                error
                                                                            ),
                                                                            android.widget.Toast.LENGTH_LONG
                                                                        ).show()
                                                                    }
                                                                )
                                                            }
                                                        }

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

                                if (showExportDialogDaily) {
                                    val dailyDateFormat =
                                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                    val dailyDateLabel =
                                        dailyDateFormat.format(Date(selectedDailyDateMillis))

                                    DailyExportDialog(
                                        dateLabel = dailyDateLabel,
                                        onDismiss = { showExportDialogDaily = false },
                                        onExport = { format, mode ->
                                            val currentUi =
                                                if (mode == DailyReportMode.TRANSACTIONS_ONLY) dailyUi?.let { ui ->
                                                    val filteredBuildings =
                                                        ui.buildings.map { b -> b.copy(clients = b.clients.filter { it.todayPaid != 0.0 || it.transactions.isNotEmpty() }) }
                                                            .filter { it.clients.isNotEmpty() }; ui.copy(
                                                    buildings = filteredBuildings,
                                                    totalClientsCount = filteredBuildings.sumOf { it.clientsCount })
                                                } else dailyUi
                                            val currentSummary =
                                                if (mode == DailyReportMode.TRANSACTIONS_ONLY && currentUi != null) {
                                                    val txClients =
                                                        currentUi.buildings.sumOf { it.clientsCount };
                                                    val txCount =
                                                        currentUi.buildings.flatMap { it.clients }
                                                            .sumOf { it.transactions.size }; dailySummary.copy(
                                                        totalClients = txClients,
                                                        totalTransactions = txCount
                                                    )
                                                } else dailySummary
                                            if (currentUi != null) {
                                                lifecycleScope.launch {
                                                    when (format) {
                                                        ExportFormat.EXCEL -> dailyExportManager.exportExcelToDownloads(
                                                            date = dailyDateLabel,
                                                            ui = currentUi,
                                                            summary = currentSummary
                                                        )

                                                        ExportFormat.PDF -> dailyExportManager.exportPdfToDownloads(
                                                            date = dailyDateLabel,
                                                            ui = currentUi,
                                                            summary = currentSummary
                                                        )

                                                        ExportFormat.SHARE -> dailyExportManager.shareExcel(
                                                            date = dailyDateLabel,
                                                            ui = currentUi,
                                                            summary = currentSummary
                                                        )

                                                        ExportFormat.SHARE_PDF -> dailyExportManager.sharePdf(
                                                            date = dailyDateLabel,
                                                            ui = currentUi,
                                                            summary = currentSummary
                                                        )
                                                    }
                                                }
                                            }
                                            showExportDialogDaily = false
                                        }
                                    )
                                }

                                // ✅ تعديل: إضافة SHARE_PDF لتصدير الإحصائيات
                                if (showExportDialogStats) {
                                    PaymentExportDialog(
                                        monthOptions = monthOptions,
                                        selectedMonth = selectedMonth,
                                        buildings = buildings.map { it.id to it.name },
                                        packages = clients.map { it.packageType }.distinct()
                                            .sorted(),
                                        onDismiss = { showExportDialogStats = false },
                                        onExport = { reportType, period, month, endMonth, format,
                                                     buildingFilter, packageFilter, statusFilter ->
                                            lifecycleScope.launch {
                                                when (format) {
                                                    ExportFormat.EXCEL -> paymentsExportManager.exportExcelToDownloads(
                                                        reportType = reportType,
                                                        period = period,
                                                        startMonth = month,
                                                        endMonth = endMonth,
                                                        buildingFilter = buildingFilter,
                                                        packageFilter = packageFilter,
                                                        statusFilter = statusFilter
                                                    )

                                                    ExportFormat.SHARE -> paymentsExportManager.shareExcel(
                                                        reportType = reportType,
                                                        period = period,
                                                        startMonth = month,
                                                        endMonth = endMonth,
                                                        buildingFilter = buildingFilter,
                                                        packageFilter = packageFilter,
                                                        statusFilter = statusFilter
                                                    )

                                                    ExportFormat.PDF -> paymentsExportManager.exportPdfToDownloads(
                                                        reportType = reportType,
                                                        period = period,
                                                        startMonth = month,
                                                        endMonth = endMonth,
                                                        buildingFilter = buildingFilter,
                                                        packageFilter = packageFilter,
                                                        statusFilter = statusFilter
                                                    )

                                                    ExportFormat.SHARE_PDF -> paymentsExportManager.sharePdf(
                                                        reportType = reportType,
                                                        period = period,
                                                        startMonth = month,
                                                        endMonth = endMonth,
                                                        buildingFilter = buildingFilter,
                                                        packageFilter = packageFilter,
                                                        statusFilter = statusFilter
                                                    )
                                                }

                                            }
                                            showExportDialogStats = false
                                        }
                                    )
                                }
                            }

                            // ===== admin (User Management) / user (My Requests) =====
                            currentScreen == "admin" && loginState.role == "ADMIN" -> {
                                var adminTab by remember { mutableStateOf("users") }
                                val userMgmtState by userManagementViewModel.uiState.collectAsState()
                                val approvalState by approvalRequestsViewModel.uiState.collectAsState()

                                Column(modifier = Modifier.fillMaxSize()) {
                                    // Sub-tab row
                                    androidx.compose.material3.TabRow(
                                        selectedTabIndex = if (adminTab == "users") 0 else 1,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        androidx.compose.material3.Tab(
                                            selected = adminTab == "users",
                                            onClick = { adminTab = "users" },
                                            text = { Text(stringResource(R.string.admin_tab_users)) },
                                            icon = {
                                                Icon(
                                                    Icons.Filled.AdminPanelSettings,
                                                    contentDescription = null
                                                )
                                            }
                                        )
                                        androidx.compose.material3.Tab(
                                            selected = adminTab == "requests",
                                            onClick = {
                                                adminTab = "requests"
                                                approvalRequestsViewModel.loadRequests()
                                            },
                                            text = { Text(stringResource(R.string.admin_tab_requests)) },
                                            icon = {
                                                val pendingCount =
                                                    approvalState.requests.count { it.status == "PENDING" }
                                                if (pendingCount > 0) {
                                                    androidx.compose.material3.BadgedBox(
                                                        badge = {
                                                            androidx.compose.material3.Badge {
                                                                Text("$pendingCount")
                                                            }
                                                        }
                                                    ) {
                                                        Icon(
                                                            Icons.Filled.Person,
                                                            contentDescription = null
                                                        )
                                                    }
                                                } else {
                                                    Icon(
                                                        Icons.Filled.Person,
                                                        contentDescription = null
                                                    )
                                                }
                                            }
                                        )
                                    }

                                    when (adminTab) {
                                        "users" -> UserManagementScreen(
                                            uiState = userMgmtState,
                                            onRefresh = { userManagementViewModel.loadUsers() },
                                            onCreateClick = { userManagementViewModel.showCreateDialog() },
                                            onEditClick = { user ->
                                                userManagementViewModel.showEditDialog(
                                                    user
                                                )
                                            },
                                            onToggleClick = { userId ->
                                                userManagementViewModel.toggleUser(
                                                    userId
                                                )
                                            },
                                            onDeleteClick = { userId ->
                                                userManagementViewModel.deleteUser(
                                                    userId
                                                )
                                            },
                                            onDismissDialog = { userManagementViewModel.dismissDialog() },
                                            onCreateUser = { userManagementViewModel.createUser() },
                                            onUpdateUser = { userManagementViewModel.updateUser() },
                                            onFormUsernameChange = {
                                                userManagementViewModel.onFormUsernameChange(
                                                    it
                                                )
                                            },
                                            onFormPasswordChange = {
                                                userManagementViewModel.onFormPasswordChange(
                                                    it
                                                )
                                            },
                                            onFormDisplayNameChange = {
                                                userManagementViewModel.onFormDisplayNameChange(
                                                    it
                                                )
                                            },
                                            onFormRoleChange = {
                                                userManagementViewModel.onFormRoleChange(
                                                    it
                                                )
                                            },
                                            onClearMessages = { userManagementViewModel.clearMessages() }
                                        )

                                        "requests" -> ApprovalRequestsScreen(
                                            uiState = approvalState,
                                            onRefresh = { approvalRequestsViewModel.loadRequests() },
                                            onApprove = { id ->
                                                approvalRequestsViewModel.approveRequest(
                                                    id
                                                )
                                            },
                                            onRejectClick = { id ->
                                                approvalRequestsViewModel.showRejectDialog(
                                                    id
                                                )
                                            },
                                            onDismissReject = { approvalRequestsViewModel.dismissRejectDialog() },
                                            onConfirmReject = { id ->
                                                approvalRequestsViewModel.rejectRequest(
                                                    id
                                                )
                                            },
                                            onFilterChange = { filter ->
                                                approvalRequestsViewModel.setFilter(
                                                    filter
                                                )
                                            },
                                            onClearMessages = { approvalRequestsViewModel.clearMessages() }
                                        )
                                    }
                                }
                            }

                            // ===== My Requests (User role only) =====
                            currentScreen == "admin" && loginState.role != "ADMIN" -> {
                                val approvalState by approvalRequestsViewModel.uiState.collectAsState()

                                LaunchedEffect(Unit) {
                                    approvalRequestsViewModel.loadRequests()
                                }

                                MyRequestsScreen(
                                    uiState = approvalState,
                                    onRefresh = { approvalRequestsViewModel.loadRequests() },
                                    onFilterChange = { filter ->
                                        approvalRequestsViewModel.setFilter(
                                            filter
                                        )
                                    },
                                    onCancelRequest = { id ->
                                        approvalRequestsViewModel.cancelRequest(
                                            id
                                        )
                                    },
                                    onClearMessages = { approvalRequestsViewModel.clearMessages() }
                                )
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

                        // مربع حوار تأكيد الخروج
                        if (showExitDialog) {
                            AlertDialog(
                                onDismissRequest = { showExitDialog = false },
                                title = { Text("Exit App") },
                                text = { Text("Are you sure you want to exit?") },
                                confirmButton = {
                                    Button(onClick = { finish() }) {
                                        Text("Exit")
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(onClick = { showExitDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

package com.pronetwork.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pronetwork.app.data.Building
import com.pronetwork.app.data.Client
import com.pronetwork.app.viewmodel.PaymentViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingDetailsScreen(
    building: Building,
    allClients: List<Client>,
    monthOptions: List<String>,
    paymentViewModel: PaymentViewModel,
    onAddClient: (Client) -> Unit,
    onEditClient: (Client) -> Unit,
    onUpdateClient: (Client) -> Unit,
    onDeleteClient: (Client) -> Unit,
    onEditBuilding: (Building) -> Unit,
    onDeleteBuilding: (Building) -> Unit,
    onBack: () -> Unit
) {
    var showAddClientDialog by remember { mutableStateOf(false) }
    var selectedClient by remember { mutableStateOf<Client?>(null) }
    var showEditClientDialog by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf(monthOptions.first()) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }
    var showDeleteBuildingDialog by remember { mutableStateOf(false) }
    var showDeleteClientDialog by remember { mutableStateOf(false) }
    var clientToDelete by remember { mutableStateOf<Client?>(null) }
    var showPaymentDialog by remember { mutableStateOf<Pair<Client, Boolean>?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val buildingClients = remember(allClients, building.id, selectedMonth) {
        allClients.filter { client ->
            client.buildingId == building.id &&
                    try {
                        val clientStartMonth = client.startMonth
                        val currentViewMonth = selectedMonth
                        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                        val clientDate = sdf.parse(clientStartMonth)
                        val viewDate = sdf.parse(currentViewMonth)

                        if (viewDate != null && clientDate != null && viewDate.time >= clientDate.time) {
                            if (client.endMonth != null) {
                                val endDate = sdf.parse(client.endMonth)
                                endDate != null && viewDate.time < endDate.time
                            } else {
                                true
                            }
                        } else {
                            false
                        }
                    } catch (_: Exception) {
                        client.startMonth == selectedMonth
                    }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(building.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // معلومات المبنى
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("معلومات المبنى", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))

                    if (building.location.isNotEmpty()) {
                        Text("الموقع:", style = MaterialTheme.typography.labelMedium)
                        Text(building.location)
                        Spacer(Modifier.height(8.dp))
                    }

                    if (building.notes.isNotEmpty()) {
                        Text("ملاحظات:", style = MaterialTheme.typography.labelMedium)
                        Text(building.notes)
                        Spacer(Modifier.height(8.dp))
                    }

                    if (building.floors > 0) {
                        Text("عدد الطوابق:", style = MaterialTheme.typography.labelMedium)
                        Text("${building.floors}")
                        Spacer(Modifier.height(8.dp))
                    }

                    if (building.managerName.isNotEmpty()) {
                        Text("اسم المدير:", style = MaterialTheme.typography.labelMedium)
                        Text(building.managerName)
                        Spacer(Modifier.height(8.dp))
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onEditBuilding(building) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("تعديل المبنى")
                        }

                        OutlinedButton(
                            onClick = { showDeleteBuildingDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("حذف المبنى")
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // اختيار الشهر
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
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { monthDropdownExpanded = true }
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
            Spacer(Modifier.height(8.dp))

            if (buildingClients.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا يوجد عملاء في هذا الشهر.")
                }
            } else {
                buildingClients.forEach { client ->
                    val payment by paymentViewModel
                        .getPaymentLive(client.id, selectedMonth)
                        .observeAsState(null)
                    val isPaid = payment?.isPaid ?: false

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        elevation = CardDefaults.cardElevation(5.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPaid)
                                MaterialTheme.colorScheme.secondaryContainer
                            else
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            Modifier
                                .clickable { selectedClient = client }
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("اسم: ${client.name}", style = MaterialTheme.typography.titleMedium)
                                Text("رقم الاشتراك: ${client.subscriptionNumber}")
                                Text("رقم الجوال: ${client.phone}")
                                Text(
                                    "الحالة: ${if (isPaid) "مدفوع" else "غير مدفوع"}",
                                    color = if (isPaid)
                                        MaterialTheme.colorScheme.tertiary
                                    else
                                        MaterialTheme.colorScheme.error
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isPaid) {
                                    Button(
                                        onClick = { showPaymentDialog = client to true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.tertiary
                                        )
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("تأكيد")
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { showPaymentDialog = client to false }
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("تراجع")
                                    }
                                }
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        selectedClient = client
                                        showEditClientDialog = true
                                    }
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "تعديل العميل")
                                }
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = {
                                        clientToDelete = client
                                        showDeleteClientDialog = true
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "حذف العميل",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // إضافة عميل
            if (showAddClientDialog) {
                ClientEditDialog(
                    buildingList = listOf(building),
                    initialBuildingId = building.id,
                    initialStartMonth = selectedMonth,
                    initialStartDay = 1,
                    initialFirstMonthAmount = "",
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
                        onAddClient(
                            Client(
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
                        showAddClientDialog = false
                    },
                    onDismiss = { showAddClientDialog = false }
                )
            }

            // تفاصيل / تعديل العميل
            selectedClient?.let { client ->
                if (!showEditClientDialog) {
                    ClientDetailsBottomSheet(
                        client = client,
                        buildingName = building.name,
                        onEdit = { showEditClientDialog = true },
                        onDelete = {
                            if (selectedMonth == client.startMonth) {
                                onDeleteClient(client)
                            } else {
                                onUpdateClient(client.copy(endMonth = selectedMonth))
                            }
                            selectedClient = null
                        },
                        onTogglePaid = { showPaymentDialog = client to true },
                        onUndoPaid = { showPaymentDialog = client to false },
                        onDismiss = { selectedClient = null }
                    )
                }

                if (showEditClientDialog) {
                    ClientEditDialog(
                        buildingList = listOf(building),
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
                            onEditClient(
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
                        onDismiss = {
                            showEditClientDialog = false
                            selectedClient = null
                        }
                    )
                }
            }
        }

        // تأكيد الدفع / التراجع
        showPaymentDialog?.let { (client, shouldPay) ->
            AlertDialog(
                onDismissRequest = { showPaymentDialog = null },
                icon = {
                    Icon(
                        if (shouldPay) Icons.Filled.CheckCircle else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (shouldPay)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text(
                        if (shouldPay) "تأكيد الدفع" else "تراجع عن الدفع",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column {
                        Text(
                            if (shouldPay)
                                "هل أنت متأكد من تأكيد دفع شهر $selectedMonth؟"
                            else
                                "هل أنت متأكد من التراجع عن دفع شهر $selectedMonth؟"
                        )
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("العميل: ${client.name}")
                                Text("الشهر: $selectedMonth")
                                Text(
                                    "المبلغ: ${client.price} ريال",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                if (shouldPay) {
                                    paymentViewModel.markAsPaid(
                                        clientId = client.id,
                                        month = selectedMonth,
                                        amount = client.price
                                    )
                                    snackbarHostState.showSnackbar("✓ تم تأكيد الدفع لشهر $selectedMonth")
                                } else {
                                    paymentViewModel.markAsUnpaid(
                                        clientId = client.id,
                                        month = selectedMonth
                                    )
                                    snackbarHostState.showSnackbar("تم التراجع عن الدفع لشهر $selectedMonth")
                                }
                            }
                            showPaymentDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (shouldPay)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(if (shouldPay) "نعم، تأكيد" else "نعم، تراجع")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showPaymentDialog = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // حذف المبنى
        if (showDeleteBuildingDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteBuildingDialog = false },
                title = { Text("تأكيد حذف المبنى") },
                text = {
                    Column {
                        Text("هل أنت متأكد من حذف المبنى \"${building.name}\"؟")
                        if (allClients.any { it.buildingId == building.id }) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "تحذير: سيتم حذف جميع العملاء المرتبطين بهذا المبنى.",
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteBuilding(building)
                            showDeleteBuildingDialog = false
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("نعم، احذف")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteBuildingDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // حذف العميل
        if (showDeleteClientDialog && clientToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteClientDialog = false
                    clientToDelete = null
                },
                title = { Text("تأكيد الحذف") },
                text = {
                    Column {
                        val c = clientToDelete!!
                        if (selectedMonth == c.startMonth) {
                            Text("هل أنت متأكد من حذف العميل \"${c.name}\" نهائيًا من جميع الشهور؟")
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "تحذير: سيتم حذف العميل بالكامل.",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("هل تريد إيقاف اشتراك العميل \"${c.name}\" ابتداءً من شهر $selectedMonth؟")
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "سيبقى العميل ظاهرًا في الشهور السابقة لـ $selectedMonth.",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val c = clientToDelete!!
                            if (selectedMonth == c.startMonth) {
                                onDeleteClient(c)
                            } else {
                                onUpdateClient(c.copy(endMonth = selectedMonth))
                            }
                            showDeleteClientDialog = false
                            clientToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("حذف")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showDeleteClientDialog = false
                            clientToDelete = null
                        }
                    ) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

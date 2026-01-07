package com.pronetwork.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
    onClientClick: (Client) -> Unit,
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

    // ⬅️ الآن نحفظ Client + month + monthAmount
    var showPaymentDialog by remember { mutableStateOf<Triple<Client, String, Double>?>(null) }

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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                // معلومات المبنى
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                Text("تعديل المبنى", color = MaterialTheme.colorScheme.onPrimary)
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
                                Text("حذف المبنى", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            item {
                // اختيار الشهر
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
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
            }

            item {
                Text(
                    "عملاء المبنى في $selectedMonth:",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            items(buildingClients) { client ->
                val payment by paymentViewModel
                    .getPaymentLive(client.id, selectedMonth)
                    .observeAsState(null)
                val isPaid = payment?.isPaid ?: false
                val monthAmount = payment?.amount ?: client.price

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    elevation = CardDefaults.cardElevation(5.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPaid)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { onClientClick(client) }
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                        ) {
                            Text("الاسم: ${client.name}", style = MaterialTheme.typography.titleMedium)
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
                                    onClick = {
                                        showPaymentDialog = Triple(
                                            client,
                                            selectedMonth,
                                            monthAmount
                                        )
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.tertiary
                                    )
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("تأكيد", color = MaterialTheme.colorScheme.onTertiary)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        showPaymentDialog = Triple(
                                            client,
                                            selectedMonth,
                                            monthAmount
                                        )
                                    }
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("تراجع", color = MaterialTheme.colorScheme.primary)
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

            if (buildingClients.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا يوجد عملاء في هذا الشهر.")
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
                           price: Double,
                           buildingId,
                           startMonth,
                           startDay,
                           firstMonthAmount: Double?,
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

        // تعديل عميل
        if (showEditClientDialog && selectedClient != null) {
            ClientEditDialog(
                buildingList = listOf(building),
                initialName = selectedClient!!.name,
                initialSubscriptionNumber = selectedClient!!.subscriptionNumber,
                initialPrice = selectedClient!!.price.toString(),
                initialBuildingId = building.id,
                initialStartMonth = selectedClient!!.startMonth,
                initialStartDay = selectedClient!!.startDay,
                initialFirstMonthAmount = selectedClient!!.firstMonthAmount?.toString() ?: "",
                initialPhone = selectedClient!!.phone,
                initialAddress = selectedClient!!.address,
                initialPackageType = selectedClient!!.packageType,
                initialNotes = selectedClient!!.notes,
                buildingSelectionEnabled = false,
                onSave = { name,
                           subscriptionNumber,
                           price: Double,
                           buildingId,
                           startMonth,
                           startDay,
                           firstMonthAmount: Double?,
                           phone,
                           address,
                           packageType,
                           notes ->

                    onUpdateClient(
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
                onDismiss = {
                    showEditClientDialog = false
                    selectedClient = null
                }
            )
        }
    }

    // تأكيد الدفع / التراجع باستخدام monthAmount الفعلي
    showPaymentDialog?.let { (client, month, monthAmount) ->
        val payment by paymentViewModel
            .getPaymentLive(client.id, month)
            .observeAsState()
        val isPaid = payment?.isPaid ?: false
        val shouldPay = !isPaid

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
                            "هل أنت متأكد من تأكيد دفع شهر $month؟"
                        else
                            "هل أنت متأكد من التراجع عن دفع شهر $month؟"
                    )
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text("العميل: ${client.name}")
                            Text("الشهر: $month")
                            Text(
                                "المبلغ: $monthAmount ريال",
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
                    Text("نعم، احذف", color = MaterialTheme.colorScheme.onError)
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
                    Text("حذف", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showDeleteClientDialog = false
                    clientToDelete = null
                }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

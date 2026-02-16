package com.pronetwork.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pronetwork.app.R
import com.pronetwork.app.data.Building
import com.pronetwork.app.data.Client
import com.pronetwork.app.viewmodel.PaymentStatus
import com.pronetwork.app.viewmodel.PaymentViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

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
    // ✅ استعادة الحالة الداخلية التي كانت تعمل في الإصدار القديم
    var showAddClientDialog by remember { mutableStateOf(false) }

    // ✅ جديد: التحكم في عرض العملاء المستقبليين (لحل مشكلة العميل المضاف بتاريخ مستقبلي)
    var showFutureClients by remember { mutableStateOf(false) }

    var selectedClient by remember { mutableStateOf<Client?>(null) }
    var selectedMonth by remember { mutableStateOf(monthOptions.first()) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }
    var showDeleteBuildingDialog by remember { mutableStateOf(false) }
    var showDeleteClientDialog by remember { mutableStateOf(false) }
    var clientToDelete by remember { mutableStateOf<Client?>(null) }

    // Client + month + monthAmount
    var showPaymentDialog by remember { mutableStateOf<Triple<Client, String, Double>?>(null) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ✅ تعديل: إضافة showFutureClients كمعتمد في remember
    val buildingClients = remember(allClients, building.id, selectedMonth, showFutureClients) {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())

        allClients.filter { client ->
            client.buildingId == building.id &&
                    try {
                        val clientStartMonth = client.startMonth
                        val currentViewMonth = selectedMonth

                        val clientDate = sdf.parse(clientStartMonth)
                        val viewDate = sdf.parse(currentViewMonth)

                        // ✅ المنطق الجديد: عرض العملاء المستقبليين عند التفعيل
                        if (showFutureClients) {
                            // نعرض العميل حتى لو كان تاريخ البداية في المستقبل
                            if (client.endMonth != null) {
                                val endDate = sdf.parse(client.endMonth)
                                endDate != null && viewDate.time < endDate.time
                            } else {
                                true
                            }
                        } else {
                            // المنطق الأصلي: نعرض فقط العملاء الذين بدأوا قبل أو في الشهر الحالي
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddClientDialog = true }, // ✅ السلوك الأصلي الصحيح
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = stringResource(R.string.building_details_add_client)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Building info
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.building_details_info_title),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(12.dp))

                        if (building.location.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.building_details_location_label),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(building.location)
                            Spacer(Modifier.height(8.dp))
                        }

                        if (building.notes.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.building_details_notes_label),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(building.notes)
                            Spacer(Modifier.height(8.dp))
                        }

                        if (building.floors > 0) {
                            Text(
                                text = stringResource(R.string.building_details_floors_label),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text("${building.floors}")
                            Spacer(Modifier.height(8.dp))
                        }

                        if (building.managerName.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.building_details_manager_label),
                                style = MaterialTheme.typography.labelMedium
                            )
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
                                Text(
                                    text = stringResource(R.string.building_details_edit_building),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
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
                                Text(
                                    text = stringResource(R.string.building_details_delete_building),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Month selector
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = selectedMonth,
                        onValueChange = {},
                        readOnly = true,
                        label = {
                            Text(
                                stringResource(R.string.building_details_browse_by_month)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = {
                                monthDropdownExpanded = !monthDropdownExpanded
                            }) {
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

            // ✅ جديد: زر التحكم في عرض العملاء المستقبليين (يوضع مباشرة بعد حقل اختيار الشهر)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = showFutureClients,
                        onCheckedChange = { showFutureClients = it }
                    )
                    Text(
                        text = stringResource(R.string.building_details_show_future_clients),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (showFutureClients) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Clients section title
            item {
                Text(
                    text = stringResource(
                        R.string.building_details_clients_in_month,
                        selectedMonth
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Clients list
            items(buildingClients) { client ->
                val status by paymentViewModel
                    .getClientMonthStatus(client.id, selectedMonth)
                    .observeAsState(initial = PaymentStatus.UNPAID)

                val payment by paymentViewModel
                    .getPaymentLive(client.id, selectedMonth)
                    .observeAsState(null)

                val monthAmount = payment?.amount ?: client.price

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    elevation = CardDefaults.cardElevation(5.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (status) {
                            PaymentStatus.FULL -> MaterialTheme.colorScheme.secondaryContainer
                            PaymentStatus.PARTIAL -> MaterialTheme.colorScheme.surfaceVariant
                            PaymentStatus.SETTLED -> MaterialTheme.colorScheme.tertiaryContainer
                            PaymentStatus.UNPAID -> MaterialTheme.colorScheme.surface
                        }
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
                        // Client info
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = stringResource(R.string.building_details_client_name_label),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = client.name,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = stringResource(R.string.building_details_client_subscription_label),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = client.subscriptionNumber,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = stringResource(R.string.building_details_client_phone_label),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Text(
                                text = client.phone,
                                style = MaterialTheme.typography.bodyMedium
                            )

                            Spacer(Modifier.height(6.dp))

                            val statusText = when (status) {
                                PaymentStatus.UNPAID -> stringResource(R.string.building_details_status_unpaid)
                                PaymentStatus.PARTIAL -> stringResource(R.string.building_details_status_partial)
                                PaymentStatus.SETTLED -> stringResource(R.string.building_details_status_settled)
                                PaymentStatus.FULL -> stringResource(R.string.building_details_status_full)
                            }
                            val statusColor = when (status) {
                                PaymentStatus.UNPAID -> MaterialTheme.colorScheme.error
                                PaymentStatus.PARTIAL -> MaterialTheme.colorScheme.primary
                                PaymentStatus.SETTLED -> MaterialTheme.colorScheme.secondary
                                PaymentStatus.FULL -> MaterialTheme.colorScheme.tertiary
                            }

                            Text(
                                text = stringResource(R.string.building_details_status_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = statusColor
                            )
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = statusColor
                            )
                        }

                        // Actions
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            when (status) {
                                PaymentStatus.UNPAID, PaymentStatus.PARTIAL -> {
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
                                        Icon(
                                            Icons.Filled.CheckCircle,
                                            contentDescription = null
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = if (status == PaymentStatus.PARTIAL)
                                                stringResource(R.string.building_details_complete_payment)
                                            else
                                                stringResource(R.string.building_details_confirm_payment),
                                            color = MaterialTheme.colorScheme.onTertiary
                                        )
                                    }
                                }

                                PaymentStatus.SETTLED -> {
                                    Text(
                                        text = stringResource(R.string.building_details_status_settled),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                PaymentStatus.FULL -> {
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
                                        Text(
                                            text = stringResource(
                                                R.string.building_details_revert_payment
                                            ),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        onEditClient(client)
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = stringResource(
                                            R.string.building_details_edit_client
                                        )
                                    )
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
                                        contentDescription = stringResource(
                                            R.string.building_details_delete_client
                                        ),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
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
                        Text(
                            text = stringResource(
                                R.string.building_details_no_clients_this_month
                            )
                        )
                    }
                }
            }
        }

        // ✅ الموضع الصحيح: داخل Scaffold content لكن خارج LazyColumn (كما في الإصدار القديم الذي كان يعمل)
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
                           roomNumber,
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
                            roomNumber = roomNumber,
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
    }

    // payment confirm/revert dialog
    showPaymentDialog?.let { (client, month, monthAmount) ->
        val payment by paymentViewModel
            .getPaymentLive(client.id, month)
            .observeAsState()

        val isPaid = payment?.isPaid ?: false
        val shouldPay = !isPaid

        // جلب حالة الدفع التفصيلية لمعرفة المتبقي
        val monthUiList by paymentViewModel
            .getClientMonthPaymentsUi(client.id)
            .observeAsState(emptyList())
        val monthUi = monthUiList.firstOrNull { it.month == month }
        val isPartial = monthUi != null && monthUi.status == PaymentStatus.PARTIAL
        val remaining = monthUi?.remaining ?: monthAmount

        AlertDialog(
            onDismissRequest = { showPaymentDialog = null },
            icon = {
                Icon(
                    if (shouldPay) Icons.Filled.CheckCircle
                    else Icons.Filled.Close,
                    contentDescription = null,
                    tint = if (shouldPay) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = if (shouldPay)
                        stringResource(R.string.building_details_dialog_confirm_title)
                    else
                        stringResource(R.string.building_details_dialog_revert_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    Text(
                        text = if (isPartial)
                            stringResource(
                                R.string.building_details_complete_payment_text,
                                month,
                                remaining
                            )
                        else if (shouldPay)
                            stringResource(
                                R.string.building_details_dialog_confirm_text,
                                month
                            )
                        else
                            stringResource(
                                R.string.building_details_dialog_revert_text,
                                month
                            )
                    )
                    Spacer(Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(
                                    R.string.building_details_dialog_client_name,
                                    client.name
                                )
                            )
                            Text(
                                text = stringResource(
                                    R.string.building_details_dialog_month,
                                    month
                                )
                            )
                            Text(
                                text = stringResource(
                                    R.string.building_details_dialog_amount,
                                    if (isPartial) remaining else monthAmount
                                ),
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
                    Text(
                        text = if (shouldPay)
                            stringResource(R.string.building_details_dialog_yes_confirm)
                        else
                            stringResource(R.string.building_details_dialog_yes_revert)
                    )
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPaymentDialog = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // delete building dialog
    if (showDeleteBuildingDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteBuildingDialog = false },
            title = {
                Text(stringResource(R.string.building_details_delete_building_title))
            },
            text = {
                Column {
                    Text(
                        text = stringResource(
                            R.string.building_details_delete_building_text,
                            building.name
                        )
                    )
                    if (allClients.any { it.buildingId == building.id }) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.building_details_delete_building_warning
                            ),
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
                    Text(
                        text = stringResource(
                            R.string.building_details_delete_building_confirm
                        ),
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteBuildingDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // delete client dialog
    if (showDeleteClientDialog && clientToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteClientDialog = false
                clientToDelete = null
            },
            title = {
                Text(stringResource(R.string.building_details_delete_client_title))
            },
            text = {
                Column {
                    val c = clientToDelete!!
                    if (selectedMonth == c.startMonth) {
                        Text(
                            text = stringResource(
                                R.string.building_details_delete_client_full_text,
                                c.name
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.building_details_delete_client_full_warning
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(
                            text = stringResource(
                                R.string.building_details_delete_client_stop_text,
                                c.name,
                                selectedMonth
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.building_details_delete_client_stop_info,
                                selectedMonth
                            ),
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
                    Text(
                        text = stringResource(
                            R.string.building_details_delete_client_confirm
                        ),
                        color = MaterialTheme.colorScheme.onError
                    )
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showDeleteClientDialog = false
                    clientToDelete = null
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
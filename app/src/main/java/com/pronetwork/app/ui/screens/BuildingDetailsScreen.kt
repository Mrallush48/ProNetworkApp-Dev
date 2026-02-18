package com.pronetwork.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pronetwork.app.R
import com.pronetwork.app.data.Building
import com.pronetwork.app.data.Client
import com.pronetwork.app.viewmodel.PaymentStatus
import com.pronetwork.app.viewmodel.PaymentViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import com.pronetwork.app.ui.components.SortOption
import com.pronetwork.app.ui.components.ViewOptionsDialog
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.filled.FilterList

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
    var showFutureClients by remember { mutableStateOf(false) }
    var selectedClient by remember { mutableStateOf<Client?>(null) }
    var selectedMonth by remember { mutableStateOf(monthOptions.first()) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }
    var showDeleteBuildingDialog by remember { mutableStateOf(false) }
    var showDeleteClientDialog by remember { mutableStateOf(false) }
    var clientToDelete by remember { mutableStateOf<Client?>(null) }
    var showPaymentDialog by remember { mutableStateOf<Triple<Client, String, Double>?>(null) }

    var selectedSortOption by remember { mutableStateOf(SortOption.NAME_ASC) }
    var showViewOptionsDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "SA")).apply {
            maximumFractionDigits = 0
        }
    }

    val buildingClients = remember(allClients, building.id, selectedMonth, showFutureClients) {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        allClients.filter { client ->
            client.buildingId == building.id && try {
                val clientStartMonth = client.startMonth
                val currentViewMonth = selectedMonth
                val clientDate = sdf.parse(clientStartMonth)
                val viewDate = sdf.parse(currentViewMonth)
                if (showFutureClients) {
                    if (client.endMonth != null) {
                        val endDate = sdf.parse(client.endMonth)
                        endDate != null && viewDate!!.time < endDate.time
                    } else { true }
                } else {
                    if (viewDate != null && clientDate != null && viewDate.time >= clientDate.time) {
                        if (client.endMonth != null) {
                            val endDate = sdf.parse(client.endMonth)
                            endDate != null && viewDate.time < endDate.time
                        } else { true }
                    } else { false }
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
                onClick = { showAddClientDialog = true },
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
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // معلومات المبنى
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(3.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.building_details_info_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(12.dp))
                        if (building.location.isNotEmpty()) {
                            Text(text = stringResource(R.string.building_details_location_label), style = MaterialTheme.typography.labelMedium)
                            Text(building.location)
                            Spacer(Modifier.height(8.dp))
                        }
                        if (building.notes.isNotEmpty()) {
                            Text(text = stringResource(R.string.building_details_notes_label), style = MaterialTheme.typography.labelMedium)
                            Text(building.notes)
                            Spacer(Modifier.height(8.dp))
                        }
                        if (building.floors > 0) {
                            Text(text = stringResource(R.string.building_details_floors_label), style = MaterialTheme.typography.labelMedium)
                            Text("${building.floors}")
                            Spacer(Modifier.height(8.dp))
                        }
                        if (building.managerName.isNotEmpty()) {
                            Text(text = stringResource(R.string.building_details_manager_label), style = MaterialTheme.typography.labelMedium)
                            Text(building.managerName)
                            Spacer(Modifier.height(8.dp))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { onEditBuilding(building) }, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.Edit, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(text = stringResource(R.string.building_details_edit_building), color = MaterialTheme.colorScheme.onPrimary)
                            }
                            OutlinedButton(
                                onClick = { showDeleteBuildingDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(text = stringResource(R.string.building_details_delete_building), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // اختيار الشهر
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                    OutlinedTextField(
                        value = selectedMonth, onValueChange = {}, readOnly = true,
                        label = { Text(stringResource(R.string.building_details_browse_by_month)) },
                        trailingIcon = {
                            IconButton(onClick = { monthDropdownExpanded = !monthDropdownExpanded }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { monthDropdownExpanded = true }
                    )
                    DropdownMenu(expanded = monthDropdownExpanded, onDismissRequest = { monthDropdownExpanded = false }) {
                        monthOptions.forEach { month ->
                            DropdownMenuItem(text = { Text(month) }, onClick = { selectedMonth = month; monthDropdownExpanded = false })
                        }
                    }
                }
            }

            // عرض العملاء المستقبليين
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showFutureClients, onCheckedChange = { showFutureClients = it })
                    Text(
                        text = stringResource(R.string.building_details_show_future_clients),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (showFutureClients) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // عنوان قسم العملاء + زر الفرز
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.building_details_clients_in_month, selectedMonth),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { showViewOptionsDialog = true }) {
                        Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.view_options_button))
                    }
                }
            }

            // ===== قائمة العملاء بالتصميم الجديد =====
            items(
                when (selectedSortOption) {
                    SortOption.NAME_ASC -> buildingClients.sortedBy { it.name.lowercase() }
                    SortOption.NAME_DESC -> buildingClients.sortedByDescending { it.name.lowercase() }
                    SortOption.STATUS_UNPAID_FIRST -> buildingClients.sortedBy { if (it.isPaid) 3 else 0 }
                    SortOption.STATUS_PAID_FIRST -> buildingClients.sortedByDescending { if (it.isPaid) 3 else 0 }
                    SortOption.PRICE_HIGH -> buildingClients.sortedByDescending { it.price }
                    SortOption.PRICE_LOW -> buildingClients.sortedBy { it.price }
                    SortOption.BUILDING -> buildingClients.sortedBy { it.buildingId }
                    SortOption.PACKAGE -> buildingClients.sortedBy { it.packageType.lowercase() }
                    SortOption.START_MONTH -> buildingClients.sortedBy { it.startMonth }
                }
            ) { client ->
            val status by paymentViewModel.getClientMonthStatus(client.id, selectedMonth).observeAsState(initial = PaymentStatus.UNPAID)
                val payment by paymentViewModel.getPaymentLive(client.id, selectedMonth).observeAsState(null)
                val monthAmount = payment?.amount ?: client.price

                val monthUiList by paymentViewModel.getClientMonthPaymentsUi(client.id).observeAsState(emptyList())
                val monthUi = monthUiList.firstOrNull { it.month == selectedMonth }
                val totalPaid = monthUi?.totalPaid ?: 0.0
                val remaining = monthUi?.remaining ?: monthAmount

                val statusColor = getStatusColor(status)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(3.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                        // الشريط اللوني
                        Box(
                            modifier = Modifier.width(6.dp).fillMaxHeight()
                                .background(statusColor, shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                        )

                        Column(modifier = Modifier.weight(1f).clickable { onClientClick(client) }.padding(12.dp)) {
                            // الاسم + شارة الحالة
                            Row(Modifier.fillMaxWidth(), horizontalArrangement
                            = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = client.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                // شارة الحالة
                                val statusText = when (status) {
                                    PaymentStatus.UNPAID -> stringResource(R.string.building_details_status_unpaid)
                                    PaymentStatus.PARTIAL -> stringResource(R.string.building_details_status_partial)
                                    PaymentStatus.SETTLED -> stringResource(R.string.building_details_status_settled)
                                    PaymentStatus.FULL -> stringResource(R.string.building_details_status_full)
                                }
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = statusColor.copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                                        Text(text = statusText, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = statusColor)
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // رقم الاشتراك + رقم الغرفة
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(text = stringResource(R.string.building_details_client_subscription_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                    Text(text = client.subscriptionNumber, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(text = stringResource(R.string.room), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                    Text(text = client.roomNumber ?: "-", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            Spacer(Modifier.height(4.dp))

                            // الباقة
                            Row(Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    Text(text = stringResource(R.string.clients_item_package_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                    Text(text = client.packageType, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }


                            Spacer(Modifier.height(8.dp))

                            // المبلغ + المتبقي + الأزرار
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                        Text(text = currencyFormat.format(monthAmount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    }
                                    if (status == PaymentStatus.PARTIAL || status == PaymentStatus.SETTLED) {
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                            Text(
                                                text = stringResource(R.string.clients_remaining_amount, currencyFormat.format(remaining)),
                                                style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = statusColor
                                            )
                                        }
                                    } else if (status == PaymentStatus.UNPAID) {
                                        Text(text = stringResource(R.string.clients_not_paid_yet), style = MaterialTheme.typography.bodySmall, color = statusColor)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // زر الدفع
                                    when (status) {
                                        PaymentStatus.UNPAID, PaymentStatus.PARTIAL -> {
                                            Button(
                                                onClick = { showPaymentDialog = Triple(client, selectedMonth, monthAmount) },
                                                colors = ButtonDefaults.buttonColors(containerColor = if (status == PaymentStatus.PARTIAL) Color(0xFFFF9800) else Color(0xFF4CAF50)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(2.dp))
                                                Text(
                                                    text = if (status == PaymentStatus.PARTIAL) stringResource(R.string.building_details_complete_payment) else stringResource(R.string.building_details_confirm_payment),
                                                    fontSize = 10.sp, maxLines = 1
                                                )
                                            }
                                        }
                                        PaymentStatus.SETTLED -> { /* لا زر */ }
                                        PaymentStatus.FULL -> {
                                            OutlinedButton(
                                                onClick = { showPaymentDialog = Triple(client, selectedMonth, monthAmount) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(2.dp))
                                                Text(text = stringResource(R.string.building_details_revert_payment), fontSize = 10.sp, maxLines = 1)
                                            }
                                        }
                                    }

                                    // أزرار التعديل والحذف
                                    IconButton(onClick = { onEditClient(client) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.building_details_edit_client), modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { clientToDelete = client; showDeleteClientDialog = true }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.building_details_delete_client), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // لا يوجد عملاء
            if (buildingClients.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.building_details_no_clients_this_month))
                    }
                }
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
                onDismiss = { showViewOptionsDialog = false }
            )
        }

        // حوار إضافة عميل
        if (showAddClientDialog) {
            ClientEditDialog(
                buildingList = listOf(building),
                initialBuildingId = building.id,
                initialStartMonth = selectedMonth,
                initialStartDay = 1,
                initialFirstMonthAmount = "",
                buildingSelectionEnabled = false,
                onSave = { name, subscriptionNumber, price, buildingId, roomNumber, startMonth, startDay, firstMonthAmount, phone, address, packageType, notes ->
                    onAddClient(Client(name = name, subscriptionNumber = subscriptionNumber, price = price, firstMonthAmount = firstMonthAmount, buildingId = buildingId, roomNumber = roomNumber, startMonth = startMonth, startDay = startDay, phone = phone, address = address, packageType = packageType, notes = notes))
                    showAddClientDialog = false
                },
                onDismiss = { showAddClientDialog = false }
            )
        }
    }

    // ===== حوار الدفع =====
    showPaymentDialog?.let { (client, month, monthAmount) ->
        val payment by paymentViewModel.getPaymentLive(client.id, month).observeAsState()
        val isPaid = payment?.isPaid ?: false
        val shouldPay = !isPaid
        val monthUiList by paymentViewModel.getClientMonthPaymentsUi(client.id).observeAsState(emptyList())
        val monthUi = monthUiList.firstOrNull { it.month == month }
        val isPartial = monthUi != null && monthUi.status == PaymentStatus.PARTIAL
        val dialogRemaining = monthUi?.remaining ?: monthAmount

        AlertDialog(
            onDismissRequest = { showPaymentDialog = null },
            icon = { Icon(if (shouldPay) Icons.Filled.CheckCircle else Icons.Filled.Close, contentDescription = null, tint = if (shouldPay) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp)) },
            title = { Text(text = if (shouldPay) stringResource(R.string.building_details_dialog_confirm_title) else stringResource(R.string.building_details_dialog_revert_title), style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    Text(text = if (isPartial) stringResource(R.string.building_details_complete_payment_text, month, dialogRemaining) else if (shouldPay) stringResource(R.string.building_details_dialog_confirm_text, month) else stringResource(R.string.building_details_dialog_revert_text, month))
                    Spacer(Modifier.height(8.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(text = stringResource(R.string.building_details_dialog_client_name, client.name))
                            Text(text = stringResource(R.string.building_details_dialog_month, month))
                            Text(text = stringResource(R.string.building_details_dialog_amount, if (isPartial) dialogRemaining else monthAmount), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    scope.launch { if (shouldPay) paymentViewModel.markFullPayment(client.id, month, monthAmount) else paymentViewModel.markAsUnpaid(client.id, month) }
                    showPaymentDialog = null
                }, colors = ButtonDefaults.buttonColors(containerColor = if (shouldPay) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)) {
                    Text(text = if (shouldPay) stringResource(R.string.building_details_dialog_yes_confirm) else stringResource(R.string.building_details_dialog_yes_revert))
                }
            },
            dismissButton = { OutlinedButton(onClick = { showPaymentDialog = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    // ===== حوار حذف المبنى =====
    if (showDeleteBuildingDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteBuildingDialog = false },
            title = { Text(stringResource(R.string.building_details_delete_building_title)) },
            text = {
                Column {
                    Text(text = stringResource(R.string.building_details_delete_building_text, building.name))
                    if (allClients.any { it.buildingId == building.id }) {
                        Spacer(Modifier.height(8.dp))
                        Text(text = stringResource(R.string.building_details_delete_building_warning), color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { onDeleteBuilding(building); showDeleteBuildingDialog = false; onBack() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text(text = stringResource(R.string.building_details_delete_building_confirm), color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = { OutlinedButton(onClick = { showDeleteBuildingDialog = false }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    // ===== حوار حذف العميل =====
    if (showDeleteClientDialog && clientToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteClientDialog = false; clientToDelete = null },
            title = { Text(stringResource(R.string.building_details_delete_client_title)) },
            text = {
                Column {
                    val c = clientToDelete!!
                    if (selectedMonth == c.startMonth) {
                        Text(text = stringResource(R.string.building_details_delete_client_full_text, c.name))
                        Spacer(Modifier.height(8.dp))
                        Text(text = stringResource(R.string.building_details_delete_client_full_warning), color = MaterialTheme.colorScheme.error)
                    } else {
                        Text(text = stringResource(R.string.building_details_delete_client_stop_text, c.name, selectedMonth))
                        Spacer(Modifier.height(8.dp))
                        Text(text = stringResource(R.string.building_details_delete_client_stop_info, selectedMonth), color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val c = clientToDelete!!
                    if (selectedMonth == c.startMonth) onDeleteClient(c) else onUpdateClient(c.copy(endMonth = selectedMonth))
                    showDeleteClientDialog = false; clientToDelete = null
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text(text = stringResource(R.string.building_details_delete_client_confirm), color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = { OutlinedButton(onClick = { showDeleteClientDialog = false; clientToDelete = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

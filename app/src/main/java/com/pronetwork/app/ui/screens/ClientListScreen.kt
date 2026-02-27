package com.pronetwork.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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
import java.util.Locale
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import com.pronetwork.app.ui.components.SortOption
import androidx.lifecycle.compose.collectAsStateWithLifecycle


// ============ ألوان حالة الدفع ============
private val PaidColor = Color(0xFF4CAF50)
private val PartialColor = Color(0xFFFF9800)
private val SettledColor = Color(0xFF2196F3)
private val UnpaidColor = Color(0xFFF44336)

fun getStatusColor(status: PaymentStatus): Color {
    return when (status) {
        PaymentStatus.FULL -> PaidColor
        PaymentStatus.PARTIAL -> PartialColor
        PaymentStatus.SETTLED -> SettledColor
        PaymentStatus.UNPAID -> UnpaidColor
    }
}

@Composable
fun ClientListScreen(
    clients: List<Client>,
    buildings: List<Building>,
    selectedMonth: String,
    paymentViewModel: PaymentViewModel,
    sortOption: SortOption,
    onAddClient: () -> Unit,
    onClientClick: (Client) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("en", "SA")).apply {
            maximumFractionDigits = 0
        }
    }

    var showPaymentDialog by remember { mutableStateOf<Triple<Client, String, Double>?>(null) }

    Column(Modifier.fillMaxSize()) {
        // الهيدر
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.clients_count_label, clients.size),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = onAddClient,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = stringResource(R.string.clients_add_button),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        if (clients.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = stringResource(R.string.clients_no_clients_in_month))
            }
        } else {
            val listState = rememberLazyListState()
            LaunchedEffect(sortOption) {
                listState.animateScrollToItem(0)
            }
            // جلب حالات الدفع لكل العملاء — Flow تفاعلي يتحدث تلقائياً عند أي تغيير
            val allStatuses by paymentViewModel
                .observeAllClientStatusesForMonth(selectedMonth)
                .collectAsStateWithLifecycle(initialValue = emptyMap())

            val sortedClients = remember(clients, allStatuses, sortOption) {
                when (sortOption) {
                    SortOption.STATUS_UNPAID_FIRST -> clients.sortedBy { client ->
                        when (allStatuses[client.id]) {
                            PaymentStatus.UNPAID -> 0
                            PaymentStatus.PARTIAL -> 1
                            PaymentStatus.SETTLED -> 2
                            PaymentStatus.FULL -> 3
                            null -> 0
                        }
                    }
                    SortOption.STATUS_PAID_FIRST -> clients.sortedBy { client ->
                        when (allStatuses[client.id]) {
                            PaymentStatus.FULL -> 0
                            PaymentStatus.SETTLED -> 1
                            PaymentStatus.PARTIAL -> 2
                            PaymentStatus.UNPAID -> 3
                            null -> 3
                        }
                    }
                    else -> clients
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sortedClients, key = { it.id }) { client ->
                    val buildingName =
                        buildings.firstOrNull { it.id == client.buildingId }?.name
                            ?: stringResource(R.string.clients_building_none)

                    val status = allStatuses[client.id] ?: PaymentStatus.UNPAID

                    val payment by paymentViewModel
                        .getPaymentLive(client.id, selectedMonth)
                        .observeAsState(null)

                    val monthAmount = payment?.amount ?: client.price

                    val monthUiList by paymentViewModel
                        .getClientMonthPaymentsUi(client.id)
                        .observeAsState(emptyList())
                    val monthUi = monthUiList.firstOrNull { it.month == selectedMonth }
                    val totalPaid = monthUi?.totalPaid ?: 0.0
                    val remaining = monthUi?.remaining ?: monthAmount

                    ClientCardItem(
                        client = client,
                        buildingName = buildingName,
                        status = status,
                        monthAmount = monthAmount,
                        totalPaid = totalPaid,
                        remaining = remaining,
                        currencyFormat = currencyFormat,
                        onClientClick = onClientClick,
                        onShowPaymentDialog = {
                            showPaymentDialog = Triple(client, selectedMonth, monthAmount)
                        }
                    )
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState)

        // حوار الدفع
        showPaymentDialog?.let { (client, month, monthAmount) ->
            val paymentState by paymentViewModel
                .getPaymentLive(client.id, month)
                .observeAsState(null)
            val isCurrentlyPaid = paymentState?.isPaid ?: false
            val shouldPay = !isCurrentlyPaid

            val monthUiList by paymentViewModel
                .getClientMonthPaymentsUi(client.id)
                .observeAsState(emptyList())
            val monthUi = monthUiList.firstOrNull { it.month == month }
            val isPartial = monthUi != null && monthUi.status == PaymentStatus.PARTIAL
            val dialogRemaining = monthUi?.remaining ?: monthAmount

            AlertDialog(
                onDismissRequest = { showPaymentDialog = null },
                icon = {
                    Icon(
                        if (shouldPay) Icons.Filled.CheckCircle else Icons.Filled.Close,
                        contentDescription = null,
                        tint = if (shouldPay) MaterialTheme.colorScheme.tertiary
                        else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text(
                        text = if (shouldPay) stringResource(R.string.clients_dialog_confirm_title)
                        else stringResource(R.string.clients_dialog_revert_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column {
                        Text(
                            text = if (isPartial) stringResource(R.string.building_details_complete_payment_text, month, dialogRemaining)
                            else if (shouldPay) stringResource(R.string.clients_dialog_confirm_text, month)
                            else stringResource(R.string.clients_dialog_revert_text, month)
                        )
                        Spacer(Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(text = stringResource(R.string.clients_dialog_client_name, client.name))
                                Text(text = stringResource(R.string.clients_dialog_month, month))
                                Text(
                                    text = stringResource(R.string.clients_dialog_amount, if (isPartial) dialogRemaining else monthAmount),
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
                                    paymentViewModel.markFullPayment(client.id, month, monthAmount)
                                } else {
                                    paymentViewModel.markAsUnpaid(client.id, month)
                                }
                            }
                            showPaymentDialog = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (shouldPay) MaterialTheme.colorScheme.tertiary
                            else MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            text = if (shouldPay) stringResource(R.string.clients_dialog_yes_confirm)
                            else stringResource(R.string.clients_dialog_yes_revert)
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
    }
}

// ============ بطاقة العميل مع الشريط اللوني ============
@Composable
private fun ClientCardItem(
    client: Client,
    buildingName: String,
    status: PaymentStatus,
    monthAmount: Double,
    totalPaid: Double,
    remaining: Double,
    currencyFormat: NumberFormat,
    onClientClick: (Client) -> Unit,
    onShowPaymentDialog: () -> Unit
) {
    val statusColor = getStatusColor(status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // ===== الشريط اللوني الجانبي =====
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(
                        statusColor,
                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    )
            )

            // ===== محتوى البطاقة =====
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClientClick(client) }
                    .padding(12.dp)
            ) {
                // الصف العلوي: الاسم + شارة الحالة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = client.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(status = status)
                }

                Spacer(Modifier.height(8.dp))

                // الصف الأوسط: المبنى + رقم الاشتراك
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoChip(
                        label = stringResource(R.string.clients_item_building_label),
                        value = buildingName,
                        modifier = Modifier.weight(1f)
                    )
                    InfoChip(
                        label = stringResource(R.string.clients_item_subscription_label),
                        value = client.subscriptionNumber,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(4.dp))

                // الصف: الباقة + الغرفة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    InfoChip(
                        label = stringResource(R.string.clients_item_package_label),
                        value = client.packageType,
                        modifier = Modifier.weight(1f)
                    )
                    if (!client.roomNumber.isNullOrBlank()) {
                        InfoChip(
                            label = stringResource(R.string.room),
                            value = client.roomNumber,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Spacer(Modifier.height(10.dp))

                // الصف السفلي: المبالغ + زر الدفع
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // عرض المبالغ
                    Column {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            Text(
                                text = currencyFormat.format(monthAmount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (status == PaymentStatus.PARTIAL || status == PaymentStatus.SETTLED) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                Text(
                                    text = stringResource(R.string.clients_remaining_amount, currencyFormat.format(remaining)),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }
                        } else if (status == PaymentStatus.UNPAID) {
                            Text(
                                text = stringResource(R.string.clients_not_paid_yet),
                                style = MaterialTheme.typography
                                    .bodySmall,
                                color = UnpaidColor
                            )
                        }
                    }
                }

                // زر الدفع
                when (status) {
                    PaymentStatus.UNPAID, PaymentStatus.PARTIAL -> {
                        Button(
                            onClick = onShowPaymentDialog,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (status == PaymentStatus.PARTIAL) PartialColor else PaidColor
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (status == PaymentStatus.PARTIAL)
                                    stringResource(R.string.building_details_complete_payment)
                                else stringResource(R.string.building_details_confirm_payment),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                    PaymentStatus.SETTLED -> {
                        // لا زر - فقط الحالة ظاهرة
                    }
                    PaymentStatus.FULL -> {
                        OutlinedButton(
                            onClick = onShowPaymentDialog,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.building_details_revert_payment),
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============ شارة الحالة ============
@Composable
private fun StatusBadge(status: PaymentStatus) {
    val statusColor = getStatusColor(status)
    val statusText = when (status) {
        PaymentStatus.UNPAID -> stringResource(R.string.clients_status_unpaid)
        PaymentStatus.PARTIAL -> stringResource(R.string.clients_status_partial)
        PaymentStatus.SETTLED -> stringResource(R.string.clients_status_settled)
        PaymentStatus.FULL -> stringResource(R.string.clients_status_full)
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
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = statusColor
            )
        }
    }
}

// ============ InfoChip ============
@Composable
private fun InfoChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

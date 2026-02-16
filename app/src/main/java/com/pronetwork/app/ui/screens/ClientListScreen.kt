package com.pronetwork.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
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

@Composable
fun ClientListScreen(
    clients: List<Client>,
    buildings: List<Building>,
    selectedMonth: String,
    paymentViewModel: PaymentViewModel,
    onAddClient: () -> Unit,
    onClientClick: (Client) -> Unit
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // نحفظ Client + month + monthAmount
    var showPaymentDialog by remember { mutableStateOf<Triple<Client, String, Double>?>(null) }

    Column(Modifier.fillMaxSize()) {
        // الهيدر (عدد العملاء + زر إضافة)
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    R.string.clients_count_label,
                    clients.size
                ),
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

        // باقي المحتوى في LazyColumn قابلة للسكرول
        if (clients.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.clients_no_clients_in_month)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(clients, key = { it.id }) { client ->
                    val buildingName =
                        buildings.firstOrNull { it.id == client.buildingId }?.name
                            ?: stringResource(R.string.clients_building_none)

                    // نقرأ حالة الشهر من ال ViewModel (UNPAID / PARTIAL / FULL)
                    val status by paymentViewModel
                        .getClientMonthStatus(client.id, selectedMonth)
                        .observeAsState(initial = PaymentStatus.UNPAID)

                    // قيمة Payment لعرض المبلغ الصحيح
                    val payment by paymentViewModel
                        .getPaymentLive(client.id, selectedMonth)
                        .observeAsState(null)

                    val monthAmount = payment?.amount ?: client.price

                    ClientCardItem(
                        client = client,
                        buildingName = buildingName,
                        status = status,
                        monthAmount = monthAmount,
                        onClientClick = onClientClick,
                        onShowPaymentDialog = {
                            showPaymentDialog = Triple(client, selectedMonth, monthAmount)
                        }
                    )
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState)

        // حوار الدفع باستخدام Triple
        showPaymentDialog?.let { (client, month, monthAmount) ->
            val paymentState by paymentViewModel
                .getPaymentLive(client.id, month)
                .observeAsState(null)
            val isCurrentlyPaid = paymentState?.isPaid ?: false
            val shouldPay = !isCurrentlyPaid   // إذا غير مدفوع → تأكيد دفع، إذا مدفوع → تراجع

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
                        text = if (shouldPay)
                            stringResource(R.string.clients_dialog_confirm_title)
                        else
                            stringResource(R.string.clients_dialog_revert_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                text = {
                    Column {
                        Text(
                            text = if (shouldPay)
                                stringResource(
                                    R.string.clients_dialog_confirm_text,
                                    month
                                )
                            else
                                stringResource(
                                    R.string.clients_dialog_revert_text,
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
                                        R.string.clients_dialog_client_name,
                                        client.name
                                    )
                                )
                                Text(
                                    text = stringResource(
                                        R.string.clients_dialog_month,
                                        month
                                    )
                                )
                                Text(
                                    text = stringResource(
                                        R.string.clients_dialog_amount,
                                        monthAmount
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
                                stringResource(R.string.clients_dialog_yes_confirm)
                            else
                                stringResource(R.string.clients_dialog_yes_revert)
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

@Composable
private fun ClientCardItem(
    client: Client,
    buildingName: String,
    status: PaymentStatus,
    monthAmount: Double,
    onClientClick: (Client) -> Unit,
    onShowPaymentDialog: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(5.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                PaymentStatus.FULL -> MaterialTheme.colorScheme.secondaryContainer
                PaymentStatus.PARTIAL -> MaterialTheme.colorScheme.surfaceVariant
                PaymentStatus.UNPAID -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClientClick(client) }
            ) {
                Text(
                    text = stringResource(R.string.clients_item_name_label),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = client.name,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.clients_item_building_label),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = buildingName,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.clients_item_subscription_label),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = client.subscriptionNumber,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.clients_item_package_label),
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = client.packageType,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(4.dp))

                val statusText = when (status) {
                    PaymentStatus.UNPAID ->
                        stringResource(R.string.clients_status_unpaid)
                    PaymentStatus.PARTIAL ->
                        stringResource(R.string.clients_status_partial)
                    PaymentStatus.FULL ->
                        stringResource(R.string.clients_status_full)
                }
                val statusColor = when (status) {
                    PaymentStatus.UNPAID -> MaterialTheme.colorScheme.error
                    PaymentStatus.PARTIAL -> MaterialTheme.colorScheme.primary
                    PaymentStatus.FULL -> MaterialTheme.colorScheme.tertiary
                }

                Text(
                    text = stringResource(R.string.clients_status_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor
                )
            }

            Spacer(Modifier.width(8.dp))

            when (status) {
                PaymentStatus.UNPAID, PaymentStatus.PARTIAL -> {
                    Button(
                        onClick = onShowPaymentDialog,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
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

                PaymentStatus.FULL -> {
                    OutlinedButton(
                        onClick = onShowPaymentDialog
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.building_details_revert_payment),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

        }
    }
}

package com.pronetwork.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
    var showPaymentDialog by remember { mutableStateOf<Pair<Client, Boolean>?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "عدد العملاء: ${clients.size}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Button(
                onClick = onAddClient,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("إضافة عميل", color = MaterialTheme.colorScheme.onPrimary)
            }
        }

        if (clients.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("لا يوجد عملاء في هذا الشهر.")
            }
        } else {
            clients.forEach { client ->
                val buildingName = buildings.firstOrNull { it.id == client.buildingId }?.name ?: "بدون مبنى"
                val payment by paymentViewModel
                    .getPaymentLive(client.id, selectedMonth)
                    .observeAsState(null)
                val isPaid = payment?.isPaid ?: false

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onClientClick(client) },
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
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("الاسم: ${client.name}", style = MaterialTheme.typography.titleMedium)
                            Text("المبنى: $buildingName")
                            Text("رقم الاشتراك: ${client.subscriptionNumber}")
                            Text("الباقة: ${client.packageType}")
                            Text(
                                "الحالة: ${if (isPaid) "مدفوع" else "غير مدفوع"}",
                                color = if (isPaid)
                                    MaterialTheme.colorScheme.tertiary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                        }

                        if (!isPaid) {
                            Button(
                                onClick = { showPaymentDialog = client to true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                )
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("تأكيد الدفع", color = MaterialTheme.colorScheme.onTertiary)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { showPaymentDialog = client to false }
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("تراجع", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState)

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
    }
}

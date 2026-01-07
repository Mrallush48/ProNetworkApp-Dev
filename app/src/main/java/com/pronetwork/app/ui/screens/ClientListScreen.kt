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

    // ✅ تعديل: استخدام Triple لتمرير المبلغ الصحيح
    var showPaymentDialog by remember { mutableStateOf<Triple<Client, String, Double>?>(null) }

    Column(Modifier.fillMaxSize()) {
        // الهيدر (عدد العملاء + زر إضافة)
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

        // باقي المحتوى في LazyColumn قابلة للسكرول
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(clients, key = { it.id }) { client ->
                    val buildingName =
                        buildings.firstOrNull { it.id == client.buildingId }?.name ?: "بدون مبنى"

                    // ✅ تعديل: جلب المبلغ من Payment (إذا موجود)
                    val payment by paymentViewModel
                        .getPaymentLive(client.id, selectedMonth)
                        .observeAsState(null)
                    val isPaid = payment?.isPaid ?: false
                    val monthAmount = payment?.amount ?: client.price  // ✅ استخدام المبلغ من Payment أولاً

                    ClientCardItem(
                        client = client,
                        buildingName = buildingName,
                        isPaid = isPaid,
                        monthAmount = monthAmount,      // ✅ تمرير المبلغ الصحيح
                        onClientClick = onClientClick,
                        onShowPaymentDialog = { shouldPay ->
                            // ✅ تعديل: إرسال المبلغ الصحيح مع Triple
                            showPaymentDialog = Triple(client, selectedMonth, monthAmount)
                        }
                    )
                }
            }
        }

        SnackbarHost(hostState = snackbarHostState)

        // ✅ تعديل: حوار الدفع باستخدام Triple
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
                                    "المبلغ: $monthAmount ريال",     // ✅ استخدام المبلغ الصحيح
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
    }
}

@Composable
private fun ClientCardItem(
    client: Client,
    buildingName: String,
    isPaid: Boolean,
    monthAmount: Double,              // ✅ معلمة جديدة
    onClientClick: (Client) -> Unit,
    onShowPaymentDialog: (Boolean) -> Unit // true = تأكيد, false = تراجع
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onClientClick(client) }
            ) {
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

            Spacer(Modifier.width(8.dp))

            if (!isPaid) {
                Button(
                    onClick = { onShowPaymentDialog(true) },
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
                    onClick = { onShowPaymentDialog(false) }
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("تراجع", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
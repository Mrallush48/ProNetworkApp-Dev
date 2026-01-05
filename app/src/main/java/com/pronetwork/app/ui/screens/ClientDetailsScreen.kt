package com.pronetwork.app.ui.screens

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import com.pronetwork.app.data.Client
import com.pronetwork.app.data.Payment
import com.pronetwork.app.data.PaymentTransaction
import com.pronetwork.app.viewmodel.ClientMonthPaymentUi
import com.pronetwork.app.viewmodel.PaymentStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailsScreen(
    client: Client,
    buildingName: String,
    payments: List<Payment>,
    monthUiList: List<ClientMonthPaymentUi>,
    onEdit: (Client) -> Unit,
    onDelete: (Client) -> Unit,
    onTogglePayment: (String, Boolean) -> Unit,           // دفع كامل / تراجع
    onPartialPaymentRequest: (String, Double) -> Unit,    // دفع جزئي (month, amount)
    getMonthTransactions: (String) -> LiveData<List<PaymentTransaction>>,
    onBack: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // للحوار الخاص بالدفع الجزئي
    var partialPaymentMonth by remember { mutableStateOf<String?>(null) }
    var partialPaymentAmountText by remember { mutableStateOf("") }

    // للحوار الخاص بتأكيد الدفع الكامل
    var confirmFullPaymentMonth by remember { mutableStateOf<String?>(null) }

    // للحوار الخاص بتأكيد التراجع عن الدفع
    var confirmCancelPaymentMonth by remember { mutableStateOf<String?>(null) }

    // للحوار الخاص بحذف الحركة
    var transactionToDelete by remember { mutableStateOf<PaymentTransaction?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل العميل") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // معلومات العميل
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "معلومات العميل",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("الاسم:", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    client.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text("رقم الاشتراك:", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    client.subscriptionNumber,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(16.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("المبنى:", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(buildingName, style = MaterialTheme.typography.bodyLarge)
                            }
                            Column(Modifier.weight(1f)) {
                                Text("الباقة:", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(client.packageType, style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("السعر الشهري:", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "${client.price} ريال",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text("شهر البداية:", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    formatYearMonth(client.startMonth),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        if (!client.roomNumber.isNullOrEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Text("رقم الغرفة:", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(client.roomNumber, style = MaterialTheme.typography.bodyLarge)
                        }

                        if (client.phone.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(16.dp))
                            Text("رقم الجوال:", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(client.phone, style = MaterialTheme.typography.bodyLarge)
                        }

                        if (client.address.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Text("العنوان:", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(client.address, style = MaterialTheme.typography.bodyLarge)
                        }

                        if (client.notes.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(16.dp))
                            Text("ملاحظات:", style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Text(
                                    client.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // أزرار التحكم
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onEdit(client) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("تعديل")
                    }

                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("حذف")
                    }
                }
            }

            // سجل المدفوعات باستخدام monthUiList
            if (monthUiList.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "سجل المدفوعات الشهرية",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val paidCount = monthUiList.count { it.status == PaymentStatus.FULL }
                            Text(
                                "$paidCount/${monthUiList.size}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                items(monthUiList) { item ->
                    val month = item.month
                    val isPaidFull = item.status == PaymentStatus.FULL
                    val isPartial = item.status == PaymentStatus.PARTIAL
                    val monthAmount = item.monthAmount

                    val transactionsLiveData = remember(month) {
                        getMonthTransactions(month)
                    }
                    val transactions by transactionsLiveData.observeAsState(emptyList())

                    val statusText: String
                    val statusColor = when {
                        isPaidFull -> {
                            statusText = "مدفوع بالكامل (${item.totalPaid} / ${item.monthAmount})"
                            MaterialTheme.colorScheme.tertiary
                        }
                        isPartial -> {
                            statusText = "مدفوع جزئيًا (${item.totalPaid} / ${item.monthAmount})، المتبقي ${item.remaining} ريال"
                            MaterialTheme.colorScheme.secondary
                        }
                        else -> {
                            statusText = "غير مدفوع (${item.monthAmount} ريال)"
                            MaterialTheme.colorScheme.error
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                isPaidFull -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                                isPartial -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                formatYearMonth(month),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))

                            Text(
                                statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColor
                            )

                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${item.monthAmount} ريال",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (transactions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "حركات هذا الشهر:",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                transactions.forEach { tx: PaymentTransaction ->
                                    val dateText = remember(tx.id) {
                                        DateFormat.format("yyyy-MM-dd HH:mm", tx.date).toString()
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "- ${tx.amount} ريال (${tx.notes ?: ""}) - $dateText",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )

                                        TextButton(
                                            onClick = {
                                                transactionToDelete = tx
                                            }
                                        ) {
                                            Text(
                                                text = "حذف",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isPaidFull) {
                                    // عندما يكون مدفوع بالكامل: نعرض زر تراجع عن الدفع
                                    OutlinedButton(
                                        onClick = { confirmCancelPaymentMonth = month },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("تراجع عن الدفع")
                                    }
                                } else {
                                    // عندما لا يكون مدفوع بالكامل: تأكيد كامل + دفع جزئي
                                    Button(
                                        onClick = { confirmFullPaymentMonth = month },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("تأكيد الدفع الكامل")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            partialPaymentMonth = month
                                            partialPaymentAmountText = ""
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("دفع جزئي")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }

        // حوار تأكيد حذف الحركة
        if (transactionToDelete != null) {
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                title = { Text("تأكيد الحذف") },
                text = {
                    val tx = transactionToDelete!!
                    val dateText = DateFormat.format("yyyy-MM-dd HH:mm", tx.date).toString()
                    Text(
                        "هل أنت متأكد من حذف هذه الحركة؟\n" +
                                "- ${tx.amount} ريال (${tx.notes ?: ""}) - $dateText"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // استدعاء ViewModel لحذف الحركة
                            // paymentViewModel.deleteTransaction(transactionToDelete!!.id)
                            transactionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("نعم، احذف")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { transactionToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // حوار الدفع الجزئي
        if (partialPaymentMonth != null) {
            AlertDialog(
                onDismissRequest = { partialPaymentMonth = null },
                title = { Text("دفع جزئي - ${formatYearMonth(partialPaymentMonth!!)}") },
                text = {
                    Column {
                        Text("أدخل المبلغ الذي دفعه العميل لهذا الشهر:")
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = partialPaymentAmountText,
                            onValueChange = { partialPaymentAmountText = it },
                            singleLine = true,
                            label = { Text("المبلغ بالريال") }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "السعر الشهري: ${client.price} ريال",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val month = partialPaymentMonth
                            val amount = partialPaymentAmountText.toDoubleOrNull()
                            if (month != null && amount != null && amount > 0.0) {
                                onPartialPaymentRequest(month, amount)
                                partialPaymentMonth = null
                            }
                        }
                    ) {
                        Text("تأكيد الدفع الجزئي")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { partialPaymentMonth = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // حوار تأكيد الدفع الكامل
        if (confirmFullPaymentMonth != null) {
            val month = confirmFullPaymentMonth!!
            AlertDialog(
                onDismissRequest = { confirmFullPaymentMonth = null },
                title = { Text("تأكيد الدفع الكامل") },
                text = {
                    Text(
                        "هل أنت متأكد من تأكيد الدفع الكامل لشهر ${formatYearMonth(month)} " +
                                "بقيمة ${client.price} ريال؟"
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onTogglePayment(month, true)
                            confirmFullPaymentMonth = null
                        }
                    ) {
                        Text("نعم، تأكيد")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { confirmFullPaymentMonth = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // حوار التراجع عن الدفع الكامل
        if (confirmCancelPaymentMonth != null) {
            val month = confirmCancelPaymentMonth!!
            AlertDialog(
                onDismissRequest = { confirmCancelPaymentMonth = null },
                title = { Text("تراجع عن الدفع") },
                text = {
                    Text(
                        "هل أنت متأكد من التراجع عن دفع شهر ${formatYearMonth(month)}؟\n" +
                                "سيتم اعتبار الشهر غير مدفوع، وسيتم حذف جميع حركات الدفع لهذا الشهر."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            // نرسل shouldPay = false ليتم استدعاء markAsUnpaid في الـ ViewModel
                            onTogglePayment(month, false)
                            confirmCancelPaymentMonth = null
                        }
                    ) {
                        Text("نعم، تراجع")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { confirmCancelPaymentMonth = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                icon = {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text("تأكيد الحذف", fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "هل أنت متأكد من حذف العميل \"${client.name}\"؟\n\n" +
                                "سيتم حذف جميع سجلات المدفوعات المرتبطة بهذا العميل.\n\n" +
                                "لا يمكن التراجع عن هذا الإجراء."
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDelete(client)
                            showDeleteDialog = false
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
                    OutlinedButton(onClick = { showDeleteDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

private fun formatYearMonth(yearMonth: String): String {
    return try {
        val (year, month) = yearMonth.split("-").map { it.toInt() }
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("ar"))
        monthFormat.format(calendar.time)
    } catch (e: Exception) {
        yearMonth
    }
}
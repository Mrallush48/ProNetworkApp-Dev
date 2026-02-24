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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import com.pronetwork.app.R
import com.pronetwork.app.data.Client
import com.pronetwork.app.data.Payment
import com.pronetwork.app.data.PaymentTransaction
import com.pronetwork.app.viewmodel.ClientMonthPaymentUi
import com.pronetwork.app.viewmodel.PaymentStatus
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.pronetwork.app.network.ApprovalHelper
import com.pronetwork.app.network.AuthManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailsScreen(
    client: Client,
    buildingName: String,
    payments: List<Payment>,
    monthUiList: List<ClientMonthPaymentUi>,
    onEdit: (Client) -> Unit,
    onDelete: (Client) -> Unit,
    onTogglePayment: (month: String, monthAmount: Double, shouldPay: Boolean) -> Unit,
    onPartialPaymentRequest: (month: String, monthAmount: Double, partialAmount: Double) -> Unit,
    getMonthTransactions: (String) -> LiveData<List<PaymentTransaction>>,
    onDeleteTransaction: (Int) -> Unit,
    onAddReverseTransaction: (month: String, monthAmount: Double, refundAmount: Double, reason: String) -> Unit,
    onBack: () -> Unit
) {

    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val appContext = remember { context.applicationContext }
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf(false) }

    var partialPaymentMonth by remember { mutableStateOf<String?>(null) }
    var partialPaymentAmountText by remember { mutableStateOf("") }
    var partialPaymentMonthAmount by remember { mutableStateOf(0.0) }

    var confirmFullPaymentMonth by remember { mutableStateOf<String?>(null) }
    var confirmFullPaymentMonthAmount by remember { mutableStateOf(0.0) }

    var confirmCancelPaymentMonth by remember { mutableStateOf<String?>(null) }
    var confirmCancelPaymentMonthAmount by remember { mutableStateOf(0.0) }

    var deleteTransactionId by remember { mutableStateOf<Int?>(null) }

    var reverseMonth by remember { mutableStateOf<String?>(null) }
    var reverseAmountText by remember { mutableStateOf("") }
    var reverseReasonText by remember { mutableStateOf("") }
    var reverseMaxAmount by remember { mutableStateOf(0.0) }
    var reverseMonthAmount by remember { mutableStateOf(0.0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.client_details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )

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
                            text = stringResource(R.string.client_details_info_title),
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
                                Text(
                                    text = stringResource(R.string.client_details_name_label),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = client.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.client_details_subscription_label),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = client.subscriptionNumber,
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
                                Text(
                                    text = stringResource(R.string.client_details_building_label),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = buildingName,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.client_details_package_label),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = client.packageType,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.client_details_monthly_price_label),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = stringResource(
                                        R.string.client_details_monthly_price_value,
                                        client.price
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.client_details_start_month_label),
                                    style = MaterialTheme.typography.labelMedium
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = formatYearMonth(client.startMonth),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        if (!client.roomNumber.isNullOrEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.client_details_room_label),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = client.roomNumber,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        if (client.phone.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.client_details_phone_label),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = client.phone,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        if (client.address.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.client_details_address_label),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = client.address,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        if (client.notes.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.client_details_notes_label),
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Text(
                                    text = client.notes,
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
                        Text(stringResource(R.string.client_details_edit_button))
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
                        Text(stringResource(R.string.client_details_delete_button))
                    }
                }
            }

            // سجل المدفوعات الشهرية
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
                                text = stringResource(R.string.client_details_monthly_history_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val paidCount = monthUiList.count { it.status == PaymentStatus.FULL }
                            Text(
                                text = stringResource(
                                    R.string.client_details_monthly_history_counter,
                                    paidCount,
                                    monthUiList.size
                                ),
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

                    val transactions by getMonthTransactions(month).observeAsState(emptyList())

                    val statusText: String
                    val statusColor = when {
                        isPaidFull -> {
                            statusText = stringResource(
                                R.string.client_details_status_full,
                                item.totalPaid,
                                item.monthAmount
                            )
                            MaterialTheme.colorScheme.tertiary
                        }
                        isPartial -> {
                            statusText = stringResource(
                                R.string.client_details_status_partial,
                                item.totalPaid,
                                item.monthAmount,
                                item.remaining
                            )
                            MaterialTheme.colorScheme.secondary
                        }
                        else -> {
                            statusText = stringResource(
                                R.string.client_details_status_unpaid,
                                item.monthAmount
                            )
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
                                text = formatYearMonth(month),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall,
                                color = statusColor
                            )

                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = stringResource(
                                    R.string.client_details_month_amount_value,
                                    item.monthAmount
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (transactions.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(
                                        R.string.client_details_month_transactions_title
                                    ),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                transactions.forEach { tx: PaymentTransaction ->
                                    val dateText = DateFormat.format(
                                        "yyyy-MM-dd HH:mm",
                                        tx.date
                                    ).toString()

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.client_details_month_transaction_line,
                                                tx.amount,
                                                tx.notes ?: "",
                                                dateText
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (tx.amount > 0.0) {
                                            TextButton(
                                                onClick = {
                                                    deleteTransactionId = tx.id
                                                }
                                            ) {
                                                Text(
                                                    text = stringResource(
                                                        R.string.client_details_month_transaction_delete
                                                    ),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
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
                                    OutlinedButton(
                                        onClick = {
                                            confirmCancelPaymentMonth = month
                                            confirmCancelPaymentMonthAmount = monthAmount
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            stringResource(
                                                R.string.client_details_action_cancel_payment
                                            )
                                        )
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            confirmFullPaymentMonth = month
                                            confirmFullPaymentMonthAmount = monthAmount
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            stringResource(
                                                R.string.client_details_action_confirm_full_payment
                                            )
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            partialPaymentMonth = month
                                            partialPaymentAmountText = ""
                                            partialPaymentMonthAmount = monthAmount
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            stringResource(
                                                R.string.client_details_action_partial_payment
                                            )
                                        )
                                    }
                                }

                                if (item.totalPaid > 0.0) {
                                    OutlinedButton(
                                        onClick = {
                                            reverseMonth = month
                                            reverseAmountText = ""
                                            reverseReasonText = ""
                                            reverseMaxAmount = item.totalPaid
                                            reverseMonthAmount = monthAmount
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            stringResource(
                                                R.string.client_details_action_refund
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }

        // حوار الدفع الجزئي
        if (partialPaymentMonth != null) {
            val month = partialPaymentMonth!!
            AlertDialog(
                onDismissRequest = { partialPaymentMonth = null },
                title = {
                    Text(
                        stringResource(
                            R.string.client_details_partial_title,
                            formatYearMonth(month)
                        )
                    )
                },
                text = {
                    Column {
                        Text(stringResource(R.string.client_details_partial_text))
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = partialPaymentAmountText,
                            onValueChange = { partialPaymentAmountText = it },
                            singleLine = true,
                            label = {
                                Text(
                                    stringResource(
                                        R.string.client_details_partial_amount_label
                                    )
                                )
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.client_details_partial_month_price,
                                partialPaymentMonthAmount
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = partialPaymentAmountText.toDoubleOrNull()
                            if (amount != null && amount > 0.0) {
                                onPartialPaymentRequest(
                                    month,
                                    partialPaymentMonthAmount,
                                    amount
                                )
                                partialPaymentMonth = null
                            }
                        }
                    ) {
                        Text(
                            stringResource(
                                R.string.client_details_partial_confirm
                            )
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { partialPaymentMonth = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // حوار تأكيد الدفع الكامل
        if (confirmFullPaymentMonth != null) {
            val month = confirmFullPaymentMonth!!
            AlertDialog(
                onDismissRequest = { confirmFullPaymentMonth = null },
                title = {
                    Text(
                        stringResource(
                            R.string.client_details_full_confirm_title
                        )
                    )
                },
                text = {
                    Text(
                        stringResource(
                            R.string.client_details_full_confirm_text,
                            formatYearMonth(month),
                            confirmFullPaymentMonthAmount
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onTogglePayment(
                                month,
                                confirmFullPaymentMonthAmount,
                                true
                            )
                            confirmFullPaymentMonth = null
                        }
                    ) {
                        Text(
                            stringResource(
                                R.string.client_details_full_confirm_yes
                            )
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { confirmFullPaymentMonth = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // حوار التراجع عن الدفع
        if (confirmCancelPaymentMonth != null) {
            val month = confirmCancelPaymentMonth!!
            AlertDialog(
                onDismissRequest = { confirmCancelPaymentMonth = null },
                title = {
                    Text(
                        stringResource(
                            R.string.client_details_cancel_confirm_title
                        )
                    )
                },
                text = {
                    Text(
                        stringResource(
                            R.string.client_details_cancel_confirm_text,
                            formatYearMonth(month)
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onTogglePayment(
                                month,
                                confirmCancelPaymentMonthAmount,
                                false
                            )
                            confirmCancelPaymentMonth = null
                        }
                    ) {
                        Text(
                            stringResource(
                                R.string.client_details_cancel_confirm_yes
                            )
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { confirmCancelPaymentMonth = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // حوار حذف الحركة
        if (deleteTransactionId != null) {
            AlertDialog(
                onDismissRequest = { deleteTransactionId = null },
                title = {
                    Text(
                        stringResource(
                            R.string.client_details_delete_tx_title
                        )
                    )
                },
                text = {
                    Text(
                        stringResource(
                            R.string.client_details_delete_tx_text
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val id = deleteTransactionId
                            if (id != null) {
                                onDeleteTransaction(id)
                            }
                            deleteTransactionId = null
                        }
                    ) {
                        Text(
                            stringResource(
                                R.string.client_details_delete_tx_yes
                            )
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { deleteTransactionId = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // حوار الاسترجاع
        if (reverseMonth != null) {
            val month = reverseMonth!!
            AlertDialog(
                onDismissRequest = { reverseMonth = null },
                title = {
                    Text(
                        stringResource(
                            R.string.client_details_refund_title,
                            formatYearMonth(month)
                        )
                    )
                },
                text = {
                    Column {
                        Text(
                            stringResource(
                                R.string.client_details_refund_text
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = reverseAmountText,
                            onValueChange = { reverseAmountText = it },
                            singleLine = true,
                            label = {
                                Text(
                                    stringResource(
                                        R.string.client_details_refund_amount_label
                                    )
                                )
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = reverseReasonText,
                            onValueChange = { reverseReasonText = it },
                            singleLine = false,
                            label = {
                                Text(
                                    stringResource(
                                        R.string.client_details_refund_reason_label
                                    )
                                )
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(
                                R.string.client_details_refund_max,
                                reverseMaxAmount
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        if (reverseReasonText.isBlank()) {
                            Text(
                                text = stringResource(
                                    R.string.client_details_refund_reason_required
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    val canConfirm = reverseAmountText.toDoubleOrNull() != null &&
                            reverseAmountText.toDouble() > 0.0 &&
                            reverseAmountText.toDouble() <= reverseMaxAmount &&
                            reverseReasonText.isNotBlank()

                    Button(
                        onClick = {
                            val amount = reverseAmountText.toDoubleOrNull()
                            if (amount != null &&
                                amount > 0.0 &&
                                amount <= reverseMaxAmount &&
                                reverseReasonText.isNotBlank()
                            ) {
                                val reason = reverseReasonText.trim()
                                onAddReverseTransaction(
                                    month,
                                    reverseMonthAmount,
                                    amount,
                                    reason
                                )
                                reverseMonth = null
                            }
                        },
                        enabled = canConfirm
                    ) {
                        Text(
                            stringResource(
                                R.string.client_details_refund_confirm
                            )
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { reverseMonth = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        // حوار حذف العميل
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
                    Text(
                        text = stringResource(R.string.client_details_delete_client_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        if (authManager.isAdmin()) {
                            Text(
                                text = stringResource(
                                    R.string.client_details_delete_client_text,
                                    client.name
                                )
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    R.string.approval_delete_client_warning,
                                    client.name
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.approval_request_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            ApprovalHelper.executeOrRequest(
                                context = context,
                                authManager = authManager,
                                scope = scope,
                                requestType = "DELETE_CLIENT",
                                targetId = client.id,
                                targetName = client.name,
                                onAdminDirect = {
                                    onDelete(client)
                                    showDeleteDialog = false
                                    onBack()
                                },
                                onRequestSent = {
                                    showDeleteDialog = false
                                    Toast.makeText(
                                        appContext,
                                        appContext.getString(R.string.approval_request_sent),
                                        Toast.LENGTH_LONG
                                    ).show()
                                },
                                onError = { error ->
                                    Toast.makeText(
                                        appContext,
                                        appContext.getString(R.string.approval_request_error, error),
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text(
                            if (authManager.isAdmin())
                                stringResource(R.string.client_details_delete_client_yes)
                            else
                                stringResource(R.string.approval_send_request)
                        )
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
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
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        monthFormat.format(calendar.time)
    } catch (e: Exception) {
        yearMonth
    }
}

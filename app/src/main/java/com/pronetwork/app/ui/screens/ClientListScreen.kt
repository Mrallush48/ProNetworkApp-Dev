package com.pronetwork.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pronetwork.app.data.Client
import com.pronetwork.app.viewmodel.ClientViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientListScreen(
    viewModel: ClientViewModel,
    buildingId: Int,
    selectedMonthIso: String,
    onMonthChanged: (String) -> Unit,
    onBack: () -> Unit
) {
    // build last 12 months list as pairs (iso, label)
    fun last12Months(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val cal = Calendar.getInstance()
        val labelFmt = SimpleDateFormat("MMMM yyyy", Locale("ar"))
        val isoFmt = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        for (i in 0 until 12) {
            val iso = isoFmt.format(cal.time)
            val label = labelFmt.format(cal.time)
            list.add(Pair(iso, label))
            cal.add(Calendar.MONTH, -1)
        }
        return list
    }

    val months = remember { last12Months() } // recent 12 months
    var expanded by remember { mutableStateOf(false) }
    var currentIso by remember { mutableStateOf(selectedMonthIso) }
    // update VM when building or month changes
    LaunchedEffect(buildingId, currentIso) {
        viewModel.setBuildingAndMonth(buildingId, currentIso)
        onMonthChanged(currentIso)
    }

    val clients by viewModel.clients.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Client?>(null) }
    var showDeleteConfirmForClient by remember { mutableStateOf<Client?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // totals
    val totalPaid = clients.filter { it.isPaid }.sumOf { it.price }
    val totalUnpaid = clients.filter { !it.isPaid }.sumOf { it.price }
    val countPaid = clients.count { it.isPaid }
    val countUnpaid = clients.count { !it.isPaid }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("عملاء المبنى") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showEditDialog = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "إضافة")
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(12.dp)
        ) {
            // Dropdown for months
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                val label = months.find { it.first == currentIso }?.second ?: currentIso
                OutlinedTextField(
                    value = label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("اختر الشهر") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    months.forEach { (iso, lbl) ->
                        DropdownMenuItem(text = { Text(lbl) }, onClick = {
                            currentIso = iso
                            expanded = false
                        })
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    viewModel.searchClients(it)
                },
                label = { Text("ابحث بالاسم أو رقم الاشتراك") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            val filtered = if (searchQuery.isBlank()) clients else clients.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.subscriptionNumber.contains(searchQuery, ignoreCase = true)
            }

            if (filtered.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا يوجد عملاء لهذا الشهر")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.id }) { client ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editing = client
                                    showEditDialog = true
                                },
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(client.name, style = MaterialTheme.typography.titleMedium)
                                    Text("اشتراك: ${client.subscriptionNumber}", style = MaterialTheme.typography.bodySmall)
                                    client.mobile?.let { Text("جوال: $it", style = MaterialTheme.typography.bodySmall) }
                                    Text("السعر: ${client.price}", style = MaterialTheme.typography.bodySmall)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Button(
                                        onClick = { viewModel.togglePaid(client) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (client.isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text(if (client.isPaid) "مدفوع" else "لم يُدفع")
                                    }

                                    Row {
                                        IconButton(onClick = { editing = client; showEditDialog = true }) {
                                            Icon(Icons.Default.Edit, contentDescription = "تعديل")
                                        }
                                        IconButton(onClick = { showDeleteConfirmForClient = client }) {
                                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "إجمالي المدفوع: $totalPaid ر.س (عدد: $countPaid)")
                    Text(text = "إجمالي غير المدفوع: $totalUnpaid ر.س (عدد: $countUnpaid)")
                }
            }
        }
    }

    // delete confirm dialog for client
    if (showDeleteConfirmForClient != null) {
        val c = showDeleteConfirmForClient!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmForClient = null },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل أنت متأكد أنك تريد حذف العميل \"${c.name}\"؟ لا يمكن التراجع.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteClient(c)
                    showDeleteConfirmForClient = null
                }) {
                    Text("نعم، احذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmForClient = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // edit/add dialog
    if (showEditDialog) {
        ClientEditDialog(
            client = editing,
            selectedMonthIso = currentIso,
            buildingId = buildingId,
            onDismiss = { showEditDialog = false },
            onSave = { client ->
                if (client.id == 0) viewModel.insertClient(client) else viewModel.updateClient(client)
                showEditDialog = false
            },
            onDelete = {
                editing?.let { viewModel.deleteClient(it) }
                showEditDialog = false
            }
        )
    }
}

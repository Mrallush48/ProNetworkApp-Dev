package com.pronetwork.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pronetwork.app.data.Building
import com.pronetwork.app.data.Client
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingDetailsScreen(
    building: Building,
    allClients: List<Client>,
    monthOptions: List<String>,
    onAddClient: (Client) -> Unit,
    onEditClient: (Client) -> Unit,
    onDeleteClient: (Client) -> Unit,
    onTogglePaid: (Client, Boolean) -> Unit,
    onUndoPaid: (Client) -> Unit,
    onEditBuilding: (Building) -> Unit,
    onDeleteBuilding: (Building) -> Unit,
    onBack: () -> Unit
) {
    var showAddClientDialog by remember { mutableStateOf(false) }
    var selectedClient by remember { mutableStateOf<Client?>(null) }
    var showEditClientDialog by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf(monthOptions.first()) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }
    var showDeleteBuildingDialog by remember { mutableStateOf(false) }

    // --- حالات جديدة لتأكيد الحذف ---
    var showDeleteClientDialog by remember { mutableStateOf(false) }
    var clientToDelete by remember { mutableStateOf<Client?>(null) }

    val buildingClients = allClients.filter { client ->
        client.buildingId == building.id &&
                // Apply same month visibility logic as main screen
                try {
                    val clientStartMonth = client.startMonth
                    val currentViewMonth = selectedMonth

                    // Parse both months for comparison
                    val clientDate = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(clientStartMonth)
                    val viewDate = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(currentViewMonth)

                    // Check if client should be visible in this month
                    if (viewDate != null && clientDate != null && viewDate.time >= clientDate.time) {
                        // If client has an end month, check if view month is before end month
                        if (client.endMonth != null) {
                            val endDate = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(client.endMonth)
                            endDate != null && viewDate.time < endDate.time
                        } else {
                            true // No end month, show indefinitely
                        }
                    } else {
                        false
                    }
                } catch (_: Exception) {
                    // Fallback to exact match if parsing fails
                    client.startMonth == selectedMonth
                }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(building.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
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
        Column(Modifier.padding(padding).padding(16.dp)) {
            // Building information card
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("معلومات المبنى", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))

                    if (building.location.isNotEmpty()) {
                        Text("الموقع:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(building.location, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                    }

                    if (building.notes.isNotEmpty()) {
                        Text("ملاحظات:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(building.notes, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(8.dp))
                    }

                    if (building.floors > 0) {
                        Text("عدد الطوابق:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${building.floors}", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                    }

                    if (building.managerName.isNotEmpty()) {
                        Text("اسم المدير:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(building.managerName, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                    }

                    // Action buttons
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onEditBuilding(building) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("تعديل المبنى")
                        }

                        OutlinedButton(
                            onClick = { showDeleteBuildingDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("حذف المبنى")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            // قائمة منسدلة للشهر
            Box(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
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
                    modifier = Modifier.fillMaxWidth().clickable { monthDropdownExpanded = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.primaryContainer,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
            Text("عملاء المبنى في $selectedMonth:", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (buildingClients.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا يوجد عملاء في هذا الشهر.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                buildingClients.forEach { client ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable { selectedClient = client },
                        elevation = CardDefaults.cardElevation(5.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (client.isPaid) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("اسم: ${client.name}", style = MaterialTheme.typography.titleMedium)
                                Text("رقم الاشتراك: ${client.subscriptionNumber}", style = MaterialTheme.typography.bodySmall)
                                Text("رقم الجوال: ${client.phone}", style = MaterialTheme.typography.bodySmall)
                                Text("الحالة: ${if (client.isPaid) "مدفوع" else "غير مدفوع"}", style = MaterialTheme.typography.bodySmall)
                            }
                            // === التعديل هنا: الأزرار في صف واحد مع تأكيد الحذف ===
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!client.isPaid) {
                                    Button(
                                        onClick = { onTogglePaid(client, true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("تأكيد الدفع", color = MaterialTheme.colorScheme.onTertiary)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onUndoPaid(client) }
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("تراجع", color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = {
                                    // عند الضغط على زر الحذف، نحفظ العميل ونظهر نافذة التأكيد
                                    clientToDelete = client
                                    showDeleteClientDialog = true
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف العميل", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
            if (showAddClientDialog) {
                ClientEditDialog(
                    buildingList = listOf(building),
                    initialBuildingId = building.id,
                    initialStartMonth = selectedMonth,
                    buildingSelectionEnabled = false,
                    onSave = { name, subscriptionNumber, price, buildingId, startMonth, phone, address, packageType, notes ->
                        onAddClient(
                            Client(
                                name = name,
                                subscriptionNumber = subscriptionNumber,
                                price = price,
                                buildingId = buildingId,
                                startMonth = startMonth,
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
            selectedClient?.let { client ->
                ClientDetailsScreen(
                    client = client,
                    buildingName = building.name,
                    onEdit = {
                        showEditClientDialog = true
                    },
                    onDelete = {
                        onDeleteClient(client)
                        selectedClient = null
                    },
                    onTogglePaid = { paid ->
                        onTogglePaid(client, paid)
                    },
                    onUndoPaid = {
                        onUndoPaid(client)
                    },
                    onBack = { selectedClient = null }
                )
                if (showEditClientDialog) {
                    ClientEditDialog(
                        buildingList = listOf(building),
                        initialName = client.name,
                        initialSubscriptionNumber = client.subscriptionNumber,
                        initialPrice = client.price.toString(),
                        initialBuildingId = client.id,
                        initialStartMonth = client.startMonth,
                        initialPhone = client.phone,
                        initialAddress = client.address,
                        initialPackageType = client.packageType,
                        initialNotes = client.notes,
                        buildingSelectionEnabled = false,
                        onSave = { name, subscriptionNumber, price, buildingId, startMonth, phone, address, packageType, notes ->
                            onEditClient(
                                client.copy(
                                    name = name,
                                    subscriptionNumber = subscriptionNumber,
                                    price = price,
                                    buildingId = buildingId,
                                    startMonth = startMonth,
                                    phone = phone,
                                    address = address,
                                    packageType = packageType,
                                    notes = notes
                                )
                            )
                            showEditClientDialog = false
                            selectedClient = null
                        },
                        onDismiss = { showEditClientDialog = false }
                    )
                }
            }
        }

        // Building delete confirmation dialog
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
                                "تحذير: يوجد عملاء مرتبطين بهذا المبنى. سيتم حذف جميع العملاء المرتبطين أيضاً.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
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
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

        // === الجديد: مربع تأكيد حذف العميل (تم تصحيح الخطأ النهائي) ===
        clientToDelete?.let { client ->
            AlertDialog(
                onDismissRequest = {
                    showDeleteClientDialog = false
                    clientToDelete = null
                },
                title = { Text("تأكيد حذف العميل") },
                text = {
                    Text("هل أنت متأكد من حذف العميل \"${client.name}\"؟ لا يمكن التراجع عن هذا الإجراء.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteClient(client) // تنفيذ الحذف الفعلي
                            showDeleteClientDialog = false // إغلاق النافذة
                            clientToDelete = null // مسح العميل المحدد
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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
}
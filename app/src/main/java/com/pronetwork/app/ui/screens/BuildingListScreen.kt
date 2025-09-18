package com.pronetwork.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pronetwork.app.data.Building
import com.pronetwork.app.viewmodel.BuildingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildingListScreen(
    viewModel: BuildingViewModel,
    onBuildingSelected: (Building) -> Unit
) {
    val buildings by viewModel.buildings.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Building?>(null) }

    // ✅ حالة لتأكيد الحذف
    var showDeleteDialog by remember { mutableStateOf(false) }
    var buildingToDelete by remember { mutableStateOf<Building?>(null) }

    // ✅ نافذة تأكيد الحذف
    if (showDeleteDialog && buildingToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل أنت متأكد أنك تريد حذف هذا المبنى؟ سيتم حذف جميع العملاء المرتبطين به ولا يمكن التراجع.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteBuilding(buildingToDelete!!)
                        buildingToDelete = null
                    }
                ) {
                    Text("حذف", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("قائمة المباني") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                showDialog = true
            }) { Icon(Icons.Default.Add, contentDescription = "إضافة") }
        }
    ) { padding ->
        if (buildings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("لا توجد مباني حالياً")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(buildings) { building ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBuildingSelected(building) },
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(building.name, style = MaterialTheme.typography.titleMedium)
                                building.description?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Row {
                                IconButton(onClick = { editing = building; showDialog = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = "تعديل")
                                }
                                IconButton(
                                    onClick = {
                                        buildingToDelete = building
                                        showDeleteDialog = true
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "حذف",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        BuildingEditDialog(
            building = editing,
            onDismiss = { showDialog = false },
            onSave = { b ->
                if (b.id == 0) viewModel.addBuilding(b) else viewModel.updateBuilding(b)
                showDialog = false
            },
            onDelete = {
                editing?.let {
                    buildingToDelete = it
                    showDeleteDialog = true
                }
                showDialog = false
            }
        )
    }
}

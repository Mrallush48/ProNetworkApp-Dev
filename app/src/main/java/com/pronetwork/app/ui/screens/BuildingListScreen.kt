package com.pronetwork.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.pronetwork.app.data.Building

@Composable
fun BuildingListScreen(
    buildings: List<Building>,
    onAddBuilding: () -> Unit,
    onBuildingClick: (Building) -> Unit,
    onSearch: (String) -> Unit
) {
    var search by remember { mutableStateOf(TextFieldValue("")) }
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = {
                    search = it
                    onSearch(it.text)
                },
                label = { Text("بحث عن مبنى") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onAddBuilding) {
                Text("إضافة مبنى")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn {
            items(buildings) { building ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onBuildingClick(building) },
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("اسم المبنى: ${building.name}", style = MaterialTheme.typography.h6)
                        if (building.location.isNotEmpty()) {
                            Text(
                                "الموقع: ${building.location}",
                                color = MaterialTheme.colors.primary,
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(building.location))
                                    context.startActivity(intent)
                                }
                            )
                        }
                        if (building.notes.isNotEmpty())
                            Text("ملاحظات: ${building.notes}")
                    }
                }
            }
        }
    }
}
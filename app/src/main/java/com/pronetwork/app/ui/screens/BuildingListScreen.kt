package com.pronetwork.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pronetwork.app.R
import com.pronetwork.app.data.Building

@Composable
fun BuildingListScreen(
    buildings: List<Building>,
    searchQuery: String,
    onAddBuilding: () -> Unit,
    onBuildingClick: (Building) -> Unit,
    onSearch: (String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearch,
                label = { Text(stringResource(R.string.buildings_search_label)) },
                modifier = Modifier.weight(1f),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearch("") }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.buildings_search_clear)
                            )
                        }
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(onClick = onAddBuilding) {
                Text(stringResource(R.string.buildings_add_button))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(buildings, key = { it.id }) { building ->
                BuildingListItem(
                    building = building,
                    onClick = { onBuildingClick(building) },
                    onOpenLocation = { location ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(location))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
private fun BuildingListItem(
    building: Building,
    onClick: () -> Unit,
    onOpenLocation: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(
                    R.string.buildings_name_label,
                    building.name
                ),
                style = MaterialTheme.typography.titleMedium
            )

            if (building.location.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.buildings_location_label,
                        building.location
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.clickable {
                        onOpenLocation(building.location)
                    }
                )
            }

            if (building.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.buildings_notes_label,
                        building.notes
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

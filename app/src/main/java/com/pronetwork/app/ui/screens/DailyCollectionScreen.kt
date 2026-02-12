package com.pronetwork.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pronetwork.app.R
import com.pronetwork.app.data.DailyBuildingCollection
import com.pronetwork.app.viewmodel.DailyCollectionUi
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DailyCollectionScreen(
    dailyCollection: DailyCollectionUi?,
    selectedDateMillis: Long,
    onChangeDate: (Long) -> Unit
) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // شريط العنوان + أزرار اليوم/أمس
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.stats_daily_collection),
                style = MaterialTheme.typography.titleLarge
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    // يوم سابق
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = selectedDateMillis
                        add(Calendar.DAY_OF_YEAR, -1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onChangeDate(cal.timeInMillis)
                }) {
                    Text(stringResource(R.string.daily_collection_prev_day))
                }

                Button(onClick = {
                    // يوم لاحق (لا يتجاوز اليوم الحالي)
                    val todayCal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val cal = Calendar.getInstance().apply {
                        timeInMillis = selectedDateMillis
                        add(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val newTime = if (cal.timeInMillis > todayCal.timeInMillis) {
                        todayCal.timeInMillis
                    } else {
                        cal.timeInMillis
                    }

                    onChangeDate(newTime)
                }) {
                    Text(stringResource(R.string.daily_collection_next_day))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // عرض التاريخ الحالي
        Text(
            text = stringResource(
                R.string.daily_collection_date_label,
                dateFormat.format(Date(selectedDateMillis))
            ),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(8.dp))

        // إجمالي اليوم
        val total = dailyCollection?.totalAmount ?: 0.0
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.daily_collection_total_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.daily_collection_total_value,
                        total
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.daily_collection_by_building_title),
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(Modifier.height(8.dp))

        val buildings: List<DailyBuildingCollection> = dailyCollection?.buildings ?: emptyList()

        if (buildings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.daily_collection_empty_for_day))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(buildings, key = { it.buildingId }) { b ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                text = stringResource(
                                    R.string.daily_collection_building_name,
                                    b.buildingName
                                )
                            )
                            Text(
                                text = stringResource(
                                    R.string.daily_collection_clients_count,
                                    b.clientsCount
                                )
                            )
                            Text(
                                text = stringResource(
                                    R.string.daily_collection_building_total,
                                    b.totalAmount
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

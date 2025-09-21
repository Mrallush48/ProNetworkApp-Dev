package com.pronetwork.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pronetwork.app.data.Client
import com.pronetwork.app.data.Building

@Composable
fun ClientListScreen(
    clients: List<Client>,
    buildings: List<Building>,
    onAddClient: () -> Unit,
    onClientClick: (Client) -> Unit,
    onUndoPaid: (Client) -> Unit,
    onPaid: (Client) -> Unit
) {
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
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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
                Text("لا يوجد عملاء في هذا الشهر.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            clients.forEach { client ->
                val buildingName = buildings.firstOrNull { it.id == client.buildingId }?.name ?: "بدون مبنى"
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onClientClick(client) },
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
                        Column {
                            Text("الاسم: ${client.name}", style = MaterialTheme.typography.titleMedium)
                            Text("المبنى: $buildingName", style = MaterialTheme.typography.bodySmall)
                            Text("رقم الاشتراك: ${client.subscriptionNumber}", style = MaterialTheme.typography.bodySmall)
                            Text("الباقة: ${client.packageType}", style = MaterialTheme.typography.bodySmall)
                            Text("الحالة: ${if (client.isPaid) "مدفوع" else "غير مدفوع"}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (!client.isPaid) {
                            Button(
                                onClick = { onPaid(client) },
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
                    }
                }
            }
        }
    }
}
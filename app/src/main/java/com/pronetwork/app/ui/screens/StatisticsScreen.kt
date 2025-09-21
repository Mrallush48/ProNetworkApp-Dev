package com.pronetwork.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StatisticsScreen(
    clientsCount: Int,
    buildingsCount: Int,
    paidClientsCount: Int,
    unpaidClientsCount: Int
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("إحصائيات التطبيق", style = MaterialTheme.typography.h5)
        Spacer(modifier = Modifier.height(24.dp))
        Text("عدد العملاء: $clientsCount")
        Text("عدد المباني: $buildingsCount")
        Text("العملاء المدفوع لهم: $paidClientsCount")
        Text("العملاء غير المدفوع لهم: $unpaidClientsCount")
    }
}
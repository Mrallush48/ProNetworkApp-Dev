package com.pronetwork.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pronetwork.app.data.Client
import com.pronetwork.app.viewmodel.PaymentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    clientsCount: Int,
    buildingsCount: Int,
    // إحصائيات الشهر المختار من PaymentViewModel
    monthStats: PaymentViewModel.MonthStats?,
    // قائمة الشهور المتاحة بصيغة yyyy-MM
    monthOptions: List<String>,
    // الشهر الحالي المختار في ViewModel
    selectedMonth: String,
    // تغيير الشهر في الـ ViewModel
    onMonthChange: (String) -> Unit,
    // يمكن استخدامه لاحقاً لإظهار قائمة المتأخرين بدقة من payments
    allClients: List<Client> = emptyList()
) {
    var monthDropdownExpanded by remember { mutableStateOf(false) }

    // نضمن أن selectedMonth دائمًا ضمن options
    val safeSelectedMonth = remember(selectedMonth, monthOptions) {
        if (monthOptions.contains(selectedMonth)) selectedMonth
        else monthOptions.firstOrNull().orEmpty()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "إحصائيات التطبيق",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        // اختيار الشهر
        ExposedDropdownMenuBox(
            expanded = monthDropdownExpanded,
            onExpandedChange = { monthDropdownExpanded = !monthDropdownExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = safeSelectedMonth,
                onValueChange = { },
                readOnly = true,
                label = { Text("الشهر") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthDropdownExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = monthDropdownExpanded,
                onDismissRequest = { monthDropdownExpanded = false }
            ) {
                monthOptions.forEach { month ->
                    DropdownMenuItem(
                        text = { Text(month) },
                        onClick = {
                            monthDropdownExpanded = false
                            onMonthChange(month)
                        }
                    )
                }
            }
        }

        // كروت الإحصائيات العامة
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                title = "إجمالي العملاء",
                value = clientsCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "المباني",
                value = buildingsCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        // إذا كانت إحصائيات الشهر جاهزة نعرضها، وإلا نظهر حالة تحميل بسيطة
        if (monthStats != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "العملاء المدفوع لهم",
                    value = monthStats.paidCount.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "العملاء غير المدفوع لهم",
                    value = monthStats.unpaidCount.toString(),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    title = "إجمالي المبلغ المحصل",
                    value = formatCurrency(monthStats.totalPaidAmount),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "إجمالي المبلغ المتبقي",
                    value = formatCurrency(monthStats.totalUnpaidAmount),
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            // حالة لا تزال الـ LiveData لم ترجع قيمة بعد
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // يمكن لاحقاً إضافة قسم "العملاء المتأخرين" مبني على جدول payments
        // بدون استخدام client.isPaid نهائياً، وبمنطق يتوافق مع طريقتك في التحصيل.
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium,
                color = color
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// تنسيق مبالغ بشكل بسيط (يمكن تحسينه لاحقاً بإضافة Locale مخصص)
private fun formatCurrency(amount: Double): String {
    return String.format("%.2f ر.س", amount)
}

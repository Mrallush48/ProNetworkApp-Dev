package com.pronetwork.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pronetwork.app.R

// ============ خيارات الفرز ============
enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    STATUS_UNPAID_FIRST,
    STATUS_PAID_FIRST,
    PRICE_HIGH,
    PRICE_LOW,
    BUILDING,
    PACKAGE,
    START_MONTH
}

@Composable
fun ViewOptionsDialog(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.view_options_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.view_options_sort_by),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SortOptionItem(
                    icon = Icons.Filled.SortByAlpha,
                    label = stringResource(R.string.sort_name_asc),
                    isSelected = currentSort == SortOption.NAME_ASC,
                    onClick = { onSortSelected(SortOption.NAME_ASC) }
                )
                SortOptionItem(
                    icon = Icons.Filled.SortByAlpha,
                    label = stringResource(R.string.sort_name_desc),
                    isSelected = currentSort == SortOption.NAME_DESC,
                    onClick = { onSortSelected(SortOption.NAME_DESC) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                SortOptionItem(
                    icon = Icons.Filled.Warning,
                    label = stringResource(R.string.sort_unpaid_first),
                    isSelected = currentSort == SortOption.STATUS_UNPAID_FIRST,
                    onClick = { onSortSelected(SortOption.STATUS_UNPAID_FIRST) },
                    tint = Color(0xFFF44336)
                )
                SortOptionItem(
                    icon = Icons.Filled.CheckCircle,
                    label = stringResource(R.string.sort_paid_first),
                    isSelected = currentSort == SortOption.STATUS_PAID_FIRST,
                    onClick = { onSortSelected(SortOption.STATUS_PAID_FIRST) },
                    tint = Color(0xFF4CAF50)
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                SortOptionItem(
                    icon = Icons.Filled.ArrowUpward,
                    label = stringResource(R.string.sort_price_high),
                    isSelected = currentSort == SortOption.PRICE_HIGH,
                    onClick = { onSortSelected(SortOption.PRICE_HIGH) }
                )
                SortOptionItem(
                    icon = Icons.Filled.ArrowDownward,
                    label = stringResource(R.string.sort_price_low),
                    isSelected = currentSort == SortOption.PRICE_LOW,
                    onClick = { onSortSelected(SortOption.PRICE_LOW) }
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

                SortOptionItem(
                    icon = Icons.Filled.Home,
                    label = stringResource(R.string.sort_by_building),
                    isSelected = currentSort == SortOption.BUILDING,
                    onClick = { onSortSelected(SortOption.BUILDING) }
                )
                SortOptionItem(
                    icon = Icons.Filled.Wifi,
                    label = stringResource(R.string.sort_by_package),
                    isSelected = currentSort == SortOption.PACKAGE,
                    onClick = { onSortSelected(SortOption.PACKAGE) }
                )
                SortOptionItem(
                    icon = Icons.Filled.DateRange,
                    label = stringResource(R.string.sort_by_start_month),
                    isSelected = currentSort == SortOption.START_MONTH,
                    onClick = { onSortSelected(SortOption.START_MONTH) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_confirm))
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun SortOptionItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    tint: Color? = null
) {
    val selectedColor = MaterialTheme.colorScheme.primary
    val defaultColor = MaterialTheme.colorScheme.onSurfaceVariant
    val activeColor = if (isSelected) selectedColor else (tint ?: defaultColor)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = activeColor,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = selectedColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

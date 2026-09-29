package com.example.personalfinances.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.personalfinances.ui.theme.wallet
import com.example.personalfinances.util.DateUtils
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Pill-shaped month picker: previous and next buttons around the month name, with the exact
 * date range underneath. The range reflects [payCycleStartDay], so a cycle starting on the 25th
 * reads "25 Aug – 24 Sep" rather than implying a calendar month.
 */
@Composable
fun MonthSelector(
    selectedMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
    payCycleStartDay: Int = 1
) {
    val wallet = MaterialTheme.wallet
    val (start, end) = DateUtils.monthDateRange(selectedMonth, payCycleStartDay)
    val rangeFormatter = DateTimeFormatter.ofPattern("d MMM")
    val buttonColors = IconButtonDefaults.iconButtonColors(containerColor = wallet.cardTonal)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .height(48.dp)
            .clip(CircleShape)
            .background(wallet.card)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth, modifier = Modifier.size(40.dp), colors = buttonColors) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous month"
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "${start.format(rangeFormatter)} – ${end.format(rangeFormatter)}",
                style = MaterialTheme.typography.labelSmall,
                color = wallet.muted
            )
        }
        IconButton(onClick = onNextMonth, modifier = Modifier.size(40.dp), colors = buttonColors) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next month"
            )
        }
    }
}

package de.formerdehne.wposter.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.formerdehne.wposter.R
import de.formerdehne.wposter.ui.CalendarState
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

// Locale bei jedem Aufruf neu lesen, damit ein Sprachwechsel sofort greift.
private fun monthFormat() = DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault())
private fun monthOnlyFormat() = DateTimeFormatter.ofPattern("LLLL", Locale.getDefault())
private fun firstDayOfWeek(): DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

/** Kurze Wochentagsnamen ab dem ersten Tag der Woche (z. B. „Mo“ … „So“ bzw. „Sun“ … „Sat“). */
private fun weekdayLabels(): List<String> {
    val locale = Locale.getDefault()
    val first = firstDayOfWeek()
    return (0 until 7).map { first.plus(it.toLong()).getDisplayName(TextStyle.SHORT, locale).removeSuffix(".") }
}

@Composable
fun CalendarScreen(
    state: CalendarState,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onSelect: (LocalDate?) -> Unit,
    onRetry: () -> Unit,
    onOpenPost: (Long) -> Unit,
) {
    Scaffold(topBar = { AppTopBar(stringResource(R.string.calendar_title), onBack = onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "calendar") {
                MonthCard(state, onPrevious, onNext, onToday, onSelect)
            }
            when {
                state.loading -> item(key = "loading") {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.error != null -> item(key = "error") {
                    MessageBlock(stringResource(R.string.home_load_failed), state.error, stringResource(R.string.common_retry), onRetry)
                }
                state.posts.isEmpty() -> item(key = "empty") {
                    MessageBlock(null, stringResource(R.string.calendar_no_posts_in, state.month.format(monthFormat())), null, null)
                }
                else -> {
                    item(key = "heading") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val count = state.visiblePosts.size
                            val dayFormat = DateTimeFormatter.ofPattern(stringResource(R.string.calendar_day_pattern), Locale.getDefault())
                            Text(
                                (state.selectedDay?.format(dayFormat) ?: state.month.format(monthOnlyFormat())) +
                                    " · " + if (count == 1) stringResource(R.string.calendar_one_post)
                                    else stringResource(R.string.calendar_posts, count),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            if (state.selectedDay != null) {
                                TextButton(onClick = { onSelect(null) }) { Text(stringResource(R.string.calendar_whole_month)) }
                            }
                        }
                    }
                    items(state.visiblePosts, key = { it.id }) { post -> PostCard(post) { onOpenPost(post.id) } }
                }
            }
        }
    }
}

@Composable
private fun MonthCard(
    state: CalendarState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onSelect: (LocalDate?) -> Unit,
) {
    val counts = state.countByDay
    val today = LocalDate.now()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.calendar_previous_month))
                }
                Text(
                    state.month.format(monthFormat()),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onNext) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.calendar_next_month))
                }
            }
            if (state.month != YearMonth.now()) {
                TextButton(onClick = onToday, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.calendar_current_month))
                }
            }
            Spacer(Modifier.height(4.dp))
            Row {
                weekdayLabels().forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            // Raster: erste Spalte = erster Tag der Woche laut Sprache/Region.
            val first = state.month.atDay(1)
            val offset = (first.dayOfWeek.value - firstDayOfWeek().value + 7) % 7
            val days = state.month.lengthOfMonth()
            val rows = (offset + days + 6) / 7
            for (row in 0 until rows) {
                Row {
                    for (col in 0 until 7) {
                        val dayNumber = row * 7 + col - offset + 1
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (dayNumber in 1..days) {
                                val date = state.month.atDay(dayNumber)
                                DayCell(
                                    day = dayNumber,
                                    count = counts[date] ?: 0,
                                    selected = state.selectedDay == date,
                                    isToday = date == today,
                                    onClick = { onSelect(date) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, count: Int, selected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val hasPosts = count > 0
    val background = when {
        selected -> colors.primary
        hasPosts -> colors.primaryContainer
        else -> colors.surfaceContainerLowest
    }
    val foreground = when {
        selected -> colors.onPrimary
        hasPosts -> colors.onPrimaryContainer
        else -> colors.onSurfaceVariant
    }
    var modifier = Modifier
        .padding(3.dp)
        .fillMaxSize()
        .clip(CircleShape)
        .background(background)
    if (isToday && !selected) modifier = modifier.border(1.5.dp, colors.tertiary, CircleShape)
    if (hasPosts) modifier = modifier.clickable(onClick = onClick)
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (hasPosts) FontWeight.SemiBold else FontWeight.Normal,
                color = foreground,
            )
            if (count > 1) {
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = foreground.copy(alpha = 0.8f),
                )
            } else if (hasPosts) {
                Box(Modifier.padding(top = 2.dp).clip(CircleShape).background(foreground).height(4.dp).aspectRatio(1f))
            }
        }
    }
}

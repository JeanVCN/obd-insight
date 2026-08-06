package com.obd.insight.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.obd.insight.data.persistence.SensorReadingEntity
import java.util.Locale
import kotlin.math.max

@Composable
fun TripMetricTile(label: String, value: String, detail: String, accent: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = accent)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SensorChartCard(readings: List<SensorReadingEntity>, accent: Color, modifier: Modifier = Modifier) {
    if (readings.isEmpty()) return
    val first = readings.first()
    val values = readings.map { it.value }
    val min = values.minOrNull() ?: 0f
    val max = values.maxOrNull() ?: 1f
    val range = max(0.001f, max - min)
    val low = min - range * .08f
    val high = max + range * .08f

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(first.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("PID 0x${first.pid.toString(16).padStart(2, '0').uppercase()}  •  ${readings.size} amostras", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(first.unit, style = MaterialTheme.typography.labelLarge, color = accent)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.width(52.dp).height(180.dp).padding(vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatNumber(high), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatNumber((high + low) / 2f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatNumber(low), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Canvas(Modifier.weight(1f).height(180.dp)) {
                    val left = 4f
                    val top = 14f
                    val right = size.width - 12f
                    val bottom = size.height - 18f
                    val chartWidth = right - left
                    val chartHeight = bottom - top
                    val gridColor = Color.White.copy(alpha = .08f)
                    repeat(3) { index ->
                        val y = top + chartHeight * index / 2f
                        drawLine(gridColor, Offset(left, y), Offset(right, y), 1f)
                    }
                    val path = Path()
                    readings.forEachIndexed { index, reading ->
                        val x = if (readings.size == 1) left else left + chartWidth * index / (readings.size - 1)
                        val y = bottom - ((reading.value - low) / (high - low)) * chartHeight
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    val fill = Path().apply {
                        addPath(path)
                        lineTo(right, bottom)
                        lineTo(left, bottom)
                        close()
                    }
                    drawPath(fill, Brush.verticalGradient(listOf(accent.copy(alpha = .32f), accent.copy(alpha = 0f))))
                    drawPath(path, accent, style = Stroke(width = 3f, cap = StrokeCap.Round))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("mín ${formatNumber(min)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("média ${formatNumber(values.average().toFloat())}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("máx ${formatNumber(max)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("atual ${formatNumber(values.last())} ${first.unit}", style = MaterialTheme.typography.labelSmall, color = accent)
        }
    }
}

data class SensorSummary(
    val pid: Int,
    val label: String,
    val unit: String,
    val count: Int,
    val min: Float,
    val average: Float,
    val max: Float
)

fun summarizeSensors(readings: List<SensorReadingEntity>): List<SensorSummary> = readings
    .groupBy { it.pid }
    .values
    .map { values ->
        SensorSummary(
            pid = values.first().pid,
            label = values.first().label,
            unit = values.first().unit,
            count = values.size,
            min = values.minOf { it.value },
            average = values.map { it.value }.average().toFloat(),
            max = values.maxOf { it.value }
        )
    }
    .sortedBy { it.pid }

private fun formatNumber(value: Float): String = String.format(Locale.US, "%.1f", value)

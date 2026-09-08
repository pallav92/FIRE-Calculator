package com.finance.firecalculator.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finance.firecalculator.domain.model.FireResult
import com.finance.firecalculator.domain.model.YearlyTrajectoryPoint
import com.finance.firecalculator.ui.util.CurrencyFormatter
import kotlin.math.max

@Composable
fun ProjectionChart(
    result: FireResult,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val points = result.trajectory
    if (points.isEmpty()) return

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val selectedPoint: YearlyTrajectoryPoint? = selectedIndex?.let { points.getOrNull(it) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Portfolio Projection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (selectedPoint != null) {
                    Text(
                        text = "Age ${selectedPoint.age}: ${CurrencyFormatter.formatCompact(selectedPoint.endCorpus, currencySymbol)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Tap to inspect age",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val maxCorpus = max(
                result.targetCorpusNeeded,
                points.maxOfOrNull { it.endCorpus } ?: 1.0
            ).coerceAtLeast(1.0) * 1.15

            val lineColor = MaterialTheme.colorScheme.primary
            val retirementLineColor = Color(0xFFE65100) // Vibrant Orange
            val targetLineColor = Color(0xFF6C757D)
            val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            val milestoneColor = MaterialTheme.colorScheme.tertiary
            val indicatorColor = MaterialTheme.colorScheme.onSurface

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .pointerInput(points) {
                        detectTapGestures(
                            onTap = { offset ->
                                val spacing = size.width / (points.size - 1).coerceAtLeast(1)
                                val index = (offset.x / spacing).toInt().coerceIn(0, points.size - 1)
                                selectedIndex = index
                            }
                        )
                    }
            ) {
                val width = size.width
                val height = size.height
                val pointCount = points.size
                if (pointCount < 2) return@Canvas

                val stepX = width / (pointCount - 1)

                // 1. Draw horizontal grid lines
                val gridSteps = 4
                for (i in 0..gridSteps) {
                    val y = height - (i.toFloat() / gridSteps) * height
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // 2. Draw Target Corpus Reference Line
                val targetY = height - ((result.targetCorpusNeeded / maxCorpus) * height).toFloat()
                if (targetY in 0f..height) {
                    drawLine(
                        color = targetLineColor,
                        start = Offset(0f, targetY),
                        end = Offset(width, targetY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }

                // 3. Draw Accumulation & Retirement Curve
                val accumulationPath = Path()
                val retirementPath = Path()
                var hasAccumulationStarted = false
                var hasRetirementStarted = false
                var retirementTransitionX = 0f
                var retirementTransitionY = 0f

                points.forEachIndexed { index, pt ->
                    val x = index * stepX
                    val y = height - ((pt.endCorpus / maxCorpus) * height).toFloat().coerceIn(0f, height)

                    if (!pt.isRetired) {
                        if (!hasAccumulationStarted) {
                            accumulationPath.moveTo(x, y)
                            hasAccumulationStarted = true
                        } else {
                            accumulationPath.lineTo(x, y)
                        }
                        retirementTransitionX = x
                        retirementTransitionY = y
                    } else {
                        if (!hasRetirementStarted) {
                            retirementPath.moveTo(retirementTransitionX, retirementTransitionY)
                            retirementPath.lineTo(x, y)
                            hasRetirementStarted = true
                        } else {
                            retirementPath.lineTo(x, y)
                        }
                    }
                }

                // Draw strokes
                drawPath(
                    path = accumulationPath,
                    color = lineColor,
                    style = Stroke(width = 3.5.dp.toPx())
                )

                drawPath(
                    path = retirementPath,
                    color = retirementLineColor,
                    style = Stroke(
                        width = 3.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                    )
                )

                // 4. Vertical Retirement Milestone Marker
                if (retirementTransitionX > 0f) {
                    drawLine(
                        color = milestoneColor,
                        start = Offset(retirementTransitionX, 0f),
                        end = Offset(retirementTransitionX, height),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    )
                    drawCircle(
                        color = milestoneColor,
                        radius = 6.dp.toPx(),
                        center = Offset(retirementTransitionX, retirementTransitionY)
                    )
                }

                // 5. Selected Indicator
                selectedIndex?.let { idx ->
                    val pt = points.getOrNull(idx)
                    if (pt != null) {
                        val sx = idx * stepX
                        val sy = height - ((pt.endCorpus / maxCorpus) * height).toFloat().coerceIn(0f, height)

                        drawLine(
                            color = indicatorColor.copy(alpha = 0.4f),
                            start = Offset(sx, 0f),
                            end = Offset(sx, height),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawCircle(
                            color = indicatorColor,
                            radius = 7.dp.toPx(),
                            center = Offset(sx, sy)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X-axis age labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Age ${points.first().age}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Retire: ${points.find { it.isRetired }?.age ?: points.last().age}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Age ${points.last().age}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chart Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = lineColor, label = "Accumulation")
                Spacer(modifier = Modifier.width(14.dp))
                LegendItem(color = retirementLineColor, label = "Retirement")
                Spacer(modifier = Modifier.width(14.dp))
                LegendItem(color = targetLineColor, label = "Target Corpus", isDashed = true)
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String,
    isDashed: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(14.dp)
                .height(3.dp)
                .background(color, shape = RoundedCornerShape(2.dp))
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

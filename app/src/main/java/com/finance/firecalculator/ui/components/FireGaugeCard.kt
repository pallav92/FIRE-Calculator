package com.finance.firecalculator.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finance.firecalculator.domain.FireCalculationEngine
import com.finance.firecalculator.domain.model.FireInput
import com.finance.firecalculator.domain.model.FireResult
import com.finance.firecalculator.ui.util.CurrencyFormatter
import kotlin.math.min

@Composable
fun FireGaugeCard(
    input: FireInput,
    result: FireResult,
    modifier: Modifier = Modifier
) {
    val currency = input.currencySymbol
    val progressFraction = if (result.targetCorpusNeeded > 0.0) {
        (input.currentCorpus / result.targetCorpusNeeded).toFloat()
    } else {
        1f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction.coerceIn(0f, 1.5f),
        animationSpec = tween(durationMillis = 1000),
        label = "fire_gauge_progress"
    )

    val progressPercent = (progressFraction * 100).toInt()
    val isFireReady = result.isFireAchieved
    val isCoastFire = result.isCoastFireAchieved

    val gaugeGradient = when {
        isFireReady -> listOf(Color(0xFF0F5132), Color(0xFF198754)) // Rich Emerald
        isCoastFire -> listOf(Color(0xFF0B5ED7), Color(0xFF0D6EFD)) // Vibrant Blue
        progressFraction >= 0.5f -> listOf(Color(0xFF20C997), Color(0xFF0D6EFD)) // Teal to Blue
        else -> listOf(Color(0xFFD97706), Color(0xFFF59E0B)) // Warm Amber
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "FIRE Readiness Progress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Circular Gauge Arc with Center Readout
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 16.dp.toPx()
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    val arcSize = Size(diameter, diameter)

                    // Background 240-degree arc
                    drawArc(
                        color = trackColor,
                        startAngle = 150f,
                        sweepAngle = 240f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Foreground animated progress arc
                    val sweep = (animatedProgress.coerceIn(0f, 1f) * 240f)
                    if (sweep > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(gaugeGradient),
                            startAngle = 150f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                // Center readout
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$progressPercent%",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (progressPercent >= 100) "FIRE Target Met!" else "of Target Saved",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status Pill
            Surface(
                shape = RoundedCornerShape(50),
                color = when {
                    isFireReady -> Color(0xFF198754).copy(alpha = 0.15f)
                    isCoastFire -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                }
            ) {
                Text(
                    text = when {
                        isFireReady -> "🎉 FIRE Ready! Projected surplus achieved"
                        isCoastFire -> "🏖️ Coast FIRE Achieved! Current savings alone will reach target"
                        else -> "🌱 Accumulating • ${result.yearsToRetirement} yrs until retirement"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = when {
                        isFireReady -> Color(0xFF198754)
                        isCoastFire -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            // Metrics Row: Saved vs Needed vs Shortfall
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = "Current Savings",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyFormatter.formatCompact(input.currentCorpus, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Target Needed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = CurrencyFormatter.formatCompact(result.targetCorpusNeeded, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    val shortfall = (result.targetCorpusNeeded - input.currentCorpus).coerceAtLeast(0.0)
                    Text(
                        text = if (shortfall > 0) "Gap Remaining" else "Surplus",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (shortfall > 0) {
                            CurrencyFormatter.formatCompact(shortfall, currency)
                        } else {
                            CurrencyFormatter.formatCompact(input.currentCorpus - result.targetCorpusNeeded, currency)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (shortfall > 0) MaterialTheme.colorScheme.error else Color(0xFF198754)
                    )
                }
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Early Stage (20%)", showBackground = true)
@Composable
fun FireGaugeCardEarlyPreview() {
    com.finance.firecalculator.ui.theme.FIRECalculatorTheme {
        val input = FireInput(currentCorpus = 2500000.0, monthlyWithdrawalPostRetirement = 100000.0)
        val result = FireCalculationEngine.calculate(input)
        FireGaugeCard(input = input, result = result)
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "FIRE Ready (100%+)", showBackground = true)
@Composable
fun FireGaugeCardAchievedPreview() {
    com.finance.firecalculator.ui.theme.FIRECalculatorTheme {
        val input = FireInput(currentCorpus = 40000000.0, monthlyWithdrawalPostRetirement = 100000.0)
        val result = FireCalculationEngine.calculate(input)
        FireGaugeCard(input = input, result = result)
    }
}


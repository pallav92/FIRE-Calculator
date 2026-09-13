package com.finance.firecalculator.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finance.firecalculator.ui.theme.FIRECalculatorTheme
import com.finance.firecalculator.ui.util.CurrencyFormatter
import java.util.Locale
import kotlin.math.max

/**
 * Calculates adaptive slider bounds centered around anchorValue with -50% and +150%.
 */
fun calculateSliderBounds(
    anchorValue: Float,
    absoluteRange: ClosedFloatingPointRange<Float>,
    isAdaptive: Boolean,
    defaultZeroMax: Float = 10_000_000f
): Pair<Float, Float> {
    if (!isAdaptive) {
        return absoluteRange.start to absoluteRange.endInclusive
    }
    if (anchorValue <= 0f) {
        val minB = absoluteRange.start
        val maxB = defaultZeroMax.coerceIn(minB + 1f, absoluteRange.endInclusive)
        return minB to maxB
    }
    // -50% to +150% of the value
    val minB = (anchorValue * 0.5f).coerceAtLeast(absoluteRange.start)
    val maxB = (anchorValue * 2.5f).coerceAtMost(absoluteRange.endInclusive)
    return minB to maxB.coerceAtLeast(minB + 1f)
}

@Composable
fun InputSliderSection(
    title: String,
    formattedValue: String,
    value: Float,
    absoluteRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    stepAmount: Float = 1f,
    allowDirectInput: Boolean = true,
    inputSuffix: String = "",
    isAdaptiveSlider: Boolean = false,
    defaultZeroMax: Float = 10_000_000f,
    currencySymbol: String = "₹",
    onValueChange: (Float) -> Unit,
    onStepChange: (Float) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    // Anchor value used to compute the -50% to +150% slider window.
    // Stays stable while sliding, and only re-anchors on direct input or out-of-bounds stepping.
    var anchorValue by remember { mutableStateOf(value) }

    // Keep anchor in sync if external value goes outside current anchor range
    LaunchedEffect(value) {
        if (isAdaptiveSlider) {
            val (minB, maxB) = calculateSliderBounds(anchorValue, absoluteRange, isAdaptive = true, defaultZeroMax = defaultZeroMax)
            if (value < minB || value > maxB) {
                anchorValue = value
            }
        }
    }

    val (sliderMin, sliderMax) = remember(anchorValue, absoluteRange, isAdaptiveSlider, defaultZeroMax) {
        calculateSliderBounds(anchorValue, absoluteRange, isAdaptiveSlider, defaultZeroMax)
    }

    // Format step label for the central capsule stepper
    val stepLabel = remember(stepAmount, isAdaptiveSlider, currencySymbol, inputSuffix) {
        if (isAdaptiveSlider) {
            "±${CurrencyFormatter.formatCompact(stepAmount.toDouble(), currencySymbol)}"
        } else if (inputSuffix.isNotEmpty()) {
            val formattedNum = if (stepAmount % 1f == 0f) stepAmount.toInt().toString() else String.format(Locale.getDefault(), "%.1f", stepAmount)
            "±$formattedNum$inputSuffix"
        } else {
            val formattedNum = if (stepAmount % 1f == 0f) stepAmount.toInt().toString() else String.format(Locale.getDefault(), "%.1f", stepAmount)
            "±$formattedNum"
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. Title & Elevated Interactive Value Pill Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                modifier = Modifier.clickable(enabled = allowDirectInput) {
                    showEditDialog = true
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formattedValue,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (allowDirectInput) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit value directly",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 2. Full-Width Scrubbing Slider
        Slider(
            value = value.coerceIn(sliderMin, sliderMax),
            onValueChange = { newValue ->
                val stepped = if (stepAmount >= 1f) {
                    (kotlin.math.round(newValue / stepAmount) * stepAmount)
                        .coerceIn(absoluteRange.start, absoluteRange.endInclusive)
                } else {
                    newValue.coerceIn(absoluteRange.start, absoluteRange.endInclusive)
                }
                onValueChange(stepped)
            },
            valueRange = sliderMin..sliderMax,
            modifier = Modifier.fillMaxWidth()
        )

        // 3. Ergonomic Bottom Bar: [Min Label] --- [Unified Capsule Stepper] --- [Max Label]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Min Boundary
            if (isAdaptiveSlider) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = CurrencyFormatter.formatCompact(sliderMin.toDouble(), currencySymbol),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "-50%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }
            } else {
                Text(
                    text = "${absoluteRange.start.toInt()}$inputSuffix",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Center: Unified Capsule Stepper Control [ - | Step | + ]
            val canDecrease = value > absoluteRange.start
            val canIncrease = value < absoluteRange.endInclusive

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = {
                            val next = (value - stepAmount).coerceAtLeast(absoluteRange.start)
                            if (isAdaptiveSlider && next < sliderMin) {
                                anchorValue = next
                            }
                            onStepChange(-stepAmount)
                        },
                        enabled = canDecrease,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease by step",
                            modifier = Modifier.size(16.dp),
                            tint = if (canDecrease) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                        )
                    }

                    Text(
                        text = stepLabel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    IconButton(
                        onClick = {
                            val next = (value + stepAmount).coerceAtMost(absoluteRange.endInclusive)
                            if (isAdaptiveSlider && next > sliderMax) {
                                anchorValue = next
                            }
                            onStepChange(stepAmount)
                        },
                        enabled = canIncrease,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase by step",
                            modifier = Modifier.size(16.dp),
                            tint = if (canIncrease) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)
                        )
                    }
                }
            }

            // Right: Max Boundary
            if (isAdaptiveSlider) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "+150%",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = CurrencyFormatter.formatCompact(sliderMax.toDouble(), currencySymbol),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "${absoluteRange.endInclusive.toInt()}$inputSuffix",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showEditDialog) {
        DirectNumberInputDialog(
            title = "Enter $title",
            currentValue = value,
            absoluteRange = absoluteRange,
            suffix = inputSuffix,
            currencySymbol = currencySymbol,
            onDismiss = { showEditDialog = false },
            onConfirm = { newValue ->
                val clamped = newValue.coerceIn(absoluteRange.start, absoluteRange.endInclusive)
                if (isAdaptiveSlider) {
                    anchorValue = clamped
                }
                onValueChange(clamped)
                showEditDialog = false
            }
        )
    }
}

@Composable
private fun DirectNumberInputDialog(
    title: String,
    currentValue: Float,
    absoluteRange: ClosedFloatingPointRange<Float>,
    suffix: String,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var textInput by remember {
        mutableStateOf(if (currentValue % 1f == 0f) currentValue.toLong().toString() else currentValue.toString())
    }
    val focusManager = LocalFocusManager.current

    val parsedValue = remember(textInput) {
        parseInputNumber(textInput)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Allowed range: ${CurrencyFormatter.formatCompact(absoluteRange.start.toDouble(), currencySymbol)} to ${CurrencyFormatter.formatCompact(absoluteRange.endInclusive.toDouble(), currencySymbol)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Tip: You can type shorthand like '50 Cr', '50 L', '10k', '2M' or raw digits.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        parsedValue?.let { onConfirm(it) }
                        focusManager.clearFocus()
                    }),
                    suffix = if (suffix.isNotEmpty()) {
                        { Text(suffix) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (parsedValue != null) {
                    val clamped = parsedValue.coerceIn(absoluteRange.start, absoluteRange.endInclusive)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Interpreted: ${CurrencyFormatter.formatCompact(clamped.toDouble(), currencySymbol)} (${CurrencyFormatter.format(clamped.toDouble(), currencySymbol)})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Slider will adapt to: ${CurrencyFormatter.formatCompact((clamped * 0.5f).toDouble(), currencySymbol)} to ${CurrencyFormatter.formatCompact((clamped * 2.5f).toDouble(), currencySymbol)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                } else if (textInput.isNotBlank()) {
                    Text(
                        text = "Invalid number format",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    parsedValue?.let { onConfirm(it) }
                },
                enabled = parsedValue != null
            ) {
                Text("Update")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Parses numeric inputs with support for commas, spaces, currency symbols, and shorthand:
 * - 'cr', 'crore', 'crores' (10,000,000)
 * - 'l', 'lac', 'lakh', 'lakhs' (100,000)
 * - 'k' (1,000)
 * - 'm', 'million' (1,000,000)
 * - 'b', 'billion' (1,000,000,000)
 */
fun parseInputNumber(raw: String): Float? {
    val clean = raw.trim()
        .replace(",", "")
        .replace(" ", "")
        .replace("$", "")
        .replace("₹", "")
        .replace("€", "")
        .replace("£", "")
        .replace("¥", "")
    if (clean.isEmpty()) return null

    val lower = clean.lowercase(Locale.ROOT)
    return try {
        when {
            lower.endsWith("crores") || lower.endsWith("crore") || lower.endsWith("cr") -> {
                val numPart = lower.substringBefore("c")
                (numPart.toDouble() * 10_000_000.0).toFloat()
            }
            lower.endsWith("lakhs") || lower.endsWith("lakh") || lower.endsWith("lac") || lower.endsWith("l") -> {
                val numPart = lower.substringBefore("l")
                (numPart.toDouble() * 100_000.0).toFloat()
            }
            lower.endsWith("k") -> {
                val numPart = lower.substringBefore("k")
                (numPart.toDouble() * 1_000.0).toFloat()
            }
            lower.endsWith("million") || lower.endsWith("m") -> {
                val numPart = lower.substringBefore("m")
                (numPart.toDouble() * 1_000_000.0).toFloat()
            }
            lower.endsWith("billion") || lower.endsWith("b") -> {
                val numPart = lower.substringBefore("b")
                (numPart.toDouble() * 1_000_000_000.0).toFloat()
            }
            else -> lower.toFloat()
        }
    } catch (e: Exception) {
        null
    }
}

// -------------------------------------------------------------------------
// COMPOSE PREVIEWS: Testing UI with 1.25 Crores and Large Values
// -------------------------------------------------------------------------

@Preview(name = "End Value 1.25 Crores (50 Lakhs current)", showBackground = true, widthDp = 360)
@Composable
fun PreviewSliderEndValueOnePointTwoFiveCrores() {
    FIRECalculatorTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            // Current = 50 Lakhs (5,000,000). -50% = 25 Lakhs. +150% = 1.25 Crores!
            var currentVal by remember { mutableFloatStateOf(5_000_000f) }
            InputSliderSection(
                title = "Current Retirement Corpus",
                subtitle = "Allowed: 0 to 99 Crores",
                formattedValue = CurrencyFormatter.formatCompact(currentVal.toDouble(), "₹"),
                value = currentVal,
                absoluteRange = 0f..990_000_000f,
                isAdaptiveSlider = true,
                currencySymbol = "₹",
                onValueChange = { currentVal = it },
                onStepChange = { currentVal += it }
            )
        }
    }
}

@Preview(name = "Current Value 1.25 Crores", showBackground = true, widthDp = 360)
@Composable
fun PreviewSliderValueAtOnePointTwoFiveCrores() {
    FIRECalculatorTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            // Current = 1.25 Crores (12,500,000). -50% = 62.5 Lakhs. +150% = 3.125 Crores!
            var currentVal by remember { mutableFloatStateOf(12_500_000f) }
            InputSliderSection(
                title = "Current Retirement Corpus",
                subtitle = "Allowed: 0 to 99 Crores",
                formattedValue = CurrencyFormatter.formatCompact(currentVal.toDouble(), "₹"),
                value = currentVal,
                absoluteRange = 0f..990_000_000f,
                isAdaptiveSlider = true,
                currencySymbol = "₹",
                onValueChange = { currentVal = it },
                onStepChange = { currentVal += it }
            )
        }
    }
}

@Preview(name = "Narrow Screen 320dp - 1.25 Crores", showBackground = true, widthDp = 320)
@Composable
fun PreviewSliderNarrowScreen() {
    FIRECalculatorTheme {
        Surface(modifier = Modifier.padding(12.dp)) {
            var currentVal by remember { mutableFloatStateOf(5_000_000f) }
            InputSliderSection(
                title = "Current Retirement Corpus",
                subtitle = "Allowed: 0 to 99 Crores",
                formattedValue = CurrencyFormatter.formatCompact(currentVal.toDouble(), "₹"),
                value = currentVal,
                absoluteRange = 0f..990_000_000f,
                isAdaptiveSlider = true,
                currencySymbol = "₹",
                onValueChange = { currentVal = it },
                onStepChange = { currentVal += it }
            )
        }
    }
}

@Preview(name = "High Value 50 Crores (max 99 Crores)", showBackground = true, widthDp = 360)
@Composable
fun PreviewSliderFiftyCrores() {
    FIRECalculatorTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            var currentVal by remember { mutableFloatStateOf(500_000_000f) }
            InputSliderSection(
                title = "Current Retirement Corpus",
                subtitle = "Allowed: 0 to 99 Crores",
                formattedValue = CurrencyFormatter.formatCompact(currentVal.toDouble(), "₹"),
                value = currentVal,
                absoluteRange = 0f..990_000_000f,
                isAdaptiveSlider = true,
                currencySymbol = "₹",
                onValueChange = { currentVal = it },
                onStepChange = { currentVal += it }
            )
        }
    }
}

@Preview(name = "Zero Corpus Initial", showBackground = true, widthDp = 360)
@Composable
fun PreviewSliderZeroCorpus() {
    FIRECalculatorTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            var currentVal by remember { mutableFloatStateOf(0f) }
            InputSliderSection(
                title = "Current Retirement Corpus",
                subtitle = "Allowed: 0 to 99 Crores",
                formattedValue = CurrencyFormatter.formatCompact(currentVal.toDouble(), "₹"),
                value = currentVal,
                absoluteRange = 0f..990_000_000f,
                isAdaptiveSlider = true,
                currencySymbol = "₹",
                onValueChange = { currentVal = it },
                onStepChange = { currentVal += it }
            )
        }
    }
}

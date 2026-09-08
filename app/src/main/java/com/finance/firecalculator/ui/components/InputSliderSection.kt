package com.finance.firecalculator.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.finance.firecalculator.ui.util.CurrencyFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

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
    currencySymbol: String = "$",
    onValueChange: (Float) -> Unit,
    onStepChange: (Float) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    // Anchor value used to compute the -50% to +150% slider window
    var anchorValue by remember { mutableStateOf(value) }

    // If external value changes significantly out of the current anchor range, re-sync anchor
    LaunchedEffect(value) {
        if (isAdaptiveSlider) {
            val curMin = if (anchorValue <= absoluteRange.start || anchorValue == 0f) absoluteRange.start else anchorValue * 0.5f
            val curMax = if (anchorValue <= absoluteRange.start || anchorValue == 0f) defaultZeroMax else anchorValue * 2.5f
            if (value < curMin || value > curMax) {
                anchorValue = value
            }
        }
    }

    // Compute slider visible bounds
    val (sliderMin, sliderMax) = remember(anchorValue, absoluteRange, isAdaptiveSlider, defaultZeroMax) {
        if (!isAdaptiveSlider) {
            absoluteRange.start to absoluteRange.endInclusive
        } else if (anchorValue <= absoluteRange.start || anchorValue == 0f) {
            val minB = absoluteRange.start
            val maxB = defaultZeroMax.coerceIn(minB + 1f, absoluteRange.endInclusive)
            minB to maxB
        } else {
            // -50% to +150% of the value
            val minB = (anchorValue * 0.5f).coerceAtLeast(absoluteRange.start)
            val maxB = (anchorValue * 2.5f).coerceAtMost(absoluteRange.endInclusive)
            minB to maxB.coerceAtLeast(minB + 1f)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.clickable(enabled = allowDirectInput) {
                    showEditDialog = true
                }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                            contentDescription = "Edit value",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Dynamic step calculation based on current value magnitude
        val effectiveStep = remember(value, stepAmount, isAdaptiveSlider) {
            if (!isAdaptiveSlider) {
                stepAmount
            } else when {
                value >= 100_000_000f -> 10_000_000f // >= 10 Cr: step by 1 Cr
                value >= 10_000_000f -> 1_000_000f  // >= 1 Cr: step by 10 L
                value >= 1_000_000f -> 100_000f     // >= 10 L: step by 1 L
                value >= 100_000f -> 10_000f        // >= 1 L: step by 10 K
                else -> stepAmount
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = {
                    val next = (value - effectiveStep).coerceAtLeast(absoluteRange.start)
                    if (isAdaptiveSlider && next < sliderMin) {
                        anchorValue = next
                    }
                    onStepChange(-effectiveStep)
                },
                enabled = value > absoluteRange.start,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Decrease",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = value.coerceIn(sliderMin, sliderMax),
                onValueChange = { newValue ->
                    onValueChange(newValue.coerceIn(absoluteRange.start, absoluteRange.endInclusive))
                },
                onValueChangeFinished = {
                    if (isAdaptiveSlider) {
                        anchorValue = value
                    }
                },
                valueRange = sliderMin..sliderMax,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = {
                    val next = (value + effectiveStep).coerceAtMost(absoluteRange.endInclusive)
                    if (isAdaptiveSlider && next > sliderMax) {
                        anchorValue = next
                    }
                    onStepChange(effectiveStep)
                },
                enabled = value < absoluteRange.endInclusive,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Increase",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Adaptive slider range indicator caption
        if (isAdaptiveSlider) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Slider: ${CurrencyFormatter.formatCompact(sliderMin.toDouble(), currencySymbol)} (-50%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp
                )
                Text(
                    text = "Tap value to enter any number up to ${CurrencyFormatter.formatCompact(absoluteRange.endInclusive.toDouble(), currencySymbol)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
                Text(
                    text = "${CurrencyFormatter.formatCompact(sliderMax.toDouble(), currencySymbol)} (+150%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp
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

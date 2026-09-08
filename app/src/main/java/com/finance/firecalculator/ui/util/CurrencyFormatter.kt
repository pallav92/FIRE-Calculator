package com.finance.firecalculator.ui.util

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object CurrencyFormatter {

    fun format(amount: Double, symbol: String = "$"): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
        return "$symbol${formatter.format(amount)}"
    }

    fun formatCompact(amount: Double, symbol: String = "$"): String {
        val absVal = abs(amount)
        val sign = if (amount < 0) "-" else ""

        if (symbol == "₹") {
            // Indian numbering format (Lakh, Crore)
            return when {
                absVal >= 10_000_000 -> "$sign$symbol${String.format(Locale.US, "%.2f", absVal / 10_000_000)} Cr"
                absVal >= 100_000 -> "$sign$symbol${String.format(Locale.US, "%.2f", absVal / 100_000)} L"
                absVal >= 1_000 -> "$sign$symbol${String.format(Locale.US, "%.1f", absVal / 1_000)} K"
                else -> "$sign$symbol${String.format(Locale.US, "%.0f", absVal)}"
            }
        }

        // Standard Western format (K, M, B)
        return when {
            absVal >= 1_000_000_000 -> "$sign$symbol${String.format(Locale.US, "%.2f", absVal / 1_000_000_000)}B"
            absVal >= 1_000_000 -> "$sign$symbol${String.format(Locale.US, "%.2f", absVal / 1_000_000)}M"
            absVal >= 1_000 -> "$sign$symbol${String.format(Locale.US, "%.1f", absVal / 1_000)}K"
            else -> "$sign$symbol${String.format(Locale.US, "%.0f", absVal)}"
        }
    }

    fun formatPercent(value: Double): String {
        return String.format(Locale.US, "%.1f%%", value)
    }
}

package com.example.domain

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

data class YearMonthKey(
    val year: Int,
    val month: Int // 1..12
) : Comparable<YearMonthKey> {
    override fun compareTo(other: YearMonthKey): Int {
        return if (year != other.year) year.compareTo(other.year) else month.compareTo(other.month)
    }

    fun prev(): YearMonthKey = if (month == 1) YearMonthKey(year - 1, 12) else YearMonthKey(year, month - 1)
    fun next(): YearMonthKey = if (month == 12) YearMonthKey(year + 1, 1) else YearMonthKey(year, month + 1)

    fun daysInMonth(): Int {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, month - 1, 1)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    /** Returns 0 for Monday .. 6 for Sunday. */
    fun firstDayOfWeekMondayBased(): Int {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, month - 1, 1)
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    fun isoDateForDay(day: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    fun displayTitle(): String {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, month - 1, 1)
        val fmt = SimpleDateFormat("MMMM yyyy", Locale.US)
        return fmt.format(cal.time)
    }

    companion object {
        fun current(): YearMonthKey {
            val cal = Calendar.getInstance()
            return YearMonthKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        }

        fun fromEpoch(epochMs: Long): YearMonthKey {
            val cal = Calendar.getInstance()
            cal.timeInMillis = epochMs
            return YearMonthKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
        }
    }
}

object DateUtils {
    private const val ONE_DAY_MS = 86_400_000L

    fun todayIso(): String = epochToIsoDate(System.currentTimeMillis())

    fun epochToIsoDate(epochMs: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return fmt.format(Date(epochMs))
    }

    fun isoDateToEpoch(iso: String, hour: Int = 9, minute: Int = 30): Long {
        val parts = iso.trim().split("-")
        if (parts.size != 3) return System.currentTimeMillis()
        val year = parts[0].toIntOrNull() ?: return System.currentTimeMillis()
        val month = parts[1].toIntOrNull() ?: return System.currentTimeMillis()
        val day = parts[2].toIntOrNull() ?: return System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
        cal.set(Calendar.DAY_OF_MONTH, day.coerceIn(1, 31))
        cal.set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        cal.set(Calendar.MINUTE, minute.coerceIn(0, 59))
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun parseFlexibleDateTime(raw: String): Long? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "MM/dd/yyyy HH:mm:ss",
            "MM/dd/yyyy HH:mm",
            "MM/dd/yyyy"
        )
        for (pattern in patterns) {
            try {
                val fmt = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val parsed = fmt.parse(trimmed)
                if (parsed != null) return parsed.time
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return trimmed.toLongOrNull()
    }

    fun formatDateShort(epochMs: Long): String {
        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.US)
        return fmt.format(Date(epochMs))
    }

    fun formatDateMonthDay(epochMs: Long): String {
        val fmt = SimpleDateFormat("MMM d", Locale.US)
        return fmt.format(Date(epochMs))
    }

    fun formatIsoDisplay(iso: String): String {
        return formatDateShort(isoDateToEpoch(iso))
    }

    fun formatDateTimeShort(epochMs: Long): String {
        val fmt = SimpleDateFormat("MMM d · HH:mm", Locale.US)
        return fmt.format(Date(epochMs))
    }

    fun formatTimeOnly(epochMs: Long): String {
        val fmt = SimpleDateFormat("HH:mm", Locale.US)
        return fmt.format(Date(epochMs))
    }

    fun formatIsoDateTime(epochMs: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return fmt.format(Date(epochMs))
    }

    /** 0 = Mon, 1 = Tue, ..., 6 = Sun */
    fun dayOfWeekMondayBased(epochMs: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochMs
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
    }

    fun hourAndMinute(epochMs: Long): Pair<Int, Int> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = epochMs
        return cal.get(Calendar.HOUR_OF_DAY) to cal.get(Calendar.MINUTE)
    }

    fun startOfYearEpoch(nowMs: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = nowMs
        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun daysAgoEpoch(days: Int, nowMs: Long = System.currentTimeMillis()): Long {
        return nowMs - days.toLong() * ONE_DAY_MS
    }
}

object FormatUtils {
    private val currencyStandard = DecimalFormat("#,##0.00")
    private val currencyWhole = DecimalFormat("#,##0")

    fun formatCurrency(
        value: Double,
        signed: Boolean = false,
        compact: Boolean = false
    ): String {
        val absVal = abs(value)
        val sign = when {
            value < -0.004 -> "-"
            signed && value > 0.004 -> "+"
            else -> ""
        }
        if (compact) {
            return when {
                absVal >= 1_000_000.0 -> String.format(Locale.US, "%s$%.2fM", sign, absVal / 1_000_000.0)
                absVal >= 10_000.0 -> String.format(Locale.US, "%s$%.1fk", sign, absVal / 1_000.0)
                absVal >= 1_000.0 -> String.format(Locale.US, "%s$%.2fk", sign, absVal / 1_000.0)
                else -> String.format(Locale.US, "%s$%s", sign, currencyWhole.format(absVal))
            }
        }
        return "$sign$${currencyStandard.format(absVal)}"
    }

    fun formatPrice(value: Double): String {
        val absVal = abs(value)
        return when {
            absVal == 0.0 -> "0.00"
            absVal < 1.0 -> String.format(Locale.US, "%.5f", value)
            absVal < 10.0 -> String.format(Locale.US, "%.4f", value)
            else -> String.format(Locale.US, "%.2f", value)
        }
    }

    fun formatPercent(value: Double, signed: Boolean = false, decimals: Int = 1): String {
        val sign = if (signed && value > 0.0001) "+" else ""
        return String.format(Locale.US, "%s%.${decimals}f%%", sign, value)
    }

    fun formatRMultiple(value: Double): String {
        val sign = if (value > 0.004) "+" else ""
        return String.format(Locale.US, "%s%.2fR", sign, value)
    }

    fun formatRatio(value: Double): String {
        return when {
            value.isInfinite() -> "∞"
            value.isNaN() -> "—"
            else -> String.format(Locale.US, "%.2f", value)
        }
    }

    fun formatQuantity(qty: Double): String {
        return if (abs(qty - qty.toLong().toDouble()) < 1e-6) {
            qty.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", qty).trimEnd('0').trimEnd('.')
        }
    }

    fun formatDurationMinutes(minutes: Long): String {
        if (minutes <= 0L) return "< 1m"
        val days = minutes / (60 * 24)
        val hours = (minutes % (60 * 24)) / 60
        val mins = minutes % 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }
    }
}

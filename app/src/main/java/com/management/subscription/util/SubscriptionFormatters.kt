package com.management.subscription.util

import com.management.subscription.data.CurrencyTotal
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

object SubscriptionFormatters {

    private val shortDateFormatter = DateTimeFormatter.ofPattern("M월 d일", Locale.KOREA)
    private val weekdayDateFormatter = DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREA)
    private val headerDateFormatter = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREA)
    private val monthLabelFormatter = DateTimeFormatter.ofPattern("M월", Locale.KOREA)
    private val weekdayShortFormatter = DateTimeFormatter.ofPattern("E", Locale.KOREA)
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREA)
    private val fullDateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREA)
    private val reminderTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREA)

    fun currency(amountMinor: Long, currencyCode: String): String {
        return when (currencyCode) {
            "KRW" -> "${NumberFormat.getIntegerInstance(Locale.KOREA).format(amountMinor)}원"

            "USD" -> {
                val formatter = NumberFormat.getCurrencyInstance(Locale.US)
                formatter.currency = Currency.getInstance(currencyCode)
                formatter.maximumFractionDigits = 2
                formatter.minimumFractionDigits = 2
                formatter.format(BigDecimal(amountMinor).movePointLeft(2))
            }

            else -> {
                val formatter = NumberFormat.getCurrencyInstance(Locale.US)
                formatter.currency = Currency.getInstance(currencyCode)
                formatter.format(amountMinor)
            }
        }
    }

    fun editAmount(amountMinor: Long, currencyCode: String): String {
        return when (currencyCode) {
            "KRW" -> amountMinor.toString()
            "USD" -> DecimalFormat("0.00").format(BigDecimal(amountMinor).movePointLeft(2))
            else -> amountMinor.toString()
        }
    }

    fun parseAmountToMinor(input: String, currencyCode: String): Long? {
        val normalized = input.trim()
        if (normalized.isBlank()) return null

        return when (currencyCode) {
            "KRW" -> if (normalized.matches(Regex("^\\d+$"))) normalized.toLongOrNull() else null
            "USD" -> {
                if (!normalized.matches(Regex("^\\d+(\\.\\d{1,2})?$"))) return null
                runCatching {
                    BigDecimal(normalized)
                        .setScale(2, RoundingMode.UNNECESSARY)
                        .movePointRight(2)
                        .longValueExact()
                }.getOrNull()
            }

            else -> null
        }
    }

    /** 통화별 합계를 "72,480원 + $30.99"처럼 한 줄로. 서로 다른 통화는 더하지 않는다. */
    fun totals(totals: List<CurrencyTotal>): String {
        return totals.joinToString(" + ") { it.formattedAmount }
    }

    fun shortDate(date: LocalDate): String = shortDateFormatter.format(date)

    /** "10월 2일 (금)" */
    fun dateWithWeekday(date: LocalDate): String = weekdayDateFormatter.format(date)

    /** "10월 1일 목요일" */
    fun headerDate(date: LocalDate): String = headerDateFormatter.format(date)

    /** "10월" */
    fun monthLabel(date: LocalDate): String = monthLabelFormatter.format(date)

    /** "금" */
    fun weekdayShort(date: LocalDate): String = weekdayShortFormatter.format(date)

    fun fullDate(date: LocalDate): String = fullDateFormatter.format(date)

    fun monthTitle(month: YearMonth): String = monthFormatter.format(month.atDay(1))

    fun reminderTime(hour: Int, minute: Int): String {
        return reminderTimeFormatter.format(LocalTime.of(hour, minute))
    }
}

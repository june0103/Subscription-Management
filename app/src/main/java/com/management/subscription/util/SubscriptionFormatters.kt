package com.management.subscription.util

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
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREA)
    private val fullDateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일", Locale.KOREA)
    private val reminderTimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREA)

    fun currency(amountMinor: Long, currencyCode: String): String {
        return when (currencyCode) {
            "KRW" -> {
                val formatter = NumberFormat.getCurrencyInstance(Locale.KOREA)
                formatter.currency = Currency.getInstance(currencyCode)
                formatter.maximumFractionDigits = 0
                formatter.minimumFractionDigits = 0
                formatter.format(amountMinor)
            }

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

    fun shortDate(date: LocalDate): String = shortDateFormatter.format(date)

    fun fullDate(date: LocalDate): String = fullDateFormatter.format(date)

    fun monthTitle(month: YearMonth): String = monthFormatter.format(month.atDay(1))

    fun reminderTime(hour: Int, minute: Int): String {
        return reminderTimeFormatter.format(LocalTime.of(hour, minute))
    }
}

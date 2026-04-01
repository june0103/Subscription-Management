package com.management.subscription.data

data class CurrencyTotal(
    val currencyCode: String,
    val amountMinor: Long,
    val formattedAmount: String
)

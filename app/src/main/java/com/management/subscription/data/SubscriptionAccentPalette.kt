package com.management.subscription.data

import androidx.annotation.ColorRes
import com.management.subscription.R

object SubscriptionAccentPalette {
    private val colors = listOf(
        R.color.home_accent_coral,
        R.color.home_accent_sky,
        R.color.home_accent_mint,
        R.color.home_accent_gold,
        R.color.home_accent_rose
    )

    @ColorRes
    fun pick(subscriptionId: String): Int {
        val index = subscriptionId.hashCode().let { kotlin.math.abs(it) } % colors.size
        return colors[index]
    }
}

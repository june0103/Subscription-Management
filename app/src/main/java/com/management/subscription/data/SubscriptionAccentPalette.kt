package com.management.subscription.data

import androidx.annotation.ColorRes
import com.management.subscription.R

object SubscriptionAccentPalette {
    private val colors = listOf(
        R.color.cat_video,
        R.color.cat_music,
        R.color.cat_book,
        R.color.cat_game,
        R.color.cat_life,
        R.color.cat_work
    )

    @ColorRes
    fun pick(subscriptionId: String): Int {
        val index = subscriptionId.hashCode().let { kotlin.math.abs(it) } % colors.size
        return colors[index]
    }
}

package com.management.subscription.ui

import android.content.res.ColorStateList
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.management.subscription.R
import com.management.subscription.databinding.ViewDateTileBinding
import com.management.subscription.util.DueFormatter
import com.management.subscription.util.DueTone
import com.management.subscription.util.SubscriptionFormatters
import java.time.LocalDate

/** 디자인 시스템 DueLabel·DateTile을 화면 어디서나 같은 규칙으로 그린다. */
object DueViews {

    fun bindDueLabel(view: TextView, dDay: Int) {
        val tone = DueFormatter.tone(dDay)
        view.text = DueFormatter.label(dDay)
        view.backgroundTintList = ContextCompat.getColorStateList(view.context, tone.backgroundRes)
        view.setTextColor(ContextCompat.getColor(view.context, tone.textRes))
    }

    fun bindDateTile(binding: ViewDateTileBinding, date: LocalDate, dDay: Int) {
        val context = binding.root.context
        val (background, ink, subInk) = when (DueFormatter.tone(dDay)) {
            DueTone.TODAY -> Triple(R.color.due_today, R.color.on_due_today, R.color.on_due_today)
            DueTone.SOON -> Triple(R.color.due_soon_soft, R.color.due_soon_ink, R.color.due_soon_ink)
            DueTone.LATER -> Triple(R.color.surface_sunken, R.color.ink, R.color.ink_2)
        }
        binding.root.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, background))
        binding.tvTileMonth.text = SubscriptionFormatters.monthLabel(date)
        binding.tvTileDay.text = date.dayOfMonth.toString()
        binding.tvTileWeekday.text = if (dDay == 0) {
            context.getString(R.string.today)
        } else {
            SubscriptionFormatters.weekdayShort(date)
        }
        binding.tvTileDay.setTextColor(ContextCompat.getColor(context, ink))
        binding.tvTileMonth.setTextColor(ContextCompat.getColor(context, subInk))
        binding.tvTileWeekday.setTextColor(ContextCompat.getColor(context, subInk))
        binding.root.contentDescription =
            SubscriptionFormatters.dateWithWeekday(date) + ", " + DueFormatter.label(dDay)
    }
}

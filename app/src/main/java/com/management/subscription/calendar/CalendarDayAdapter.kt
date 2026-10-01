package com.management.subscription.calendar

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.management.subscription.R
import com.management.subscription.databinding.ItemCalendarDayBinding
import java.time.DayOfWeek

class CalendarDayAdapter(
    private val onDateClick: (CalendarDayUiModel) -> Unit
) : ListAdapter<CalendarDayUiModel, CalendarDayAdapter.CalendarDayViewHolder>(CalendarDayDiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarDayViewHolder {
        val binding = ItemCalendarDayBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CalendarDayViewHolder(binding, onDateClick)
    }

    override fun onBindViewHolder(holder: CalendarDayViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CalendarDayViewHolder(
        private val binding: ItemCalendarDayBinding,
        private val onDateClick: (CalendarDayUiModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CalendarDayUiModel) {
            binding.root.setOnClickListener { onDateClick(item) }
            binding.tvDay.text = item.dayLabel
            binding.root.contentDescription = item.contentDescription
            bindDayState(item)
            bindDots(item)
        }

        /** 오늘은 due_today 원, 선택한 날은 primary 원. 일요일은 붉은 글자. */
        private fun bindDayState(item: CalendarDayUiModel) {
            val context = binding.root.context
            val day = binding.tvDay

            val (circleColor, textColor) = when {
                item.isToday -> R.color.due_today to R.color.on_due_today
                item.isSelected -> R.color.primary to R.color.on_primary
                item.date.dayOfWeek == DayOfWeek.SUNDAY -> null to R.color.due_today_ink
                else -> null to R.color.ink
            }
            if (circleColor == null) {
                day.background = null
            } else {
                day.setBackgroundResource(R.drawable.bg_calendar_day)
                day.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, circleColor))
            }
            day.setTextColor(ContextCompat.getColor(context, textColor))
            day.setTypeface(
                null,
                if (item.isToday || item.isSelected || item.dotColors.isNotEmpty()) Typeface.BOLD else Typeface.NORMAL
            )
            binding.root.alpha = if (item.isInCurrentMonth) 1f else 0.4f
            binding.root.isSelected = item.isSelected
        }

        private fun bindDots(item: CalendarDayUiModel) {
            val dots = listOf(binding.dotOne, binding.dotTwo, binding.dotThree)
            dots.forEachIndexed { index, view ->
                val colorRes = item.dotColors.getOrNull(index)
                if (colorRes == null) {
                    view.visibility = if (index == 0) View.INVISIBLE else View.GONE
                } else {
                    view.visibility = View.VISIBLE
                    view.backgroundTintList =
                        ContextCompat.getColorStateList(binding.root.context, colorRes)
                }
            }
        }
    }

    private object CalendarDayDiffCallback :
        androidx.recyclerview.widget.DiffUtil.ItemCallback<CalendarDayUiModel>() {
        override fun areItemsTheSame(oldItem: CalendarDayUiModel, newItem: CalendarDayUiModel): Boolean {
            return oldItem.date == newItem.date
        }

        override fun areContentsTheSame(
            oldItem: CalendarDayUiModel,
            newItem: CalendarDayUiModel
        ): Boolean = oldItem == newItem
    }
}

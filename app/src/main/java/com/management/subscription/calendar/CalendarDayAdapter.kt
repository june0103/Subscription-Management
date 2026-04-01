package com.management.subscription.calendar

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.management.subscription.R
import com.management.subscription.databinding.ItemCalendarDayBinding

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
            bindCardState(item)
            bindDots(item)
        }

        private fun bindCardState(item: CalendarDayUiModel) {
            val context = binding.root.context
            val card = binding.cardDay

            val backgroundColor = when {
                item.isSelected -> ContextCompat.getColor(context, R.color.home_navy)
                else -> ContextCompat.getColor(context, R.color.home_surface)
            }
            val textColor = when {
                item.isSelected -> ContextCompat.getColor(context, R.color.white)
                item.isInCurrentMonth -> ContextCompat.getColor(context, R.color.home_text_primary)
                else -> ContextCompat.getColor(context, R.color.home_text_muted)
            }

            card.setCardBackgroundColor(backgroundColor)
            binding.tvDay.setTextColor(textColor)
            binding.root.alpha = if (item.isInCurrentMonth) 1f else 0.55f

            if (item.isToday && !item.isSelected) {
                card.strokeWidth = context.resources.getDimensionPixelSize(R.dimen.calendar_today_stroke)
                card.strokeColor = ContextCompat.getColor(context, R.color.home_navy)
            } else {
                card.strokeWidth = 0
                card.strokeColor = ContextCompat.getColor(context, android.R.color.transparent)
            }
        }

        private fun bindDots(item: CalendarDayUiModel) {
            val dots = listOf(binding.dotOne, binding.dotTwo, binding.dotThree)
            dots.forEachIndexed { index, view ->
                val colorRes = item.dotColors.getOrNull(index)
                if (colorRes == null) {
                    view.visibility = View.INVISIBLE
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

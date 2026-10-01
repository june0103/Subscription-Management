package com.management.subscription.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.management.subscription.R
import com.management.subscription.data.ScheduledSubscription
import com.management.subscription.databinding.ItemTimelineGroupBinding
import com.management.subscription.databinding.ItemTimelineRowBinding
import com.management.subscription.services.ServiceIconBinder
import com.management.subscription.services.ServiceIconResolver
import com.management.subscription.ui.DueViews
import com.management.subscription.ui.SubscriptionLabels
import com.management.subscription.util.SubscriptionFormatters

/** 홈 결제 타임라인: 날짜별 묶음 하나가 한 아이템 */
class TimelineAdapter(
    private val onSubscriptionClick: (subscriptionId: String) -> Unit
) : ListAdapter<TimelineGroup, TimelineAdapter.GroupViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = ItemTimelineGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding, onSubscriptionClick)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class GroupViewHolder(
        private val binding: ItemTimelineGroupBinding,
        private val onSubscriptionClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(group: TimelineGroup) {
            val context = binding.root.context
            DueViews.bindDateTile(binding.dateTile, group.date, group.dDay)
            DueViews.bindDueLabel(binding.tvDueLabel, group.dDay)
            binding.tvGroupSummary.text = context.getString(
                R.string.timeline_group_summary_format,
                group.schedules.size,
                SubscriptionFormatters.totals(group.totals)
            )

            binding.layoutRows.removeAllViews()
            val inflater = LayoutInflater.from(context)
            group.schedules.forEach { schedule ->
                val row = ItemTimelineRowBinding.inflate(inflater, binding.layoutRows, false)
                bindRow(row, schedule)
                binding.layoutRows.addView(row.root)
            }
        }

        private fun bindRow(row: ItemTimelineRowBinding, schedule: ScheduledSubscription) {
            val context = row.root.context
            val subscription = schedule.subscription
            row.tvName.text = subscription.name
            SubscriptionLabels.bindMeta(row.tvMeta, SubscriptionLabels.timelineMeta(context, subscription), subscription.category)
            row.tvAmount.text =
                SubscriptionFormatters.currency(subscription.amountMinor, subscription.currencyCode)
            ServiceIconBinder.bind(
                context = context,
                imageView = row.ivServiceIcon,
                badgeView = row.tvBadge,
                iconModel = ServiceIconResolver.resolve(context, subscription)
            )
            row.root.setOnClickListener { onSubscriptionClick(subscription.id) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<TimelineGroup>() {
        override fun areItemsTheSame(oldItem: TimelineGroup, newItem: TimelineGroup) =
            oldItem.date == newItem.date

        override fun areContentsTheSame(oldItem: TimelineGroup, newItem: TimelineGroup) =
            oldItem == newItem
    }
}

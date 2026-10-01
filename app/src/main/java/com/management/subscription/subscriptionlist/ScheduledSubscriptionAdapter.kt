package com.management.subscription.subscriptionlist

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.management.subscription.data.ScheduledSubscription
import com.management.subscription.databinding.ItemScheduledSubscriptionBinding
import com.management.subscription.services.ServiceIconBinder
import com.management.subscription.services.ServiceIconResolver
import com.management.subscription.ui.DueViews
import com.management.subscription.ui.SubscriptionLabels
import com.management.subscription.util.SubscriptionFormatters

class ScheduledSubscriptionAdapter(
    private val onItemClick: (ScheduledSubscriptionItemUiModel) -> Unit
) : ListAdapter<ScheduledSubscriptionItemUiModel, ScheduledSubscriptionAdapter.ScheduledSubscriptionViewHolder>(
    ScheduledSubscriptionDiffCallback
) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ScheduledSubscriptionViewHolder {
        val binding = ItemScheduledSubscriptionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScheduledSubscriptionViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ScheduledSubscriptionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ScheduledSubscriptionViewHolder(
        private val binding: ItemScheduledSubscriptionBinding,
        private val onItemClick: (ScheduledSubscriptionItemUiModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ScheduledSubscriptionItemUiModel) {
            binding.root.setOnClickListener { onItemClick(item) }
            binding.tvName.text = item.title
            binding.tvSubtitle.text = item.subtitle
            binding.tvAmount.text = item.amountLabel
            binding.tvDueLabel.isVisible = item.dDay != null
            item.dDay?.let { DueViews.bindDueLabel(binding.tvDueLabel, it) }
            ServiceIconBinder.bind(
                context = binding.root.context,
                imageView = binding.ivServiceIcon,
                badgeView = binding.tvBadge,
                iconModel = item.serviceIconModel
            )
        }
    }

    private object ScheduledSubscriptionDiffCallback :
        DiffUtil.ItemCallback<ScheduledSubscriptionItemUiModel>() {
        override fun areItemsTheSame(
            oldItem: ScheduledSubscriptionItemUiModel,
            newItem: ScheduledSubscriptionItemUiModel
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ScheduledSubscriptionItemUiModel,
            newItem: ScheduledSubscriptionItemUiModel
        ): Boolean = oldItem == newItem
    }

    companion object {
        /** 목록 한 줄의 기본 문구: 메타는 "매월 2일 · 1일 전 알림" */
        fun toUiModel(
            context: Context,
            schedule: ScheduledSubscription,
            showDue: Boolean = true
        ): ScheduledSubscriptionItemUiModel {
            val subscription = schedule.subscription
            return ScheduledSubscriptionItemUiModel(
                id = subscription.id,
                serviceIconModel = ServiceIconResolver.resolve(context, subscription),
                title = subscription.name,
                subtitle = SubscriptionLabels.cycleWithReminder(context, subscription),
                amountLabel = SubscriptionFormatters.currency(subscription.amountMinor, subscription.currencyCode),
                dDay = if (showDue) schedule.dDay else null
            )
        }
    }
}

package com.management.subscription.subscriptionlist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.management.subscription.databinding.ItemScheduledSubscriptionBinding
import com.management.subscription.services.ServiceIconBinder

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
            binding.tvCycle.text = item.cycleLabel
            binding.tvSubtitle.text = item.subtitle
            binding.tvAmount.text = item.amountLabel
            binding.tvTrailing.text = item.trailingLabel
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
}

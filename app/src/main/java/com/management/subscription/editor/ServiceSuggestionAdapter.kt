package com.management.subscription.editor

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import com.management.subscription.databinding.ItemServiceSuggestionBinding
import com.management.subscription.services.ServiceIconBinder
import com.management.subscription.services.ServiceSuggestionUiModel

class ServiceSuggestionAdapter(
    context: Context
) : BaseAdapter(), Filterable {

    private val inflater = LayoutInflater.from(context)
    private val noOpFilter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            return FilterResults().apply {
                values = items
                count = items.size
            }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            displayedItems = when (val values = results?.values) {
                is List<*> -> values.filterIsInstance<ServiceSuggestionUiModel>()
                else -> items
            }
            notifyDataSetChanged()
        }

        override fun convertResultToString(resultValue: Any?): CharSequence {
            return (resultValue as? ServiceSuggestionUiModel)?.displayName.orEmpty()
        }
    }

    private var items: List<ServiceSuggestionUiModel> = emptyList()
    private var displayedItems: List<ServiceSuggestionUiModel> = emptyList()

    fun submitList(newItems: List<ServiceSuggestionUiModel>) {
        items = newItems
        displayedItems = newItems
        notifyDataSetChanged()
    }

    fun getItemOrNull(position: Int): ServiceSuggestionUiModel? {
        return displayedItems.getOrNull(position)
    }

    override fun getCount(): Int = displayedItems.size

    override fun getItem(position: Int): ServiceSuggestionUiModel = displayedItems[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val binding = if (convertView == null) {
            ItemServiceSuggestionBinding.inflate(inflater, parent, false)
        } else {
            ItemServiceSuggestionBinding.bind(convertView)
        }
        val item = getItem(position)
        binding.tvServiceName.text = item.displayName
        binding.tvSource.text = parent.context.getString(item.sourceLabelRes)
        ServiceIconBinder.bind(
            context = parent.context,
            imageView = binding.ivServiceIcon,
            badgeView = binding.tvBadge,
            iconModel = item.iconModel
        )
        return binding.root
    }

    override fun getFilter(): Filter = noOpFilter
}

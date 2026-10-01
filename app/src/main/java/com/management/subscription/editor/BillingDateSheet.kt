package com.management.subscription.editor

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckedTextView
import android.widget.GridLayout
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.management.subscription.R
import com.management.subscription.databinding.SheetBillingDateBinding

/** DayPicker 바텀시트. 날짜를 누르면 결과를 넘기고 바로 닫힌다. */
class BillingDateSheet : BottomSheetDialogFragment() {

    private var _binding: SheetBillingDateBinding? = null
    private val binding get() = checkNotNull(_binding)

    private val isAnnual by lazy(LazyThreadSafetyMode.NONE) { requireArguments().getBoolean(ARG_ANNUAL) }
    private var selectedMonth = 1
    private var selectedDay = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetBillingDateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectedMonth = savedInstanceState?.getInt(ARG_MONTH) ?: requireArguments().getInt(ARG_MONTH, 1)
        selectedDay = savedInstanceState?.getInt(ARG_DAY) ?: requireArguments().getInt(ARG_DAY, 1)

        binding.tvSheetTitle.setText(
            if (isAnnual) R.string.billing_sheet_title_annual else R.string.billing_sheet_title_monthly
        )
        binding.layoutMonths.isVisible = isAnnual
        if (isAnnual) buildMonthChips()
        buildDayGrid()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_MONTH, selectedMonth)
        outState.putInt(ARG_DAY, selectedDay)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun buildMonthChips() {
        (1..12).forEach { month ->
            val chip = layoutInflater.inflate(R.layout.view_choice_chip, binding.chipGroupMonths, false) as Chip
            chip.id = View.generateViewId()
            chip.text = getString(R.string.editor_month_value_format, month)
            chip.isChecked = month == selectedMonth
            chip.setOnClickListener { selectedMonth = month }
            binding.chipGroupMonths.addView(chip)
        }
    }

    private fun buildDayGrid() {
        val size = resources.getDimensionPixelSize(R.dimen.calendar_day_circle) + 8.dp
        (1..31).forEach { day ->
            val cell = CheckedTextView(requireContext()).apply {
                text = day.toString()
                gravity = Gravity.CENTER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                textSize = 16f
                isChecked = day == selectedDay
                setBackgroundResource(R.drawable.bg_day_button)
                setTextColor(
                    ContextCompat.getColor(
                        context,
                        when {
                            isChecked -> R.color.on_primary
                            day > 28 -> R.color.ink_2
                            else -> R.color.ink
                        }
                    )
                )
                if (isChecked) setTypeface(typeface, android.graphics.Typeface.BOLD)
                contentDescription = getString(R.string.editor_day_value_format, day)
                isClickable = true
                isFocusable = true
                setOnClickListener { deliver(day) }
            }
            val params = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
            ).apply {
                // 열 너비는 균등하게 나누고, 원은 정사각형으로 가운데에 둔다.
                width = size
                height = size
                setGravity(Gravity.CENTER)
                setMargins(0, 2.dp, 0, 2.dp)
            }
            binding.gridDays.addView(cell, params)
        }
    }

    private fun deliver(day: Int) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY,
            bundleOf(RESULT_DAY to day, RESULT_MONTH to if (isAnnual) selectedMonth else null)
        )
        dismiss()
    }

    private val Int.dp: Int get() = (this * resources.displayMetrics.density).toInt()

    companion object {
        const val TAG = "BillingDateSheet"
        const val REQUEST_KEY = "billing_date_sheet"
        const val RESULT_DAY = "day"
        const val RESULT_MONTH = "month"
        private const val ARG_ANNUAL = "annual"
        private const val ARG_MONTH = "month"
        private const val ARG_DAY = "day"

        fun newInstance(annual: Boolean, month: Int, day: Int) = BillingDateSheet().apply {
            arguments = bundleOf(ARG_ANNUAL to annual, ARG_MONTH to month, ARG_DAY to day)
        }
    }
}

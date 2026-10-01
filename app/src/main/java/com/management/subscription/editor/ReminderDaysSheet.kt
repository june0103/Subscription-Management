package com.management.subscription.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.management.subscription.R
import com.management.subscription.data.BillingCycle
import com.management.subscription.databinding.SheetReminderDaysBinding
import com.management.subscription.domain.SubscriptionScheduleCalculator
import com.management.subscription.util.DueFormatter
import com.management.subscription.util.SubscriptionFormatters
import java.time.LocalDate

/** 알림 시점 직접 설정: 당일부터 30일 전까지 하루 단위로 고른다. */
class ReminderDaysSheet : BottomSheetDialogFragment() {

    private var _binding: SheetReminderDaysBinding? = null
    private val binding get() = checkNotNull(_binding)

    private var days = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SheetReminderDaysBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        days = savedInstanceState?.getInt(ARG_DAYS) ?: requireArguments().getInt(ARG_DAYS)
        binding.sliderReminder.valueTo = SubscriptionEditorViewModel.MAX_REMINDER_DAYS.toFloat()

        binding.buttonMinus.setOnClickListener { setDays(days - 1) }
        binding.buttonPlus.setOnClickListener { setDays(days + 1) }
        binding.sliderReminder.addOnChangeListener { _, value, fromUser ->
            if (fromUser) setDays(value.toInt())
        }
        binding.buttonConfirm.setOnClickListener {
            parentFragmentManager.setFragmentResult(REQUEST_KEY, bundleOf(RESULT_DAYS to days))
            dismiss()
        }
        setDays(days)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(ARG_DAYS, days)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun setDays(value: Int) {
        days = value.coerceIn(0, SubscriptionEditorViewModel.MAX_REMINDER_DAYS)
        binding.tvReminderValue.text = DueFormatter.reminder(days)
        binding.sliderReminder.value = days.toFloat()
        binding.buttonMinus.isEnabled = days > 0
        binding.buttonPlus.isEnabled = days < SubscriptionEditorViewModel.MAX_REMINDER_DAYS

        val args = requireArguments()
        val (payment, reminder) = SubscriptionScheduleCalculator.nextReminderDate(
            cycle = BillingCycle.valueOf(args.getString(ARG_CYCLE) ?: BillingCycle.MONTHLY.name),
            billingDay = args.getInt(ARG_BILLING_DAY),
            annualMonth = args.getInt(ARG_ANNUAL_MONTH),
            reminderDaysBefore = days,
            today = LocalDate.now()
        )
        binding.tvReminderPreview.text = getString(
            R.string.reminder_sheet_preview_format,
            SubscriptionFormatters.shortDate(payment),
            SubscriptionFormatters.dateWithWeekday(reminder),
            args.getString(ARG_TIME_LABEL).orEmpty()
        )
    }

    companion object {
        const val TAG = "ReminderDaysSheet"
        const val REQUEST_KEY = "reminder_days_sheet"
        const val RESULT_DAYS = "days"
        private const val ARG_DAYS = "days"
        private const val ARG_CYCLE = "cycle"
        private const val ARG_BILLING_DAY = "billingDay"
        private const val ARG_ANNUAL_MONTH = "annualMonth"
        private const val ARG_TIME_LABEL = "timeLabel"

        fun newInstance(
            days: Int,
            cycle: BillingCycle,
            billingDay: Int,
            annualMonth: Int,
            timeLabel: String
        ) = ReminderDaysSheet().apply {
            arguments = bundleOf(
                ARG_DAYS to days,
                ARG_CYCLE to cycle.name,
                ARG_BILLING_DAY to billingDay,
                ARG_ANNUAL_MONTH to annualMonth,
                ARG_TIME_LABEL to timeLabel
            )
        }
    }
}

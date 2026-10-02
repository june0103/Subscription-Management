package com.management.subscription.editor

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.management.subscription.R
import com.management.subscription.SubscriptionEditorArgs
import com.management.subscription.analytics.Analytics
import com.management.subscription.analytics.AnalyticsEvent
import android.content.res.ColorStateList
import android.text.SpannableStringBuilder
import androidx.core.content.ContextCompat
import androidx.core.text.color
import com.google.android.material.chip.Chip
import com.management.subscription.data.BillingCycle
import com.management.subscription.data.PaymentMethod
import com.management.subscription.data.SubscriptionCategory
import com.management.subscription.data.SettingsRepository
import com.management.subscription.data.SubscriptionRepository
import com.management.subscription.databinding.FragmentSubscriptionEditorBinding
import com.management.subscription.domain.SubscriptionScheduleCalculator
import com.management.subscription.services.ServiceSuggestionRepository
import com.management.subscription.ui.DueViews
import com.management.subscription.ui.SubscriptionLabels
import com.management.subscription.util.DueFormatter
import com.management.subscription.util.SubscriptionFormatters
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit

class SubscriptionEditorFragment : Fragment() {

    private var _binding: FragmentSubscriptionEditorBinding? = null
    private val binding get() = checkNotNull(_binding)

    /** 칩으로 바로 고르는 알림 시점. 나머지 값은 "직접 설정" 칩에 표시된다. */
    private val reminderPresets by lazy(LazyThreadSafetyMode.NONE) {
        mapOf(
            0 to binding.chipReminder0,
            1 to binding.chipReminder1,
            3 to binding.chipReminder3,
            7 to binding.chipReminder7
        )
    }
    private var isRenderingState = false
    private var appliedTextSyncVersion = -1
    private val categoryChips = mutableMapOf<SubscriptionCategory, Chip>()
    private val paymentChips = mutableMapOf<PaymentMethod, Chip>()

    private val repository by lazy(LazyThreadSafetyMode.NONE) {
        SubscriptionRepository.getInstance(requireContext().applicationContext)
    }
    private val suggestionRepository by lazy(LazyThreadSafetyMode.NONE) {
        ServiceSuggestionRepository.getInstance(requireContext().applicationContext)
    }
    private val subscriptionId by lazy(LazyThreadSafetyMode.NONE) {
        arguments?.getString(SubscriptionEditorArgs.KEY_SUBSCRIPTION_ID)
    }
    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProvider(
            this,
            SubscriptionEditorViewModel.factory(
                repository = repository,
                suggestionRepository = suggestionRepository,
                settingsRepository = SettingsRepository.getInstance(requireContext().applicationContext),
                subscriptionId = subscriptionId
            )
        )[SubscriptionEditorViewModel::class.java]
    }
    private val suggestionAdapter by lazy(LazyThreadSafetyMode.NONE) {
        ServiceSuggestionAdapter(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // 새 뷰의 입력칸은 비어 있으므로 첫 렌더에서 상태 값으로 채운다.
        appliedTextSyncVersion = -1
        configureResults()
        configureToolbar()
        configureServiceNameInput()
        configureInputs()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::renderState) }
                launch { viewModel.events.collect(::handleEvent) }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun configureToolbar() {
        binding.toolbarEditor.setNavigationOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun configureResults() {
        setFragmentResultListener(REQUEST_KEY_DISCOVERY_RESULT) { _, bundle ->
            viewModel.onSuggestionSelected(
                displayName = bundle.getString(RESULT_DISPLAY_NAME).orEmpty(),
                serviceKey = bundle.getString(RESULT_SERVICE_KEY),
                linkedPackageName = bundle.getString(RESULT_LINKED_PACKAGE),
                source = "discovery"
            )
        }
        childFragmentManager.setFragmentResultListener(BillingDateSheet.REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            viewModel.onBillingDateChanged(
                billingDay = bundle.getInt(BillingDateSheet.RESULT_DAY),
                annualMonth = bundle.getInt(BillingDateSheet.RESULT_MONTH).takeIf { it in 1..12 }
            )
        }
        childFragmentManager.setFragmentResultListener(ReminderDaysSheet.REQUEST_KEY, viewLifecycleOwner) { _, bundle ->
            viewModel.onReminderDaysChanged(bundle.getInt(ReminderDaysSheet.RESULT_DAYS))
        }
    }

    private fun configureServiceNameInput() {
        binding.editServiceName.setAdapter(suggestionAdapter)
        binding.editServiceName.setOnItemClickListener { _, _, position, _ ->
            val suggestion = suggestionAdapter.getItemOrNull(position)
            if (suggestion == null) {
                Log.w(TAG, "Ignored suggestion click because adapter item was missing at position=$position")
                return@setOnItemClickListener
            }
            viewModel.onSuggestionSelected(suggestion)
        }
        binding.editServiceName.doAfterTextChanged { text ->
            if (!isRenderingState) {
                binding.editServiceName.dismissDropDown()
                viewModel.onNameChanged(text?.toString().orEmpty())
            }
        }
        binding.editServiceName.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                updateSuggestionDropdown(viewModel.uiState.value)
            } else {
                binding.editServiceName.dismissDropDown()
            }
        }
    }

    private fun configureInputs() {
        binding.buttonDiscoverServices.setOnClickListener {
            findNavController().navigate(R.id.subscriptionDiscoveryFragment)
        }

        binding.editAmount.doAfterTextChanged { text ->
            if (!isRenderingState) viewModel.onAmountChanged(text?.toString().orEmpty())
        }
        binding.groupCurrency.setOnCheckedChangeListener { _, checkedId ->
            if (!isRenderingState) {
                viewModel.onCurrencyChanged(if (checkedId == R.id.radioUsd) "USD" else "KRW")
            }
        }
        binding.groupCycle.setOnCheckedChangeListener { _, checkedId ->
            if (!isRenderingState) {
                viewModel.onCycleChanged(
                    if (checkedId == R.id.radioAnnual) BillingCycle.ANNUAL else BillingCycle.MONTHLY
                )
            }
        }

        binding.pickerBillingDate.setOnClickListener {
            val state = viewModel.uiState.value
            if (childFragmentManager.findFragmentByTag(BillingDateSheet.TAG) == null) {
                BillingDateSheet.newInstance(
                    annual = state.billingCycle == BillingCycle.ANNUAL,
                    month = state.annualMonth,
                    day = state.billingDay
                ).show(childFragmentManager, BillingDateSheet.TAG)
            }
        }

        reminderPresets.forEach { (days, chip) ->
            chip.text = DueFormatter.reminder(days)
            chip.setOnClickListener { viewModel.onReminderDaysChanged(days) }
        }
        binding.chipReminderCustom.setOnClickListener {
            // 시트를 닫기 전까지는 기존 선택을 유지한다.
            renderReminder(viewModel.uiState.value)
            openReminderSheet()
        }

        buildCategoryChips()
        buildPaymentChips()
        binding.tvPaymentLabel.text = optionalLabel(R.string.editor_label_payment)
        binding.tvMemoLabel.text = optionalLabel(R.string.editor_label_memo)
        binding.editMemo.doAfterTextChanged { text ->
            if (!isRenderingState) viewModel.onMemoChanged(text?.toString().orEmpty())
        }

        binding.buttonSave.setOnClickListener { viewModel.submit() }

        binding.buttonDelete.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.editor_delete_dialog_title)
                .setMessage(R.string.editor_delete_dialog_body)
                .setNegativeButton(R.string.editor_delete_dialog_cancel, null)
                .setPositiveButton(R.string.editor_delete_dialog_confirm) { _, _ ->
                    viewModel.delete()
                }
                .show()
        }
    }

    /** 카테고리 칩: 앞에 카테고리 색 점, 선택되면 점은 on_primary. */
    private fun buildCategoryChips() {
        categoryChips.clear()
        binding.chipGroupCategory.removeAllViews()
        val onPrimary = ContextCompat.getColor(requireContext(), R.color.on_primary)
        SubscriptionCategory.entries.forEach { category ->
            val chip = newChip(category.label)
            chip.chipIcon = ContextCompat.getDrawable(requireContext(), R.drawable.bg_dot)
            chip.chipIconSize = resources.displayMetrics.density * 8
            chip.iconStartPadding = resources.displayMetrics.density * 4
            chip.isChipIconVisible = true
            chip.chipIconTint = ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                intArrayOf(onPrimary, ContextCompat.getColor(requireContext(), category.colorRes))
            )
            chip.setOnClickListener {
                viewModel.onCategoryChanged(if (chip.isChecked) category else null)
            }
            categoryChips[category] = chip
            binding.chipGroupCategory.addView(chip)
        }
    }

    private fun buildPaymentChips() {
        paymentChips.clear()
        binding.chipGroupPayment.removeAllViews()
        PaymentMethod.entries.forEach { method ->
            val chip = newChip(method.label)
            // 한 번 더 누르면 선택을 풀 수 있다(선택 사항).
            chip.setOnClickListener {
                viewModel.onPaymentMethodChanged(if (chip.isChecked) method else null)
            }
            paymentChips[method] = chip
            binding.chipGroupPayment.addView(chip)
        }
    }

    private fun newChip(label: String): Chip {
        val chip = layoutInflater.inflate(R.layout.view_choice_chip, binding.chipGroupCategory, false) as Chip
        chip.id = View.generateViewId()
        chip.text = label
        return chip
    }

    /** "결제수단 (선택)"에서 "(선택)"만 옅게 */
    private fun optionalLabel(labelRes: Int): CharSequence {
        return SpannableStringBuilder()
            .append(getString(labelRes))
            .append(" ")
            .color(ContextCompat.getColor(requireContext(), R.color.ink_3)) {
                append(getString(R.string.editor_optional))
            }
    }

    private fun openReminderSheet() {
        if (childFragmentManager.findFragmentByTag(ReminderDaysSheet.TAG) != null) return
        Analytics.log(AnalyticsEvent.ReminderCustomOpen)
        val state = viewModel.uiState.value
        ReminderDaysSheet.newInstance(
            days = state.reminderDaysBefore,
            cycle = state.billingCycle,
            billingDay = state.billingDay,
            annualMonth = state.annualMonth,
            timeLabel = SubscriptionFormatters.reminderTime(state.reminderHour, state.reminderMinute)
        ).show(childFragmentManager, ReminderDaysSheet.TAG)
    }

    private fun renderState(state: SubscriptionEditorUiState) {
        isRenderingState = true

        binding.toolbarEditor.title = getString(
            if (state.isEditMode) R.string.editor_title_edit else R.string.editor_title_add
        )
        binding.buttonDelete.isVisible = state.showDelete

        suggestionAdapter.submitList(state.suggestions)

        // 키보드로 친 값은 화면이 원본이다. 코드에서 채울 때(추천 선택, 불러오기)만 덮어쓴다.
        if (appliedTextSyncVersion != state.textSyncVersion) {
            appliedTextSyncVersion = state.textSyncVersion
            if (binding.editServiceName.text?.toString() != state.name) {
                binding.editServiceName.setText(state.name, false)
                binding.editServiceName.setSelection(state.name.length)
            }
            if (binding.editAmount.text?.toString() != state.amountText) {
                binding.editAmount.setText(state.amountText)
                binding.editAmount.setSelection(state.amountText.length)
            }
            if (binding.editMemo.text?.toString() != state.memo) {
                binding.editMemo.setText(state.memo)
            }
        }

        binding.tilServiceName.error = state.nameErrorResId?.let(::getString)
        binding.tilAmount.error = state.amountErrorResId?.let(::getString)

        val isUsd = state.currencyCode == "USD"
        binding.groupCurrency.check(if (isUsd) R.id.radioUsd else R.id.radioKrw)
        binding.tilAmount.prefixText = if (isUsd) "$" else null
        binding.tilAmount.suffixText = if (isUsd) null else getString(R.string.unit_won)
        val amountInputType = if (isUsd) {
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        } else {
            InputType.TYPE_CLASS_NUMBER
        }
        // 매번 바꾸면 키보드가 다시 열리므로 달라졌을 때만.
        if (binding.editAmount.inputType != amountInputType) {
            binding.editAmount.inputType = amountInputType
        }

        binding.groupCycle.check(
            if (state.billingCycle == BillingCycle.ANNUAL) R.id.radioAnnual else R.id.radioMonthly
        )
        renderBillingDate(state)
        renderReminder(state)
        categoryChips.forEach { (category, chip) -> chip.isChecked = category == state.category }
        binding.tvCategoryHint.isVisible = state.category != null && !state.isCategoryChosenByUser
        paymentChips.forEach { (method, chip) -> chip.isChecked = method == state.paymentMethod }
        renderSummary(state)

        isRenderingState = false
        updateSuggestionDropdown(state)
    }

    private fun renderBillingDate(state: SubscriptionEditorUiState) {
        binding.tvBillingValue.text = SubscriptionLabels.cycle(
            requireContext(),
            state.billingCycle,
            state.annualMonth,
            state.billingDay
        )
        val nextPayment = nextPaymentDate(state)
        val dDay = ChronoUnit.DAYS.between(state.today, nextPayment).toInt()
        binding.tvBillingHint.text = getString(
            R.string.editor_billing_hint_format,
            SubscriptionFormatters.dateWithWeekday(nextPayment),
            DueFormatter.relative(dDay)
        )
    }

    private fun renderReminder(state: SubscriptionEditorUiState) {
        val presetChip = reminderPresets[state.reminderDaysBefore]
        if (presetChip != null) {
            presetChip.isChecked = true
            binding.chipReminderCustom.text = getString(R.string.reminder_custom)
        } else {
            binding.chipReminderCustom.isChecked = true
            binding.chipReminderCustom.text = DueFormatter.reminder(state.reminderDaysBefore)
        }

        val (_, reminderDate) = SubscriptionScheduleCalculator.nextReminderDate(
            cycle = state.billingCycle,
            billingDay = state.billingDay,
            annualMonth = state.annualMonth,
            reminderDaysBefore = state.reminderDaysBefore,
            today = state.today
        )
        binding.tvReminderHint.text = getString(
            R.string.editor_reminder_hint_format,
            SubscriptionFormatters.dateWithWeekday(reminderDate),
            SubscriptionFormatters.reminderTime(state.reminderHour, state.reminderMinute)
        )
    }

    private fun renderSummary(state: SubscriptionEditorUiState) {
        val nextPayment = nextPaymentDate(state)
        val dDay = ChronoUnit.DAYS.between(state.today, nextPayment).toInt()
        DueViews.bindDueLabel(binding.tvSummaryDue, dDay)

        val amountMinor = SubscriptionFormatters.parseAmountToMinor(state.amountText, state.currencyCode)
        val (_, reminderDate) = SubscriptionScheduleCalculator.nextReminderDate(
            cycle = state.billingCycle,
            billingDay = state.billingDay,
            annualMonth = state.annualMonth,
            reminderDaysBefore = state.reminderDaysBefore,
            today = state.today
        )
        val payment = if (amountMinor != null) {
            SubscriptionFormatters.shortDate(nextPayment) + " " +
                SubscriptionFormatters.currency(amountMinor, state.currencyCode)
        } else {
            SubscriptionFormatters.shortDate(nextPayment)
        }
        binding.tvSummary.text = getString(
            R.string.editor_summary_format,
            payment,
            SubscriptionFormatters.shortDate(reminderDate)
        )
    }

    private fun nextPaymentDate(state: SubscriptionEditorUiState) =
        SubscriptionScheduleCalculator.nextPaymentDate(
            cycle = state.billingCycle,
            billingDay = state.billingDay,
            annualMonth = state.annualMonth,
            fromDate = state.today
        )

    private fun handleEvent(event: EditorEvent) {
        when (event) {
            is EditorEvent.Saved -> {
                Snackbar.make(
                    binding.root,
                    getString(R.string.editor_save_success_format, event.name),
                    Snackbar.LENGTH_SHORT
                ).show()
                findNavController().popBackStack()
            }

            EditorEvent.Deleted -> {
                Snackbar.make(binding.root, R.string.editor_delete_success_message, Snackbar.LENGTH_SHORT)
                    .show()
                findNavController().popBackStack()
            }

            EditorEvent.Close -> findNavController().popBackStack()
        }
    }

    private fun updateSuggestionDropdown(state: SubscriptionEditorUiState) {
        val currentText = binding.editServiceName.text?.toString().orEmpty()
        val shouldShow = binding.editServiceName.hasFocus() &&
            currentText.isNotBlank() &&
            state.suggestionQuery == currentText &&
            !state.isSuggestionsLoading &&
            suggestionAdapter.count > 0

        if (shouldShow) {
            binding.editServiceName.showDropDown()
        } else {
            binding.editServiceName.dismissDropDown()
        }
    }

    companion object {
        private const val TAG = "SubscriptionEditor"
        const val REQUEST_KEY_DISCOVERY_RESULT = "subscription_discovery_result"
        const val RESULT_DISPLAY_NAME = "displayName"
        const val RESULT_SERVICE_KEY = "serviceKey"
        const val RESULT_LINKED_PACKAGE = "linkedPackageName"
    }
}

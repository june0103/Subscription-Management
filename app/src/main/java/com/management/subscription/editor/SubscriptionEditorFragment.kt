package com.management.subscription.editor

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.isVisible
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
import com.management.subscription.data.BillingCycle
import com.management.subscription.data.SubscriptionRepository
import com.management.subscription.databinding.FragmentSubscriptionEditorBinding
import com.management.subscription.services.ServiceSuggestionUiModel
import com.management.subscription.services.ServiceSuggestionRepository
import kotlinx.coroutines.launch

class SubscriptionEditorFragment : Fragment() {

    private var _binding: FragmentSubscriptionEditorBinding? = null
    private val binding get() = checkNotNull(_binding)

    private val reminderItems = listOf(1, 3, 7, 14)
    private var isRenderingState = false

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
        configureDiscoveryResult()
        configureToolbar()
        configureServiceNameInput()
        configureSpinners()
        configureListeners()

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

    private fun configureDiscoveryResult() {
        setFragmentResultListener(REQUEST_KEY_DISCOVERY_RESULT) { _, bundle ->
            viewModel.onSuggestionSelected(
                displayName = bundle.getString(RESULT_DISPLAY_NAME).orEmpty(),
                serviceKey = bundle.getString(RESULT_SERVICE_KEY),
                linkedPackageName = bundle.getString(RESULT_LINKED_PACKAGE)
            )
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
                val value = text?.toString().orEmpty()
                binding.editServiceName.dismissDropDown()
                viewModel.onNameChanged(value)
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

    private fun configureSpinners() {
        val billingDayItems = (1..31).map { getString(R.string.editor_day_value_format, it) }
        val annualMonthItems = (1..12).map { getString(R.string.editor_month_value_format, it) }
        val reminderLabels =
            reminderItems.map { getString(R.string.editor_reminder_days_value_format, it) }

        binding.spinnerBillingDay.adapter = buildSpinnerAdapter(billingDayItems)
        binding.spinnerAnnualMonth.adapter = buildSpinnerAdapter(annualMonthItems)
        binding.spinnerReminder.adapter = buildSpinnerAdapter(reminderLabels)
    }

    private fun configureListeners() {
        binding.radioGroupCycle.setOnCheckedChangeListener { _, checkedId ->
            if (!isRenderingState) {
                binding.layoutAnnualMonth.isVisible = checkedId == R.id.radioAnnual
            }
        }

        binding.buttonDiscoverServices.setOnClickListener {
            findNavController().navigate(R.id.subscriptionDiscoveryFragment)
        }

        binding.toggleCurrency.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked && !isRenderingState) {
                updateAmountInputType(
                    if (checkedId == R.id.buttonCurrencyUsd) "USD" else "KRW"
                )
            }
        }

        binding.buttonSave.setOnClickListener {
            viewModel.submit(
                name = binding.editServiceName.text?.toString().orEmpty(),
                amountText = binding.editAmount.text?.toString().orEmpty(),
                currencyCode = selectedCurrencyCode(),
                billingCycle = selectedBillingCycle(),
                billingDay = binding.spinnerBillingDay.selectedItemPosition + 1,
                annualMonth = binding.spinnerAnnualMonth.selectedItemPosition + 1,
                reminderDaysBefore = reminderItems[binding.spinnerReminder.selectedItemPosition]
            )
        }

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

    private fun renderState(state: SubscriptionEditorUiState) {
        isRenderingState = true

        binding.toolbarEditor.title = getString(
            if (state.isEditMode) R.string.editor_title_edit else R.string.editor_title_add
        )
        binding.buttonDelete.isVisible = state.showDelete
        binding.layoutAnnualMonth.isVisible = state.billingCycle == BillingCycle.ANNUAL

        suggestionAdapter.submitList(state.suggestions)

        if (binding.editServiceName.text?.toString() != state.name) {
            binding.editServiceName.setText(state.name, false)
            binding.editServiceName.setSelection(state.name.length)
        }
        if (binding.editAmount.text?.toString() != state.amountText) {
            binding.editAmount.setText(state.amountText)
        }

        binding.tilServiceName.error = state.nameErrorResId?.let(::getString)
        binding.tilAmount.error = state.amountErrorResId?.let(::getString)

        if (state.currencyCode == "USD") {
            binding.toggleCurrency.check(R.id.buttonCurrencyUsd)
        } else {
            binding.toggleCurrency.check(R.id.buttonCurrencyKrw)
        }
        updateAmountInputType(state.currencyCode)

        if (state.billingCycle == BillingCycle.ANNUAL) {
            binding.radioAnnual.isChecked = true
        } else {
            binding.radioMonthly.isChecked = true
        }

        binding.spinnerBillingDay.setSelection((state.billingDay - 1).coerceAtLeast(0))
        binding.spinnerAnnualMonth.setSelection((state.annualMonth - 1).coerceAtLeast(0))
        binding.spinnerReminder.setSelection(
            reminderItems.indexOf(state.reminderDaysBefore).coerceAtLeast(0)
        )

        isRenderingState = false

        updateSuggestionDropdown(state)
    }

    private fun handleEvent(event: EditorEvent) {
        when (event) {
            EditorEvent.Saved -> {
                Snackbar.make(binding.root, R.string.editor_save_success_message, Snackbar.LENGTH_SHORT)
                    .show()
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

    private fun updateAmountInputType(currencyCode: String) {
        binding.editAmount.inputType = if (currencyCode == "USD") {
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        } else {
            InputType.TYPE_CLASS_NUMBER
        }
    }

    private fun selectedCurrencyCode(): String {
        return if (binding.toggleCurrency.checkedButtonId == R.id.buttonCurrencyUsd) "USD" else "KRW"
    }

    private fun selectedBillingCycle(): BillingCycle {
        return if (binding.radioGroupCycle.checkedRadioButtonId == R.id.radioAnnual) {
            BillingCycle.ANNUAL
        } else {
            BillingCycle.MONTHLY
        }
    }

    private fun buildSpinnerAdapter(items: List<String>): ArrayAdapter<String> {
        return ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            items
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
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

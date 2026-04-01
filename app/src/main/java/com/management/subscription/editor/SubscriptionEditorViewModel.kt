package com.management.subscription.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.management.subscription.R
import com.management.subscription.data.BillingCycle
import com.management.subscription.data.SubscriptionDraft
import com.management.subscription.data.SubscriptionRepository
import com.management.subscription.services.ResolvedServiceIdentity
import com.management.subscription.services.ServiceSuggestionRepository
import com.management.subscription.services.ServiceSuggestionUiModel
import com.management.subscription.services.SubscriptionServiceCatalog
import com.management.subscription.util.SubscriptionFormatters
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubscriptionEditorViewModel(
    private val repository: SubscriptionRepository,
    private val suggestionRepository: ServiceSuggestionRepository,
    private val subscriptionId: String?
) : ViewModel() {

    private var selectedIdentityLabel: String? = null

    private val _uiState = MutableStateFlow(
        SubscriptionEditorUiState(
            isLoading = subscriptionId != null,
            isEditMode = subscriptionId != null,
            showDelete = subscriptionId != null
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>()
    val events = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            val initialState = if (subscriptionId == null) {
                _uiState.value.copy(isLoading = false)
            } else {
                loadInitialState()
            }
            _uiState.value = initialState
            suggestionRepository.preload()
            refreshSuggestionsForCurrentName()
        }
    }

    fun onNameChanged(name: String) {
        val shouldClearIdentity = selectedIdentityLabel != null &&
            SubscriptionServiceCatalog.normalize(name) !=
            SubscriptionServiceCatalog.normalize(selectedIdentityLabel.orEmpty())

        if (shouldClearIdentity) {
            selectedIdentityLabel = null
        }

        _uiState.value = _uiState.value.copy(
            name = name,
            suggestionQuery = name,
            isSuggestionsLoading = false,
            serviceKey = if (shouldClearIdentity) null else _uiState.value.serviceKey,
            linkedPackageName = if (shouldClearIdentity) null else _uiState.value.linkedPackageName,
            suggestions = if (name.isBlank()) emptyList() else suggestionRepository.suggestionsFor(name),
            nameErrorResId = null
        )
    }

    fun onSuggestionSelected(suggestion: ServiceSuggestionUiModel) {
        onSuggestionSelected(
            displayName = suggestion.displayName,
            serviceKey = suggestion.serviceKey,
            linkedPackageName = suggestion.linkedPackageName
        )
    }

    fun onSuggestionSelected(
        displayName: String,
        serviceKey: String?,
        linkedPackageName: String?
    ) {
        selectedIdentityLabel = displayName
        _uiState.value = _uiState.value.copy(
            name = displayName,
            suggestionQuery = displayName,
            isSuggestionsLoading = false,
            serviceKey = serviceKey,
            linkedPackageName = linkedPackageName,
            suggestions = suggestionRepository.suggestionsFor(displayName),
            nameErrorResId = null
        )
    }

    fun submit(
        name: String,
        amountText: String,
        currencyCode: String,
        billingCycle: BillingCycle,
        billingDay: Int,
        annualMonth: Int,
        reminderDaysBefore: Int
    ) {
        val trimmedName = name.trim()
        val amountMinor = SubscriptionFormatters.parseAmountToMinor(amountText, currencyCode)
        val annualMonthValue = if (billingCycle == BillingCycle.ANNUAL) annualMonth else null
        val currentState = _uiState.value

        val nameError = if (trimmedName.isBlank()) R.string.editor_error_name_required else null
        val amountError = when {
            amountText.isBlank() -> R.string.editor_error_amount_required
            amountMinor == null -> if (currencyCode == "USD") {
                R.string.editor_error_amount_invalid_usd
            } else {
                R.string.editor_error_amount_invalid_krw
            }

            else -> null
        }

        if (nameError != null || amountError != null) {
            _uiState.value = currentState.copy(
                name = name,
                suggestionQuery = name,
                amountText = amountText,
                currencyCode = currencyCode,
                billingCycle = billingCycle,
                billingDay = billingDay,
                annualMonth = annualMonth,
                reminderDaysBefore = reminderDaysBefore,
                nameErrorResId = nameError,
                amountErrorResId = amountError
            )
            return
        }

        val resolvedIdentity = resolveIdentity(trimmedName, currentState)

        viewModelScope.launch {
            val draft = SubscriptionDraft(
                name = trimmedName,
                amountMinor = checkNotNull(amountMinor),
                currencyCode = currencyCode,
                serviceKey = resolvedIdentity.serviceKey,
                linkedPackageName = resolvedIdentity.linkedPackageName,
                billingCycle = billingCycle,
                billingDay = billingDay,
                annualMonth = annualMonthValue,
                reminderDaysBefore = reminderDaysBefore
            )

            if (subscriptionId == null) {
                repository.createSubscription(draft)
                _events.emit(EditorEvent.Saved)
            } else {
                repository.updateSubscription(subscriptionId, draft)
                _events.emit(EditorEvent.Saved)
            }
        }
    }

    fun delete() {
        val targetId = subscriptionId ?: return
        viewModelScope.launch {
            repository.deleteSubscription(targetId)
            _events.emit(EditorEvent.Deleted)
        }
    }

    private suspend fun loadInitialState(): SubscriptionEditorUiState {
        if (subscriptionId == null) {
            return _uiState.value.copy(isLoading = false)
        }

        val subscription = repository.getSubscription(subscriptionId)
        if (subscription == null) {
            _uiState.value = SubscriptionEditorUiState()
            _events.emit(EditorEvent.Close)
            return SubscriptionEditorUiState()
        }

        selectedIdentityLabel = subscription.name
        return SubscriptionEditorUiState(
            isLoading = false,
            isSuggestionsLoading = true,
            isEditMode = true,
            name = subscription.name,
            suggestionQuery = subscription.name,
            amountText = SubscriptionFormatters.editAmount(
                subscription.amountMinor,
                subscription.currencyCode
            ),
            currencyCode = subscription.currencyCode,
            serviceKey = subscription.serviceKey,
            linkedPackageName = subscription.linkedPackageName,
            billingCycle = subscription.billingCycle,
            billingDay = subscription.billingDay,
            annualMonth = subscription.annualMonth ?: 1,
            reminderDaysBefore = subscription.reminderDaysBefore,
            showDelete = true
        )
    }

    private fun resolveIdentity(
        trimmedName: String,
        currentState: SubscriptionEditorUiState
    ): ResolvedServiceIdentity {
        return if (currentState.serviceKey != null || currentState.linkedPackageName != null) {
            ResolvedServiceIdentity(
                serviceKey = currentState.serviceKey,
                linkedPackageName = currentState.linkedPackageName
            )
        } else {
            suggestionRepository.resolveFreeTextIdentity(trimmedName)
        }
    }

    private fun refreshSuggestionsForCurrentName() {
        val currentState = _uiState.value
        val currentName = currentState.name
        _uiState.value = currentState.copy(
            isLoading = false,
            isSuggestionsLoading = false,
            suggestionQuery = currentName,
            suggestions = if (currentName.isBlank()) {
                emptyList()
            } else {
                suggestionRepository.suggestionsFor(currentName)
            }
        )
    }

    companion object {
        fun factory(
            repository: SubscriptionRepository,
            suggestionRepository: ServiceSuggestionRepository,
            subscriptionId: String?
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SubscriptionEditorViewModel(
                        repository = repository,
                        suggestionRepository = suggestionRepository,
                        subscriptionId = subscriptionId
                    ) as T
                }
            }
        }
    }
}

sealed interface EditorEvent {
    data object Saved : EditorEvent
    data object Deleted : EditorEvent
    data object Close : EditorEvent
}

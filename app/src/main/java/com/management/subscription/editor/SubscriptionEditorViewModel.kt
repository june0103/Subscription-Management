package com.management.subscription.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.management.subscription.R
import com.management.subscription.analytics.Analytics
import com.management.subscription.analytics.AnalyticsEvent
import com.management.subscription.data.BillingCycle
import com.management.subscription.data.PaymentMethod
import com.management.subscription.data.SubscriptionCategory
import com.management.subscription.data.SettingsRepository
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SubscriptionEditorViewModel(
    private val repository: SubscriptionRepository,
    private val suggestionRepository: ServiceSuggestionRepository,
    private val settingsRepository: SettingsRepository,
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
            val settings = settingsRepository.getSettings()
            _uiState.update {
                it.copy(reminderHour = settings.reminderHour, reminderMinute = settings.reminderMinute)
            }
            if (subscriptionId != null) {
                loadSubscription(subscriptionId)
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
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

        _uiState.update { state ->
            state.copy(
                name = name,
                suggestionQuery = name,
                isSuggestionsLoading = false,
                serviceKey = if (shouldClearIdentity) null else state.serviceKey,
                identitySource = if (shouldClearIdentity) "manual" else state.identitySource,
                linkedPackageName = if (shouldClearIdentity) null else state.linkedPackageName,
                suggestions = if (name.isBlank()) emptyList() else suggestionRepository.suggestionsFor(name),
                nameErrorResId = null
            )
        }
    }

    fun onSuggestionSelected(suggestion: ServiceSuggestionUiModel) {
        onSuggestionSelected(
            displayName = suggestion.displayName,
            serviceKey = suggestion.serviceKey,
            linkedPackageName = suggestion.linkedPackageName,
            source = "suggestion"
        )
    }

    /** source: 등록 화면 드롭다운이면 suggestion, 설치된 앱에서 찾기면 discovery */
    fun onSuggestionSelected(
        displayName: String,
        serviceKey: String?,
        linkedPackageName: String?,
        source: String
    ) {
        selectedIdentityLabel = displayName
        val catalogCategory = SubscriptionServiceCatalog.defaultCategory(serviceKey)
            ?: SubscriptionServiceCatalog.findByPackage(linkedPackageName)
                ?.let { SubscriptionServiceCatalog.defaultCategory(it.key) }
        _uiState.update {
            it.copy(
                name = displayName,
                suggestionQuery = displayName,
                isSuggestionsLoading = false,
                serviceKey = serviceKey,
                linkedPackageName = linkedPackageName,
                category = if (it.isCategoryChosenByUser) it.category else catalogCategory ?: it.category,
                identitySource = source,
                suggestions = suggestionRepository.suggestionsFor(displayName),
                nameErrorResId = null,
                textSyncVersion = it.textSyncVersion + 1
            )
        }
    }

    fun onAmountChanged(amountText: String) {
        _uiState.update { it.copy(amountText = amountText, amountErrorResId = null) }
    }

    fun onCurrencyChanged(currencyCode: String) {
        _uiState.update { it.copy(currencyCode = currencyCode, amountErrorResId = null) }
    }

    fun onCycleChanged(cycle: BillingCycle) {
        _uiState.update { it.copy(billingCycle = cycle) }
    }

    fun onBillingDateChanged(billingDay: Int, annualMonth: Int?) {
        _uiState.update {
            it.copy(
                billingDay = billingDay.coerceIn(1, 31),
                annualMonth = annualMonth?.coerceIn(1, 12) ?: it.annualMonth
            )
        }
    }

    fun onReminderDaysChanged(daysBefore: Int) {
        _uiState.update { it.copy(reminderDaysBefore = daysBefore.coerceIn(0, MAX_REMINDER_DAYS)) }
    }

    fun onCategoryChanged(category: SubscriptionCategory?) {
        _uiState.update { it.copy(category = category, isCategoryChosenByUser = true) }
    }

    fun onPaymentMethodChanged(paymentMethod: PaymentMethod?) {
        _uiState.update { it.copy(paymentMethod = paymentMethod) }
    }

    fun onMemoChanged(memo: String) {
        _uiState.update { it.copy(memo = memo.take(MAX_MEMO_LENGTH)) }
    }

    fun submit() {
        val state = _uiState.value
        val trimmedName = state.name.trim()
        val amountMinor = SubscriptionFormatters.parseAmountToMinor(state.amountText, state.currencyCode)

        val nameError = if (trimmedName.isBlank()) R.string.editor_error_name_required else null
        val amountError = when {
            state.amountText.isBlank() -> R.string.editor_error_amount_required
            amountMinor == null -> if (state.currencyCode == "USD") {
                R.string.editor_error_amount_invalid_usd
            } else {
                R.string.editor_error_amount_invalid_krw
            }

            else -> null
        }

        if (nameError != null || amountError != null) {
            Analytics.log(AnalyticsEvent.SubscriptionSaveError(field = if (nameError != null) "name" else "amount"))
            _uiState.update { it.copy(nameErrorResId = nameError, amountErrorResId = amountError) }
            return
        }

        val resolvedIdentity = resolveIdentity(trimmedName, state)

        viewModelScope.launch {
            val draft = SubscriptionDraft(
                name = trimmedName,
                amountMinor = checkNotNull(amountMinor),
                currencyCode = state.currencyCode,
                serviceKey = resolvedIdentity.serviceKey,
                linkedPackageName = resolvedIdentity.linkedPackageName,
                billingCycle = state.billingCycle,
                billingDay = state.billingDay,
                annualMonth = if (state.billingCycle == BillingCycle.ANNUAL) state.annualMonth else null,
                reminderDaysBefore = state.reminderDaysBefore,
                // 직접 입력한 이름이 카탈로그 서비스와 맞으면 카테고리를 채워 준다.
                category = state.category
                    ?: SubscriptionServiceCatalog.defaultCategory(resolvedIdentity.serviceKey)
                        .takeUnless { state.isCategoryChosenByUser },
                paymentMethod = state.paymentMethod,
                memo = state.memo
            )

            if (subscriptionId == null) {
                repository.createSubscription(draft)
            } else {
                repository.updateSubscription(subscriptionId, draft)
            }
            Analytics.log(
                AnalyticsEvent.SubscriptionSaved(
                    isEdit = subscriptionId != null,
                    source = state.identitySource,
                    // 카탈로그 키만 보낸다. 직접 입력한 서비스명은 보내지 않는다.
                    serviceKey = draft.serviceKey,
                    category = draft.category,
                    paymentMethod = draft.paymentMethod,
                    cycle = draft.billingCycle,
                    reminderDays = draft.reminderDaysBefore,
                    currencyCode = draft.currencyCode,
                    hasMemo = !draft.memo.isNullOrBlank()
                )
            )
            _events.emit(EditorEvent.Saved(trimmedName, isNew = subscriptionId == null))
        }
    }

    fun delete() {
        val targetId = subscriptionId ?: return
        viewModelScope.launch {
            repository.deleteSubscription(targetId)
            Analytics.log(AnalyticsEvent.SubscriptionDeleted)
            _events.emit(EditorEvent.Deleted)
        }
    }

    private suspend fun loadSubscription(id: String) {
        val subscription = repository.getSubscription(id)
        if (subscription == null) {
            _events.emit(EditorEvent.Close)
            return
        }

        selectedIdentityLabel = subscription.name
        _uiState.update {
            it.copy(
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
                annualMonth = subscription.annualMonth ?: it.annualMonth,
                reminderDaysBefore = subscription.reminderDaysBefore,
                category = subscription.category,
                isCategoryChosenByUser = subscription.category != null,
                paymentMethod = subscription.paymentMethod,
                memo = subscription.memo.orEmpty(),
                identitySource = "existing",
                showDelete = true,
                textSyncVersion = it.textSyncVersion + 1
            )
        }
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
        _uiState.update { state ->
            state.copy(
                isLoading = false,
                isSuggestionsLoading = false,
                suggestionQuery = state.name,
                suggestions = if (state.name.isBlank()) {
                    emptyList()
                } else {
                    suggestionRepository.suggestionsFor(state.name)
                }
            )
        }
    }

    companion object {
        const val MAX_REMINDER_DAYS = 30
        const val MAX_MEMO_LENGTH = 500

        fun factory(
            repository: SubscriptionRepository,
            suggestionRepository: ServiceSuggestionRepository,
            settingsRepository: SettingsRepository,
            subscriptionId: String?
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SubscriptionEditorViewModel(
                        repository = repository,
                        suggestionRepository = suggestionRepository,
                        settingsRepository = settingsRepository,
                        subscriptionId = subscriptionId
                    ) as T
                }
            }
        }
    }
}

sealed interface EditorEvent {
    data class Saved(val name: String, val isNew: Boolean) : EditorEvent
    data object Deleted : EditorEvent
    data object Close : EditorEvent
}

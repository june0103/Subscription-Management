package com.management.subscription.editor

import com.management.subscription.data.BillingCycle
import com.management.subscription.services.ServiceSuggestionUiModel

data class SubscriptionEditorUiState(
    val isLoading: Boolean = false,
    val isSuggestionsLoading: Boolean = true,
    val isEditMode: Boolean = false,
    val name: String = "",
    val suggestionQuery: String = "",
    val amountText: String = "",
    val currencyCode: String = "KRW",
    val serviceKey: String? = null,
    val linkedPackageName: String? = null,
    val suggestions: List<ServiceSuggestionUiModel> = emptyList(),
    val billingCycle: BillingCycle = BillingCycle.MONTHLY,
    val billingDay: Int = 1,
    val annualMonth: Int = 1,
    val reminderDaysBefore: Int = 3,
    val showDelete: Boolean = false,
    val nameErrorResId: Int? = null,
    val amountErrorResId: Int? = null
)

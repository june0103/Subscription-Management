package com.management.subscription.services

import androidx.annotation.StringRes

data class ServiceSuggestionUiModel(
    val displayName: String,
    val serviceKey: String?,
    val linkedPackageName: String?,
    @StringRes val sourceLabelRes: Int,
    val iconModel: ServiceIconModel
)

data class ResolvedServiceIdentity(
    val serviceKey: String?,
    val linkedPackageName: String?
)

internal data class RankedServiceSuggestion(
    val suggestion: ServiceSuggestionUiModel,
    val isInstalled: Boolean,
    val matchRank: SearchMatchRank
)

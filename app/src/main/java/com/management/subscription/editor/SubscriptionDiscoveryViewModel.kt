package com.management.subscription.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.management.subscription.services.ServiceSuggestionRepository
import com.management.subscription.services.ServiceSuggestionUiModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SubscriptionDiscoveryViewModel(
    private val suggestionRepository: ServiceSuggestionRepository
) : ViewModel() {

    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow(SubscriptionDiscoveryUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    init {
        loadSuggestions()
    }

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        loadSuggestions()
    }

    private fun loadSuggestions() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val query = _uiState.value.query
            _uiState.value = _uiState.value.copy(isLoading = true)
            suggestionRepository.preload()
            val suggestions = if (query.isBlank()) {
                suggestionRepository.discoverySuggestions()
            } else {
                suggestionRepository.suggestionsFor(query, limit = 24)
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                suggestions = suggestions
            )
        }
    }

    companion object {
        fun factory(
            suggestionRepository: ServiceSuggestionRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SubscriptionDiscoveryViewModel(
                        suggestionRepository = suggestionRepository
                    ) as T
                }
            }
        }
    }
}

data class SubscriptionDiscoveryUiState(
    val isLoading: Boolean = false,
    val query: String = "",
    val suggestions: List<ServiceSuggestionUiModel> = emptyList()
)

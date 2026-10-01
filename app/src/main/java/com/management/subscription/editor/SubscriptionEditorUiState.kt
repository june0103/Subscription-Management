package com.management.subscription.editor

import com.management.subscription.data.BillingCycle
import com.management.subscription.data.PaymentMethod
import com.management.subscription.data.SubscriptionCategory
import com.management.subscription.services.ServiceSuggestionUiModel
import java.time.LocalDate

/**
 * 등록 화면의 모든 입력값. 화면은 입력이 바뀔 때마다 ViewModel에 알리고, 그리기만 한다.
 * (예전에는 금액·결제일이 화면에만 있어서 상태를 다시 그릴 때 지워졌다.)
 */
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
    val billingDay: Int = LocalDate.now().dayOfMonth,
    val annualMonth: Int = LocalDate.now().monthValue,
    val reminderDaysBefore: Int = 1,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val category: SubscriptionCategory? = null,
    /** 사용자가 카테고리를 직접 고르면 서비스를 바꿔도 자동 선택으로 덮어쓰지 않는다. */
    val isCategoryChosenByUser: Boolean = false,
    val paymentMethod: PaymentMethod? = null,
    val memo: String = "",
    val today: LocalDate = LocalDate.now(),
    val showDelete: Boolean = false,
    val nameErrorResId: Int? = null,
    val amountErrorResId: Int? = null,
    /**
     * 입력칸을 코드에서 채워야 할 때만 올라가는 번호(추천 선택, 기존 구독 불러오기).
     * 키보드 입력은 올리지 않으므로, 빠르게 입력하는 중에 늦게 도착한 상태가 글자를 지우지 않는다.
     */
    val textSyncVersion: Int = 0
)

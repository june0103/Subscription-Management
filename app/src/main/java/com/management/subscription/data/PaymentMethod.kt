package com.management.subscription.data

/** 결제수단. DB에는 enum 이름(TEXT)으로 저장하므로 이름을 바꾸지 않는다. */
enum class PaymentMethod(val label: String) {
    CARD("카드"),
    KAKAO_PAY("카카오페이"),
    NAVER_PAY("네이버페이"),
    TOSS_PAY("토스페이"),
    GOOGLE_PLAY("구글 플레이"),
    APP_STORE("앱스토어"),
    WEB("웹 결제"),
    BANK_TRANSFER("계좌이체");

    companion object {
        fun fromStored(value: String?): PaymentMethod? =
            entries.firstOrNull { it.name == value }
    }
}

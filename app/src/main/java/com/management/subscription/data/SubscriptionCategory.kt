package com.management.subscription.data

import androidx.annotation.ColorRes
import com.management.subscription.R

/**
 * 구독 카테고리. DB에는 enum 이름(TEXT)으로 저장하므로 이름을 바꾸지 않는다.
 * 색은 디자인 시스템 cat-* 토큰이며, 항상 텍스트 라벨과 함께 쓴다.
 */
enum class SubscriptionCategory(
    val label: String,
    @ColorRes val colorRes: Int
) {
    VIDEO("영상", R.color.cat_video),
    MUSIC("음악", R.color.cat_music),
    BOOK("독서", R.color.cat_book),
    GAME("게임", R.color.cat_game),
    LIFE("생활", R.color.cat_life),
    WORK("생산성", R.color.cat_work),
    ETC("기타", R.color.cat_etc);

    companion object {
        fun fromStored(value: String?): SubscriptionCategory? =
            entries.firstOrNull { it.name == value }
    }
}

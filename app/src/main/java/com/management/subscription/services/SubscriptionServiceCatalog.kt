package com.management.subscription.services

import androidx.annotation.DrawableRes
import com.management.subscription.R
import com.management.subscription.data.SubscriptionCategory

data class SubscriptionServiceDefinition(
    val key: String,
    val displayName: String,
    val aliases: List<String>,
    @DrawableRes val logoRes: Int,
    val packageNames: Set<String>,
    val searchKeys: List<HangulSearchKey>
)

object SubscriptionServiceCatalog {

    private val services = listOf(
        service(
            key = "youtube_premium",
            displayName = "YouTube Premium",
            aliases = listOf("youtube", "youtube premium", "\uC720\uD29C\uBE0C", "\uC720\uD29C\uBE0C \uD504\uB9AC\uBBF8\uC5C4"),
            logoRes = R.drawable.ic_service_youtube_premium,
            packageNames = setOf("com.google.android.youtube")
        ),
        service(
            key = "netflix",
            displayName = "Netflix",
            aliases = listOf("netflix", "\uB137\uD50C\uB9AD\uC2A4"),
            logoRes = R.drawable.ic_service_netflix,
            packageNames = setOf("com.netflix.mediaclient")
        ),
        service(
            key = "disney_plus",
            displayName = "Disney+",
            aliases = listOf("disney+", "disney plus", "\uB514\uC988\uB2C8+", "\uB514\uC988\uB2C8 \uD50C\uB7EC\uC2A4"),
            logoRes = R.drawable.ic_service_disney_plus,
            packageNames = setOf("com.disney.disneyplus")
        ),
        service(
            key = "prime_video",
            displayName = "Prime Video",
            aliases = listOf("prime video", "amazon prime video", "\uD504\uB77C\uC784 \uBE44\uB514\uC624"),
            logoRes = R.drawable.ic_service_prime_video,
            packageNames = setOf("com.amazon.avod.thirdpartyclient")
        ),
        service(
            key = "tving",
            displayName = "TVING",
            aliases = listOf("tving", "\uD2F0\uBE59"),
            logoRes = R.drawable.ic_service_tving,
            packageNames = setOf("net.cj.cjhv.gs.tving")
        ),
        service(
            key = "wavve",
            displayName = "wavve",
            aliases = listOf("wavve", "\uC6E8\uC774\uBE0C"),
            logoRes = R.drawable.ic_service_wavve,
            packageNames = setOf("kr.co.captv.pooqV2")
        ),
        service(
            key = "watcha",
            displayName = "WATCHA",
            aliases = listOf("watcha", "\uC649\uCC28"),
            logoRes = R.drawable.ic_service_watcha,
            packageNames = setOf("com.frograms.wplay")
        ),
        service(
            key = "laftel",
            displayName = "Laftel",
            aliases = listOf("laftel", "\uB77C\uD504\uD154"),
            logoRes = R.drawable.ic_service_laftel,
            packageNames = setOf("laftel.net.laftel")
        ),
        service(
            key = "spotify",
            displayName = "Spotify",
            aliases = listOf("spotify", "\uC2A4\uD3EC\uD2F0\uD30C\uC774"),
            logoRes = R.drawable.ic_service_spotify,
            packageNames = setOf("com.spotify.music")
        ),
        service(
            key = "apple_music",
            displayName = "Apple Music",
            aliases = listOf("apple music", "\uC560\uD50C \uBBA4\uC9C1"),
            logoRes = R.drawable.ic_service_apple_music,
            packageNames = setOf("com.apple.android.music")
        ),
        service(
            key = "apple_tv_plus",
            displayName = "Apple TV+",
            aliases = listOf("apple tv+", "apple tv plus", "\uC560\uD50C tv+", "\uC560\uD50C tv \uD50C\uB7EC\uC2A4"),
            logoRes = R.drawable.ic_service_apple_tv_plus,
            packageNames = setOf("com.apple.atve.androidtv.appletv")
        ),
        service(
            key = "google_one",
            displayName = "Google One",
            aliases = listOf("google one", "\uAD6C\uAE00 \uC6D0"),
            logoRes = R.drawable.ic_service_google_one,
            packageNames = setOf("com.google.android.apps.subscriptions.red")
        ),
        service(
            key = "chatgpt_plus",
            displayName = "ChatGPT Plus",
            aliases = listOf("chatgpt", "chatgpt plus", "\uCC57gpt", "\uCC57gpt \uD50C\uB7EC\uC2A4"),
            logoRes = R.drawable.ic_service_chatgpt_plus,
            packageNames = setOf("com.openai.chatgpt")
        ),
        service(
            key = "claude_pro",
            displayName = "Claude Pro",
            aliases = listOf("claude", "claude pro", "\uD074\uB85C\uB4DC", "\uD074\uB85C\uB4DC \uD504\uB85C"),
            logoRes = R.drawable.ic_service_claude_pro,
            packageNames = setOf("com.anthropic.claude")
        ),
        service(
            key = "gemini_advanced",
            displayName = "Gemini Advanced",
            aliases = listOf("gemini", "gemini advanced", "\uC81C\uBBF8\uB098\uC774", "\uC81C\uBBF8\uB098\uC774 \uC5B4\uB4DC\uBC34\uC2A4"),
            logoRes = R.drawable.ic_service_gemini_advanced,
            packageNames = setOf("com.google.android.apps.bard")
        ),
        service(
            key = "perplexity_pro",
            displayName = "Perplexity Pro",
            aliases = listOf("perplexity", "perplexity pro"),
            logoRes = R.drawable.ic_service_perplexity_pro,
            packageNames = setOf("ai.perplexity.app.android")
        ),
        service(
            key = "copilot_pro",
            displayName = "Copilot Pro",
            aliases = listOf("copilot", "copilot pro", "microsoft copilot", "\uCF54\uD30C\uC77C\uB7FF"),
            logoRes = R.drawable.ic_service_copilot_pro,
            packageNames = setOf("com.microsoft.copilot")
        ),
        service(
            key = "notion_ai",
            displayName = "Notion AI",
            aliases = listOf("notion", "notion ai", "\uB178\uC158", "\uB178\uC158 ai"),
            logoRes = R.drawable.ic_service_notion_ai,
            packageNames = setOf("notion.id")
        ),
        service(
            key = "grammarly_pro",
            displayName = "Grammarly Pro",
            aliases = listOf("grammarly", "grammarly pro"),
            logoRes = R.drawable.ic_service_grammarly_pro,
            packageNames = setOf("com.grammarly.android.keyboard")
        ),
        service(
            key = "canva_pro",
            displayName = "Canva Pro",
            aliases = listOf("canva", "canva pro"),
            logoRes = R.drawable.ic_service_canva_pro,
            packageNames = setOf("com.canva.editor")
        ),
        service(
            key = "coupang_wow",
            displayName = "\uCFE0\uD321 \uC640\uC6B0",
            aliases = listOf(
                "\uCFE0\uD321 \uC640\uC6B0",
                "\uCFE0\uD321\uC640\uC6B0",
                "\uCFE0\uD321",
                "\uCFE0",
                "coupang wow",
                "coupang",
                "wow"
            ),
            logoRes = R.drawable.ic_service_coupang_wow,
            packageNames = setOf("com.coupang.mobile")
        ),
        service(
            key = "naver_plus",
            displayName = "\uB124\uC774\uBC84\uD50C\uB7EC\uC2A4 \uBA64\uBC84\uC2ED",
            aliases = listOf("\uB124\uC774\uBC84\uD50C\uB7EC\uC2A4", "\uB124\uC774\uBC84 \uD50C\uB7EC\uC2A4", "naver plus", "naver plus membership"),
            logoRes = R.drawable.ic_service_naver_plus,
            packageNames = setOf("com.nhn.android.search")
        ),
        service(
            key = "amazon_prime",
            displayName = "Amazon Prime",
            aliases = listOf("amazon prime", "\uC544\uB9C8\uC874 \uD504\uB77C\uC784"),
            logoRes = R.drawable.ic_service_amazon_prime,
            packageNames = setOf("com.amazon.mShop.android.shopping")
        ),
        service(
            key = "baemin_club",
            displayName = "\uBC30\uBBFC\uD074\uB7FD",
            aliases = listOf("\uBC30\uBBFC\uD074\uB7FD", "\uBC30\uB2EC\uC758\uBBFC\uC871", "\uBC30\uBBFC", "baemin club", "baemin"),
            logoRes = R.drawable.ic_service_baemin_club,
            packageNames = setOf("com.sampleapp")
        ),
        service(
            key = "yogiyo_pass",
            displayName = "\uC694\uAE30\uC694 Plus",
            aliases = listOf("\uC694\uAE30\uC694", "\uC694\uAE30\uC694 plus", "yogiyo", "yogiyo plus"),
            logoRes = R.drawable.ic_service_yogiyo_pass,
            packageNames = setOf("com.fineapp.yogiyo")
        ),
        service(
            key = "coupang_eats",
            displayName = "\uCFE0\uD321\uC774\uCE20",
            aliases = listOf("\uCFE0\uD321\uC774\uCE20", "\uCFE0\uD321 \uC774\uCE20", "\uCFE0\uD321", "coupang eats", "coupang"),
            logoRes = R.drawable.ic_service_coupang_eats,
            packageNames = setOf("com.coupang.mobile.eats")
        ),
        service(
            key = "millie",
            displayName = "\uBC00\uB9AC\uC758\uC11C\uC7AC",
            aliases = listOf("\uBC00\uB9AC\uC758\uC11C\uC7AC", "\uBC00\uB9AC", "millie", "millie's library"),
            logoRes = R.drawable.ic_service_millie,
            packageNames = setOf("kr.co.millie.millieshelf")
        ),
        service(
            key = "ridi_select",
            displayName = "RIDI Select",
            aliases = listOf("ridi", "ridi select", "\uB9AC\uB514", "\uB9AC\uB514 \uC140\uB809\uD2B8"),
            logoRes = R.drawable.ic_service_ridi_select,
            packageNames = setOf("com.ridibooks.android")
        ),
        service(
            key = "xbox_game_pass",
            displayName = "Xbox Game Pass",
            aliases = listOf("xbox game pass", "game pass", "\uC5D1\uC2A4\uBC15\uC2A4 \uAC8C\uC784\uD328\uC2A4"),
            logoRes = R.drawable.ic_service_xbox_game_pass,
            packageNames = setOf("com.gamepass")
        ),
        service(
            key = "playstation_plus",
            displayName = "PlayStation Plus",
            aliases = listOf("playstation plus", "ps plus", "\uD50C\uB808\uC774\uC2A4\uD14C\uC774\uC158 \uD50C\uB7EC\uC2A4"),
            logoRes = R.drawable.ic_service_playstation_plus,
            packageNames = setOf("com.scee.psxandroid")
        )
    )

    private val recommendedOrder = listOf(
        "youtube_premium",
        "netflix",
        "tving",
        "wavve",
        "chatgpt_plus",
        "claude_pro",
        "gemini_advanced",
        "coupang_wow",
        "naver_plus",
        "baemin_club",
        "yogiyo_pass",
        "amazon_prime",
        "spotify",
        "watcha",
        "notion_ai",
        "coupang_eats"
    )

    val recommendedServices: List<SubscriptionServiceDefinition> =
        recommendedOrder.mapNotNull(::findByKey)

    val allServices: List<SubscriptionServiceDefinition>
        get() = services

    /** 카탈로그 서비스를 고르면 미리 선택해 두는 카테고리. 사용자가 바꿀 수 있다. */
    fun defaultCategory(key: String?): SubscriptionCategory? = defaultCategories[key]

    private val defaultCategories: Map<String, SubscriptionCategory> = buildMap {
        listOf(
            "youtube_premium", "netflix", "disney_plus", "prime_video", "tving", "wavve",
            "watcha", "laftel", "apple_tv_plus"
        ).forEach { put(it, SubscriptionCategory.VIDEO) }
        listOf("spotify", "apple_music").forEach { put(it, SubscriptionCategory.MUSIC) }
        listOf("millie", "ridi_select").forEach { put(it, SubscriptionCategory.BOOK) }
        listOf("xbox_game_pass", "playstation_plus").forEach { put(it, SubscriptionCategory.GAME) }
        listOf(
            "coupang_wow", "naver_plus", "amazon_prime", "baemin_club", "yogiyo_pass", "coupang_eats"
        ).forEach { put(it, SubscriptionCategory.LIFE) }
        listOf(
            "google_one", "chatgpt_plus", "claude_pro", "gemini_advanced", "perplexity_pro",
            "copilot_pro", "notion_ai", "grammarly_pro", "canva_pro"
        ).forEach { put(it, SubscriptionCategory.WORK) }
    }

    fun findByKey(key: String?): SubscriptionServiceDefinition? {
        return services.firstOrNull { it.key == key }
    }

    fun findByPackage(packageName: String?): SubscriptionServiceDefinition? {
        if (packageName.isNullOrBlank()) return null
        return services.firstOrNull { packageName in it.packageNames }
    }

    fun findInstalledAppMatch(
        label: String,
        packageName: String?
    ): SubscriptionServiceDefinition? {
        return findByPackage(packageName) ?: findExactMatch(label)
    }

    fun findExactMatch(name: String): SubscriptionServiceDefinition? {
        val normalizedName = normalize(name)
        return services.firstOrNull { service ->
            normalize(service.displayName) == normalizedName ||
                service.aliases.any { normalize(it) == normalizedName }
        }
    }

    fun normalize(value: String): String {
        return HangulSearchKey.from(value).normalized
    }

    private fun service(
        key: String,
        displayName: String,
        aliases: List<String>,
        @DrawableRes logoRes: Int,
        packageNames: Set<String> = emptySet()
    ): SubscriptionServiceDefinition {
        return SubscriptionServiceDefinition(
            key = key,
            displayName = displayName,
            aliases = aliases,
            logoRes = logoRes,
            packageNames = packageNames,
            searchKeys = listOf(displayName, *aliases.toTypedArray()).map(HangulSearchKey::from)
        )
    }
}

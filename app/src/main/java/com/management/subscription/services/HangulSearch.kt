package com.management.subscription.services

data class HangulSearchKey(
    val normalized: String,
    val jamo: String,
    val choseong: String
) {
    val isBlank: Boolean
        get() = normalized.isBlank() && jamo.isBlank() && choseong.isBlank()

    companion object {
        fun from(value: String): HangulSearchKey {
            val compact = value.lowercase().replace("\\s+".toRegex(), "")
            val jamoBuilder = StringBuilder(compact.length * 2)
            val choseongBuilder = StringBuilder(compact.length)

            compact.forEach { char ->
                when {
                    char in HANGUL_SYLLABLE_START..HANGUL_SYLLABLE_END -> {
                        val syllableIndex = char.code - HANGUL_SYLLABLE_START.code
                        val choseongIndex = syllableIndex / (JUNGSEONG.size * JONGSEONG.size)
                        val jungseongIndex = syllableIndex % (JUNGSEONG.size * JONGSEONG.size) / JONGSEONG.size
                        val jongseongIndex = syllableIndex % JONGSEONG.size

                        jamoBuilder.append(CHOSEONG[choseongIndex])
                        jamoBuilder.append(JUNGSEONG[jungseongIndex])
                        if (jongseongIndex > 0) {
                            jamoBuilder.append(JONGSEONG[jongseongIndex])
                        }
                        choseongBuilder.append(CHOSEONG[choseongIndex])
                    }

                    char in COMPATIBILITY_JAMO_RANGE -> {
                        jamoBuilder.append(char)
                        if (char in CHOSEONG_SET) {
                            choseongBuilder.append(char)
                        }
                    }

                    else -> {
                        jamoBuilder.append(char)
                        choseongBuilder.append(char)
                    }
                }
            }

            return HangulSearchKey(
                normalized = compact,
                jamo = jamoBuilder.toString(),
                choseong = choseongBuilder.toString()
            )
        }

        private const val HANGUL_SYLLABLE_START = '\uAC00'
        private const val HANGUL_SYLLABLE_END = '\uD7A3'
        private val COMPATIBILITY_JAMO_RANGE = '\u3131'..'\u3163'

        private val CHOSEONG = charArrayOf(
            '\u3131', '\u3132', '\u3134', '\u3137', '\u3138',
            '\u3139', '\u3141', '\u3142', '\u3143', '\u3145',
            '\u3146', '\u3147', '\u3148', '\u3149', '\u314A',
            '\u314B', '\u314C', '\u314D', '\u314E'
        )
        private val JUNGSEONG = charArrayOf(
            '\u314F', '\u3150', '\u3151', '\u3152', '\u3153',
            '\u3154', '\u3155', '\u3156', '\u3157', '\u3158',
            '\u3159', '\u315A', '\u315B', '\u315C', '\u315D',
            '\u315E', '\u315F', '\u3160', '\u3161', '\u3162',
            '\u3163'
        )
        private val JONGSEONG = charArrayOf(
            '\u0000', '\u3131', '\u3132', '\u3133', '\u3134',
            '\u3135', '\u3136', '\u3137', '\u3139', '\u313A',
            '\u313B', '\u313C', '\u313D', '\u313E', '\u313F',
            '\u3140', '\u3141', '\u3142', '\u3144', '\u3145',
            '\u3146', '\u3147', '\u3148', '\u314A', '\u314B',
            '\u314C', '\u314D', '\u314E'
        )
        private val CHOSEONG_SET = CHOSEONG.toSet()
    }
}

data class SearchMatchRank(
    val tier: Int,
    val prefixMatch: Boolean
)

object HangulSearchMatcher {

    fun match(
        query: HangulSearchKey,
        candidate: HangulSearchKey
    ): SearchMatchRank? {
        if (query.isBlank) return SearchMatchRank(tier = 0, prefixMatch = true)

        return when {
            candidate.normalized.startsWith(query.normalized) -> SearchMatchRank(0, true)
            candidate.jamo.startsWith(query.jamo) -> SearchMatchRank(1, true)
            candidate.choseong.startsWith(query.choseong) -> SearchMatchRank(2, true)
            candidate.normalized.contains(query.normalized) -> SearchMatchRank(3, false)
            candidate.jamo.contains(query.jamo) -> SearchMatchRank(4, false)
            candidate.choseong.contains(query.choseong) -> SearchMatchRank(5, false)
            else -> null
        }
    }
}

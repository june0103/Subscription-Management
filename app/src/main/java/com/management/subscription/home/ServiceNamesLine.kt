package com.management.subscription.home

/**
 * 오늘 결제 배너의 서비스 이름 줄을 만든다.
 *
 * - 한 줄 폭에 들어가는 만큼만 이름을 보여 주고 나머지는 "외 N개"로 줄인다.
 * - 이름 안에서는 줄이 바뀌지 않게 묶는다("Claude / Pro", "쿠팡 / 이츠"처럼 갈라지지 않게).
 *   첫 이름 하나만으로도 넘치는 예외적인 경우에도 이름 중간이 아니라 " · " 사이에서만 줄이 바뀐다.
 */
object ServiceNamesLine {

    private const val SEPARATOR = " · "
    private const val WORD_JOINER = '⁠'
    private const val NO_BREAK_SPACE = ' '

    /**
     * @param maxWidth 한 줄에 쓸 수 있는 폭(px)
     * @param measure 실제 글꼴로 잰 문자열 폭
     * @param more 남은 개수를 받아 " 외 N개" 같은 꼬리를 돌려준다
     */
    fun fit(
        names: List<String>,
        maxWidth: Float,
        measure: (String) -> Float,
        more: (Int) -> String
    ): String {
        if (names.isEmpty()) return ""
        val unbreakable = names.map(::keepTogether)
        for (shown in names.size downTo 1) {
            val hidden = names.size - shown
            val text = unbreakable.take(shown).joinToString(SEPARATOR) +
                (if (hidden > 0) more(hidden) else "")
            if (shown == 1 || measure(text) <= maxWidth) return text
        }
        return unbreakable.first()
    }

    /** 글자 사이에 줄바꿈 금지 문자(폭 0)를 넣고 띄어쓰기는 줄바꿈 없는 공백으로 바꾼다. */
    fun keepTogether(name: String): String = buildString {
        name.forEachIndexed { i, c ->
            if (i > 0) append(WORD_JOINER)
            append(if (c == ' ') NO_BREAK_SPACE else c)
        }
    }
}

package com.management.subscription.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ServiceNamesLineTest {

    // 폭 0인 줄바꿈 금지 문자는 세지 않고 나머지 글자를 1씩 센다.
    private val measure: (String) -> Float = { text -> text.count { it != '⁠' }.toFloat() }
    private val more: (Int) -> String = { " 외 ${it}개" }

    private fun plain(text: String) = text.replace("⁠", "").replace(' ', ' ')

    private val names = listOf("ChatGPT Plus", "Claude Pro", "Netflix", "Spotify", "쿠팡이츠")

    @Test
    fun allNamesFit_showsEveryName() {
        val text = ServiceNamesLine.fit(names, 100f, measure, more)
        assertEquals("ChatGPT Plus · Claude Pro · Netflix · Spotify · 쿠팡이츠", plain(text))
    }

    @Test
    fun narrowWidth_showsWhatFitsAndCountsTheRest() {
        // "ChatGPT Plus · Claude Pro · Netflix 외 2개" = 41
        val text = ServiceNamesLine.fit(names, 41f, measure, more)
        assertEquals("ChatGPT Plus · Claude Pro · Netflix 외 2개", plain(text))
    }

    @Test
    fun tooNarrowForAnyList_keepsFirstNameWhole() {
        val text = ServiceNamesLine.fit(names, 5f, measure, more)
        assertEquals("ChatGPT Plus 외 4개", plain(text))
    }

    @Test
    fun namesCannotBreakInside() {
        val joined = ServiceNamesLine.keepTogether("Claude Pro")
        assertFalse(joined.contains(' '))
        assertEquals("C⁠l⁠a⁠u⁠d⁠e⁠ ⁠P⁠r⁠o", joined)
    }

    @Test
    fun noNames_isEmpty() {
        assertEquals("", ServiceNamesLine.fit(emptyList(), 100f, measure, more))
    }
}

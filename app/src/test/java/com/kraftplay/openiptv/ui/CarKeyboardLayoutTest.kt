package com.kraftplay.openiptv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CarKeyboardLayoutTest {

    @Test
    fun `num and sym layouts expose all required password and login symbols`() {
        val numKeys = keyboardRowsFor(KeyboardLayoutMode.NUM).flatten().toSet()
        val symKeys = keyboardRowsFor(KeyboardLayoutMode.SYM).flatten().toSet()
        val allSymbols = numKeys + symKeys

        val required = listOf(
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "@", "_", "-", ".", "+", "!", "#", "$", "%", "&", "*", "=",
            "^", "~", "\\", "|", "(", ")", "[", "]", "{", "}", "<", ">",
            "/", ":", ";", ",", "?", "'", "\""
        )
        for (symbol in required) {
            assertTrue("Missing key '$symbol' in NUM/SYM layouts", symbol in allSymbols)
        }
    }

    @Test
    fun `ru and en layouts include shift and backspace`() {
        for (mode in listOf(KeyboardLayoutMode.RU, KeyboardLayoutMode.EN)) {
            val keys = keyboardRowsFor(mode).flatten().toSet()
            assertTrue("Missing shift in $mode", "⇧" in keys)
            assertTrue("Missing backspace in $mode", "⌫" in keys)
        }
    }

    @Test
    fun `symbol toggle goes directly from letter layouts to NUM and toggles NUM with SYM`() {
        assertEquals(KeyboardLayoutMode.NUM, toggleSymbolMode(KeyboardLayoutMode.RU))
        assertEquals(KeyboardLayoutMode.NUM, toggleSymbolMode(KeyboardLayoutMode.EN))
        assertEquals(KeyboardLayoutMode.SYM, toggleSymbolMode(KeyboardLayoutMode.NUM))
        assertEquals(KeyboardLayoutMode.NUM, toggleSymbolMode(KeyboardLayoutMode.SYM))
    }

    @Test
    fun `language toggle switches RU and EN and returns from symbols to last letter layout`() {
        assertEquals(KeyboardLayoutMode.EN, toggleLanguageOrLetters(KeyboardLayoutMode.RU, KeyboardLayoutMode.RU))
        assertEquals(KeyboardLayoutMode.RU, toggleLanguageOrLetters(KeyboardLayoutMode.EN, KeyboardLayoutMode.EN))
        assertEquals(KeyboardLayoutMode.EN, toggleLanguageOrLetters(KeyboardLayoutMode.NUM, KeyboardLayoutMode.EN))
        assertEquals(KeyboardLayoutMode.RU, toggleLanguageOrLetters(KeyboardLayoutMode.SYM, KeyboardLayoutMode.RU))
    }

    @Test
    fun `preferred layout matches web input types`() {
        assertEquals(KeyboardLayoutMode.NUM, preferredLayoutForInputType("tel"))
        assertEquals(KeyboardLayoutMode.NUM, preferredLayoutForInputType("number"))
        assertEquals(KeyboardLayoutMode.EN, preferredLayoutForInputType("email"))
        assertEquals(KeyboardLayoutMode.EN, preferredLayoutForInputType("password"))
        assertEquals(KeyboardLayoutMode.EN, preferredLayoutForInputType("url"))
        assertEquals(KeyboardLayoutMode.EN, preferredLayoutForInputType("text", "Email or phone"))
        assertEquals(KeyboardLayoutMode.NUM, preferredLayoutForInputType("text", "Номер телефона"))
        assertEquals(KeyboardLayoutMode.RU, preferredLayoutForInputType("text"))
    }
}

package com.toolbox.util

import kotlin.test.Test
import kotlin.test.assertEquals

class TextCleanerTest {
    
    @Test
    fun trimLinesAndRemoveEmpty() {
        val input = "  a  \n\n   \nb\t"
        val result = TextCleaner.clean(
            input,
            CleanOptions(trimLines = true, removeEmptyLines = true)
        )
        assertEquals("a\nb", result)
    }
    
    @Test
    fun removeSpaces() {
        val result = TextCleaner.clean("a b\u3000c", CleanOptions(removeSpaces = true))
        assertEquals("abc", result)
    }
    
    @Test
    fun mergeSpaces() {
        val result = TextCleaner.clean("a   b\t\tc", CleanOptions(mergeSpaces = true))
        assertEquals("a b c", result)
    }
    
    @Test
    fun dedupeLines() {
        val result = TextCleaner.clean("a\nb\na", CleanOptions(dedupeLines = true))
        assertEquals("a\nb", result)
    }
    
    @Test
    fun sortLines() {
        val result = TextCleaner.clean("b\na\nc", CleanOptions(sortLines = true))
        assertEquals("a\nb\nc", result)
    }
    
    @Test
    fun removeInvisible() {
        val result = TextCleaner.clean("a\uFEFFb\u0007c", CleanOptions(removeInvisible = true))
        assertEquals("abc", result)
    }
    
    @Test
    fun noOptionsKeepsText() {
        val input = " a \n\n b "
        assertEquals(input, TextCleaner.clean(input, CleanOptions()))
    }
}